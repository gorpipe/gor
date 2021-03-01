/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.GorScriptBaseVisitor;
import org.gorpipe.gor.GorScriptParser;
import scala.collection.Iterator;
import scala.collection.mutable.ListBuffer;

import java.util.*;

/**
 * CalcCompiler implements an Antlr parse tree visitor, starting on an expression node. It builds
 * up a TypedCalcLambda along the way, collapsing constant expressions into constants, otherwise
 * a lambda taking a ColumnValueProvider as an argument.
 *
 * To use the CalcCompiler, construct a parse tree with GorScriptParser - this is also done
 * by SyntaxChecker. Then call setColumnNamesAndTypes to inform the compiler of available variables
 * before passing the compiler to the accept method of the parse tree node.
 */
public class CalcCompiler extends GorScriptBaseVisitor<TypedCalcLambda> {
    private static final int REPLACE_COLUMN = -3;
    private final FunctionRegistry functionRegistry = CalcFunctions.registry();
    private final ParseArith owner;
    private final Map<String, Integer> columns = new HashMap<>();
    private final Map<Integer, String> columnTypes = new HashMap<>();
    private final List<String> columnNames = new ArrayList<>();

    public CalcCompiler() {
        this(null);
    }

    public CalcCompiler(ParseArith owner) {
        this.owner = owner;

        // Special column for REPLACE with wildcard
        columnTypes.put(REPLACE_COLUMN, "S");
    }

    private static int typePriority(String type) {
        if (type.equals("Boolean")) {
            return 0;
        }
        if (type.equals("Int")) {
            return 1;
        }
        if (type.equals("Long")) {
            return 2;
        }
        if (type.equals("Double")) {
            return 3;
        }
        if (type.equals("String")) {
            return 4;
        }

        throw new GorSystemException("Unknown type", null);
    }

    private GorParsingException getIncompatibleTypes() {
        return new GorParsingException("Incompatible types");
    }

    public void setColumnNamesAndTypes(String[] colNames, String[] colTypes) {
        for (int i = 0; i < colNames.length; i++) {
            columnNames.add(colNames[i]);
            columns.put(colNames[i].toUpperCase(), i);
            columnTypes.put(i, colTypes[i]);
        }
    }

    public void addSpecialVars() {
        columns.put("X", -2);
        columnTypes.put(-2, "S");
        columns.put("I", -1);
        columnTypes.put(-1, "I");
    }

    @Override
    public TypedCalcLambda visitCalc_expression(GorScriptParser.Calc_expressionContext ctx) {
        return ctx.getChild(0).accept(this);
    }

    @Override
    public TypedCalcLambda visitExpression(GorScriptParser.ExpressionContext ctx) {
        TypedCalcLambda accumulator = ctx.getChild(0).accept(this);
        int childCount = ctx.getChildCount();
        for (int i = 1; i < childCount; i += 2) {
            int op = ((TerminalNode) ctx.getChild(i)).getSymbol().getType();
            GorScriptParser.TermContext child = (GorScriptParser.TermContext) ctx.getChild(i + 1);
            TypedCalcLambda nextTerm = child.accept(this);
            try {
                if (op == GorScriptParser.PLUS) {
                    accumulator = nextTerm.addedTo(accumulator);
                } else if (op == GorScriptParser.MINUS) {
                    accumulator = nextTerm.subtractedFrom(accumulator);
                }
            } catch (GorParsingException e) {
                e.setLine(child.start.getLine());
                e.setPos(child.start.getStopIndex() + 1);
                throw e;
            }
        }
        return accumulator;
    }

    @Override
    public TypedCalcLambda visitTerm(GorScriptParser.TermContext ctx) {
        TypedCalcLambda accumulator = ctx.getChild(0).accept(this);
        int childCount = ctx.getChildCount();
        for (int i = 1; i < childCount; i += 2) {
            int op = ((TerminalNode) ctx.getChild(i)).getSymbol().getType();
            GorScriptParser.Optional_power_factorContext child = (GorScriptParser.Optional_power_factorContext)ctx.getChild(i + 1);
            TypedCalcLambda nextTerm = child.accept(this);
            try {
                if (op == GorScriptParser.TIMES) {
                    accumulator = nextTerm.multipliedWith(accumulator);
                } else if (op == GorScriptParser.DIV) {
                    accumulator = nextTerm.dividedInto(accumulator);
                }
            } catch (GorParsingException e) {
                e.setLine(child.start.getLine());
                e.setPos(child.start.getStopIndex() + 1);
                throw e;
            }
        }
        return accumulator;
    }

    @Override
    public TypedCalcLambda visitSigned_factor(GorScriptParser.Signed_factorContext ctx) {
        int op = ((TerminalNode) ctx.getChild(0)).getSymbol().getType();
        TypedCalcLambda ex = ctx.getChild(1).accept(this);
        if (op == GorScriptParser.MINUS) {
            return ex.negate();
        } else {
            return ex;
        }
    }

    @Override
    public TypedCalcLambda visitFunction_call(GorScriptParser.Function_callContext ctx) {
        String name = ctx.function_name().getText().toUpperCase();
        List<TypedCalcLambda> argLambdas = getArgLambdas(ctx.expression());

        TypedCalcLambda lambdaFromFunctionWrapper = getTypedCalcLambdaForFunction(name, argLambdas, ArgumentPromotion.NONE);
        if (lambdaFromFunctionWrapper != null) {
            return lambdaFromFunctionWrapper;
        }
        lambdaFromFunctionWrapper = getTypedCalcLambdaForFunction(name, argLambdas, ArgumentPromotion.NUMERIC_VARIABLES_AS_STRING);
        if (lambdaFromFunctionWrapper != null) {
            return lambdaFromFunctionWrapper;
        }
        throw getIncompatibleTypes();
    }

    private TypedCalcLambda getTypedCalcLambdaForFunction(String name, List<TypedCalcLambda> argLambdas, ArgumentPromotion argumentPromotion) {
        TypedCalcLambda lambdaFromFunctionWrapper = null;

        List<String> sortedVariants = new ArrayList<>();
        functionRegistry.getVariants(name).iterator().foreach(sortedVariants::add);
        sortedVariants.sort((a, b) -> {
            String returnTypeA = a.split("2")[1];
            String returnTypeB = b.split("2")[1];
            return typePriority(returnTypeA) - typePriority(returnTypeB);
        });

        for (String signature : sortedVariants) {
            String mangledName = name + "_" + signature;
            FunctionWrapper functionWrapper = functionRegistry.lookupWrapper(mangledName);

            List<TypedExpression> args = new ArrayList<>();
            Iterator<String> expectedArgs = functionWrapper.expectedArgs().iterator();
            java.util.Iterator<TypedCalcLambda> lambdaIterator = argLambdas.iterator();
            boolean argumentsMatch = true;
            while (expectedArgs.hasNext()) {
                TypedExpression typedExpression = getTypedExpression(expectedArgs.next(), lambdaIterator, argumentPromotion);
                if (typedExpression == null) {
                    argumentsMatch = false;
                    break;
                }
                args.add(typedExpression);
            }
            if (!lambdaIterator.hasNext() && argumentsMatch) {
                lambdaFromFunctionWrapper = getLambdaFromFunctionWrapper(functionWrapper, args);
                break;
            }
        }
        return lambdaFromFunctionWrapper;
    }

    private List<TypedCalcLambda> getArgLambdas(List<GorScriptParser.ExpressionContext> arguments) {
        List<TypedCalcLambda> argLambdas = new ArrayList<>();
        for (GorScriptParser.ExpressionContext e : arguments) {
            argLambdas.add(e.accept(this));
        }
        return argLambdas;
    }

    private TypedExpression getTypedExpression(String argType, java.util.Iterator<TypedCalcLambda> lambdaIterator, ArgumentPromotion argumentPromotion) {
        TypedExpression typedExpression = null;
        if (lambdaIterator.hasNext()) {
            TypedCalcLambda orgLambda = lambdaIterator.next();
            final TypedCalcLambda argLambda = orgLambda.toLambda();
            if (argType.equals(FunctionTypes.DoubleFun()) && argLambda instanceof Numeric) {
                typedExpression = new TypedExpression(FunctionTypes.DoubleFun(), (CvpDoubleLambda) argLambda::evaluateDouble);
            } else if (argType.equals(FunctionTypes.IntFun()) && argLambda instanceof IntegerType) {
                typedExpression = new TypedExpression(FunctionTypes.IntFun(), (CvpIntegerLambda)argLambda::evaluateInt);
            } else if (argType.equals(FunctionTypes.LongFun()) && (argLambda instanceof IntegerType || argLambda instanceof LongType)) {
                typedExpression = new TypedExpression(FunctionTypes.LongFun(), (CvpLongLambda)argLambda::evaluateLong);
            } else if (argType.equals(FunctionTypes.StringFun())) {
                if (orgLambda instanceof StringType ||
                        orgLambda instanceof Numeric && argumentPromotion == ArgumentPromotion.NUMERIC_AS_STRING ||
                        orgLambda instanceof CalcLambdaVariable && argumentPromotion == ArgumentPromotion.NUMERIC_VARIABLES_AS_STRING
                ) {
                    typedExpression = new TypedExpression(FunctionTypes.StringFun(), (CvpStringLambda)orgLambda::evaluateString);
                }
            } else if (argType.equals(FunctionTypes.StringList()) && argLambda instanceof CalcLambdaStringConstant) {
                ListBuffer<String> sl = new ListBuffer<>();
                boolean consumedAllArguments = false;
                while (orgLambda instanceof CalcLambdaStringConstant) {
                    sl.$plus$eq(orgLambda.evaluateString(null));
                    if (!lambdaIterator.hasNext()) {
                        consumedAllArguments = true;
                        break;
                    }
                    orgLambda = lambdaIterator.next();
                }
                if (consumedAllArguments) {
                    typedExpression = new TypedExpression(FunctionTypes.StringList(), sl.toList());
                }
            }
        }
        return typedExpression;
    }

    private TypedCalcLambda getLambdaFromFunctionWrapper(FunctionWrapper functionWrapper, List<TypedExpression> args) {
        if (functionWrapper.returnType().equals(FunctionTypes.DoubleFun())) {
            return new CalcLambdaDouble(FunctionTypes.dFunToLambda(functionWrapper.call(owner, args)));
        } else if (functionWrapper.returnType().equals(FunctionTypes.IntFun())) {
            return new CalcLambdaInteger(FunctionTypes.iFunToLambda(functionWrapper.call(owner, args)));
        } else if (functionWrapper.returnType().equals(FunctionTypes.LongFun())) {
            return new CalcLambdaLong(FunctionTypes.lFunToLambda(functionWrapper.call(owner, args)));
        } else if (functionWrapper.returnType().equals(FunctionTypes.StringFun())) {
            return new CalcLambdaString(FunctionTypes.sFunToLambda(functionWrapper.call(owner, args)));
        } else if (functionWrapper.returnType().equals(FunctionTypes.BooleanFun())) {
            return new CalcLambdaBoolean(FunctionTypes.bFunToLambda(functionWrapper.call(owner, args)));
        } else {
            throw new GorSystemException("Unsupported return type", null);
        }
    }

    @Override
    public TypedCalcLambda visitParen_expr(GorScriptParser.Paren_exprContext ctx) {
        return ctx.getChild(1).accept(this);
    }

    @Override
    public TypedCalcLambda visitParen_rel_expr(GorScriptParser.Paren_rel_exprContext ctx) {
        return ctx.getChild(1).accept(this);
    }

    @Override
    public TypedCalcLambda visitNot_rel_expr(GorScriptParser.Not_rel_exprContext ctx) {
        TypedCalcLambda expr = ctx.getChild(1).accept(this);
        return new CalcLambdaBoolean(cvp -> !expr.evaluateBoolean(cvp));
    }

    @Override
    public TypedCalcLambda visitIn_expression(GorScriptParser.In_expressionContext ctx) {
        TypedCalcLambda left = ctx.getChild(0).accept(this);

        Set<String> sl = new HashSet<>();

        ParseTree slCtx = ctx.getChild(2);

        for (int argIx = 1; argIx < slCtx.getChildCount(); argIx += 2) {
            TypedCalcLambda arg = slCtx.getChild(argIx).accept(this);
            sl.add(arg.evaluateString(null));
        }

        return new CalcLambdaBoolean(cvp -> sl.contains(left.evaluateString(cvp)));
    }

    @Override
    public TypedCalcLambda visitIndag_expression(GorScriptParser.Indag_expressionContext ctx) {
        TypedCalcLambda left = ctx.getChild(0).accept(this);
        TypedCalcLambda file = ctx.getChild(3).accept(this);
        TypedCalcLambda v = ctx.getChild(5).accept(this);
        scala.collection.immutable.Set<String> dagSet = owner.aDagSet(file.evaluateString(null), v.evaluateString(null));
        return new CalcLambdaBoolean( cvp -> dagSet.contains(left.evaluateString(cvp).toUpperCase()));
    }

    @Override
    public TypedCalcLambda visitRel_term(GorScriptParser.Rel_termContext ctx) {
        TypedCalcLambda accumulator = ctx.getChild(0).accept(this);
        int childCount = ctx.getChildCount();
        for (int i = 1; i < childCount; i += 2) {
            int op = ((TerminalNode) ctx.getChild(i)).getSymbol().getType();
            TypedCalcLambda nextTerm = ctx.getChild(i + 1).accept(this);
            if (op == GorScriptParser.AND) {
                TypedCalcLambda acc = accumulator;
                accumulator = new CalcLambdaBoolean(cvp -> acc.evaluateBoolean(cvp) && nextTerm.evaluateBoolean(cvp));
            }
        }
        return accumulator;
    }

    @Override
    public TypedCalcLambda visitRel_expr(GorScriptParser.Rel_exprContext ctx) {
        TypedCalcLambda accumulator = ctx.getChild(0).accept(this);
        int childCount = ctx.getChildCount();
        for (int i = 1; i < childCount; i += 2) {
            int op = ((TerminalNode) ctx.getChild(i)).getSymbol().getType();
            TypedCalcLambda nextTerm = ctx.getChild(i + 1).accept(this);
            if (op == GorScriptParser.OR) {
                TypedCalcLambda acc = accumulator;
                accumulator = new CalcLambdaBoolean(cvp -> acc.evaluateBoolean(cvp) || nextTerm.evaluateBoolean(cvp));
            }
        }
        return accumulator;
    }

    @Override
    public TypedCalcLambda visitCompare_expressions(GorScriptParser.Compare_expressionsContext ctx) {
        GorScriptParser.ExpressionContext leftCtx = ctx.getChild(GorScriptParser.ExpressionContext.class, 0);
        GorScriptParser.ExpressionContext rightCtx = ctx.getChild(GorScriptParser.ExpressionContext.class, 1);
        int op = ((TerminalNode) ctx.getChild(1)).getSymbol().getType();

        TypedCalcLambda left = leftCtx.accept(this);
        TypedCalcLambda right = rightCtx.accept(this);

        return left.compare(right, op);
    }

    @Override
    public TypedCalcLambda visitIf_expr(GorScriptParser.If_exprContext ctx) {
        GorScriptParser.Rel_exprContext relExprContext = ctx.getChild(GorScriptParser.Rel_exprContext.class, 0);
        TypedCalcLambda predicate = relExprContext.accept(this);

        GorScriptParser.ExpressionContext thenExprContext = ctx.getChild(GorScriptParser.ExpressionContext.class, 0);
        TypedCalcLambda thenExpr = thenExprContext.accept(this).toLambda();
        GorScriptParser.ExpressionContext elseExprContext = ctx.getChild(GorScriptParser.ExpressionContext.class, 1);
        TypedCalcLambda elseExpr = elseExprContext.accept(this).toLambda();

        if (thenExpr instanceof IntegerType && elseExpr instanceof IntegerType) {
            return new CalcLambdaInteger(cvp -> {
                if (predicate.evaluateBoolean(cvp)) {
                    return thenExpr.evaluateInt(cvp);
                } else {
                    return elseExpr.evaluateInt(cvp);
                }
            });
        } else if (thenExpr instanceof DoubleType && elseExpr instanceof DoubleType) {
            return new CalcLambdaDouble(cvp -> {
                if (predicate.evaluateBoolean(cvp)) {
                    return thenExpr.evaluateDouble(cvp);
                } else {
                    return elseExpr.evaluateDouble(cvp);
                }
            });
        } else if (thenExpr instanceof StringType && (elseExpr instanceof StringType || !(elseExpr instanceof Constant))) {
            return new CalcLambdaString(cvp -> {
                if (predicate.evaluateBoolean(cvp)) {
                    return thenExpr.evaluateString(cvp);
                } else {
                    return elseExpr.evaluateString(cvp);
                }
            });
        }

        throw getIncompatibleTypes();
    }

    @Override
    public TypedCalcLambda visitPower_factor(GorScriptParser.Power_factorContext ctx) {
        ParseTree leftCtx = ctx.getChild(0);
        ParseTree rightCtx = ctx.getChild(2);

        TypedCalcLambda left = leftCtx.accept(this);
        TypedCalcLambda right = rightCtx.accept(this);

        return left.pow(right);
    }

    @Override
    public TypedCalcLambda visitString_literal(GorScriptParser.String_literalContext ctx) {
        String textWithQuotes = ctx.getText();

        boolean escaped = false;
        StringBuilder text = new StringBuilder(textWithQuotes.length() - 2);
        for (int i = 1; i < textWithQuotes.length() - 1; i++) {
            char c = textWithQuotes.charAt(i);
            if (escaped || c != '\\') {
                text.append(c);
                escaped = false;
            } else {
                escaped = true;
            }
        }
        return new CalcLambdaStringConstant(text.toString());
    }

    @Override
    public TypedCalcLambda visitNumber(GorScriptParser.NumberContext ctx) {
        String text = ctx.getText();
        try {
            int i = Integer.parseInt(text);
            return new CalcLambdaIntegerConstant(i);
        } catch (NumberFormatException e1) {
            try {
                long l = Long.parseLong(text);
                return new CalcLambdaLongConstant(l);
            } catch (NumberFormatException e2) {
                double d = Double.parseDouble(text);
                return new CalcLambdaDoubleConstant(d);
            }
        }
    }

    @Override
    public TypedCalcLambda visitVariable(GorScriptParser.VariableContext ctx) {
        String originalName = ctx.getText();
        String name = originalName.toUpperCase();
        int columnIndex;

        if (name.equals("#RC")) {
            columnIndex = REPLACE_COLUMN;
        } else if (name.startsWith("#")) {
            try {
                columnIndex = Integer.parseInt(name.substring(1)) - 1;
            } catch (NumberFormatException e) {
                throw new GorParsingException(String.format("Variable '%s' not found", name));
            }
        } else {
            Integer ix = columns.get(name);
            if (ix == null) {
                ListBuffer<String> names = new ListBuffer<>();
                columnNames.forEach(names::$plus$eq);
                String closest = StringDistance.findClosest(name, 3, names.toList());
                String suffix = "";
                if (!closest.isEmpty()) {
                    suffix = " - did you mean: " + closest + "?";
                }
                String message = String.format("Variable name '%s' not found%s", originalName, suffix);
                throw new GorParsingException(message, ctx.start.getLine(), ctx.start.getStopIndex() + 1);
            }
            columnIndex = ix;
        }

        return new CalcLambdaVariable(columnIndex, columnTypes.get(columnIndex));
    }

    @Override
    public TypedCalcLambda visitFilename(GorScriptParser.FilenameContext ctx) {
        return new CalcLambdaStringConstant(ctx.getText());
    }

    enum ArgumentPromotion {
        NONE,
        NUMERIC_VARIABLES_AS_STRING,
        NUMERIC_AS_STRING
    }
}
