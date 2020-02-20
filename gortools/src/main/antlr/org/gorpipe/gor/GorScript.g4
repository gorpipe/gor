grammar GorScript;

@header {
package org.gorpipe.gor;
}

script:
	(def_macro | create_statement)* query_expression EOF;

calc_expression:
	expression EOF;

create_statement:
	CREATE virtual_relation_name '=' query_expression EOS;

def_macro:
	DEF macro_name '=' macro_value;

macro_value : ~EOS* EOS ;

macro_name:
	IDENTIFIER;

virtual_relation_name:
	IDENTIFIER;

query_expression:
	gornor_expression;

gornor_expression:
	gor_expression | nor_expression | pgor_expression;

gor_expression:
	(GOR | input_source | virtual_relation) options_and_args (PIPE pipe_command)*;

pgor_expression:
	PGOR options_and_args (PIPE pipe_command)*;

nor_expression:
	NOR options_and_args (PIPE pipe_command)*;

options_and_args:
	(macro_name | argument | option)*;

option:
	'-' IDENTIFIER (option_value)?;

argument:
	(virtual_relation | input_source | nested_query | string_literal | column_selection | NUMBER);

virtual_relation:
	'[' IDENTIFIER ']';

input_source:
	 filename | macro_name;

filename:
	FILENAME;

nested_query:
	'<(' gornor_expression ')';

option_value:
	(IDENTIFIER | NUMBER | '?') (',' (IDENTIFIER | NUMBER | '?'))*;

pipe_command :
	calc_command
	| where_command
	| replace_command
	| select_command
	| hide_command
	| rename_command
	| generic_command
	;

calc_command :
	CALC column_list '='? calc_expression_list;

where_command :
	WHERE rel_expr;

replace_command :
	REPLACE column_selection calc_expression_list;

select_command :
	SELECT column_selection;

hide_command :
	HIDE column_selection;

rename_command :
	RENAME column_selection column_rename_rule;

generic_command :
	IDENTIFIER options_and_args;

column_list:
	column_name (',' column_name)*;

column_selection:
	(column_ref | column_range | column_wildcard) (',' (column_ref | column_range | column_wildcard))*;

column_range :
	column_ref '-' column_ref?;

column_ref :
	column_name
	| column_number;

column_wildcard :
	column_name '*'
	| column_name '(''.''*'')';

column_name : IDENTIFIER;
column_number : NUMBER;

column_rename_rule:
	IDENTIFIER
	| '#''{'NUMBER'}'
	| IDENTIFIER '{'NUMBER'}';

calc_expression_list:
	expression (',' expression)*;

expression : term ((PLUS | MINUS) term)* ;

term : optional_power_factor ((TIMES | DIV) optional_power_factor)* ;

optional_power_factor:
	power_factor | factor;

power_factor:
	factor POW factor;

factor : signed_factor | xfactor ;

signed_factor : (PLUS | MINUS) xfactor ;

xfactor : paren_expr | function_expr | value ;

paren_expr : OPEN_PAREN expression CLOSE_PAREN ;

function_expr :
	if_expr
	| function_call
	;

function_call :
	function_name OPEN_PAREN (expression (',' expression)*)? CLOSE_PAREN
	;

if_expr :
	IF OPEN_PAREN rel_expr ',' expression ',' expression CLOSE_PAREN
	;

rel_expr : rel_term (OR rel_term)* ;

rel_term : predicate_factor (AND predicate_factor)*;

predicate_factor:
	paren_rel_expr
	| not_rel_expr
	| predicate;

paren_rel_expr:
	OPEN_PAREN rel_expr CLOSE_PAREN;

not_rel_expr:
	NOT rel_expr;

predicate :
	function_call | compare_expressions | indag_expression | in_expression;

compare_expressions :
	expression (EQ | |S_EQ | NE | GT | GE | LT | LE | LIKE | RLIKE ) expression;

in_expression:
	expression IN string_literal_list;

indag_expression:
	expression INDAG OPEN_PAREN (filename | virtual_relation | string_literal) COMMA expression CLOSE_PAREN;

function_name : REPLACE | IDENTIFIER ;

value : variable | number | string_literal;

variable : IDENTIFIER ;

number : NUMBER | NAN;

string_literal_list:
	OPEN_PAREN string_literal (',' string_literal)* CLOSE_PAREN;

string_literal : SingleQuoteString | DoubleQuoteString ;

SingleQuoteString
    :
    '\'' (ESC_S|.)*? '\''
    ;

DoubleQuoteString
    :
    '"' (ESC_D|.)*? '"'
    ;

OPEN_PAREN: '(';
CLOSE_PAREN: ')';
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
COMMA: ',';
PIPE: '|';
EOS: ';';
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
POW: '^';
S_EQ: '=';
EQ: '==';
NE: '!=' | '<>';
GT: '>';
GE: '>=';
LT: '<';
LE: '<=';
LIKE: L I K E | '~';
RLIKE: R L I K E;
INDAG: I N D A G;
IN: I N;

IF: I F ;
OR: O R ;
AND: A N D ;
NOT: N O T;

DEF: D E F;
CREATE: C R E A T E;
PGOR: P G O R;
GOR: G O R;
NOR: N O R;

CALC : C A L C ;
HIDE : H I D E ;
RENAME : R E N A M E ;
REPLACE : R E P L A C E ;
SELECT : S E L E C T ;
WHERE : W H E R E ;

NAN : N A N ;

NUMBER: DIGIT+ ('.' DIGIT+)? ENOTATION? | '.' DIGIT+ ENOTATION?;
IDENTIFIER: (LETTER|DIGIT)+;
LETTER: ('a'..'z')|('A'..'Z')|'_'|'#'|':';
FILENAME: (LETTER|DIGIT|'/')+ '.' LETTER (LETTER|DIGIT)*;
OPTION_VALUE: (LETTER | DIGIT | '_' | '?')+;
DIGIT: ('0'..'9');

fragment
ENOTATION: E (PLUS|MINUS)? DIGIT+ ;

fragment
ESC_D: '\\"' | '\\\\' ;

fragment
ESC_S: '\\\'' | '\\\\' ;


fragment A : [aA]; // match either an 'a' or 'A'
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

WS : [ \r\n\t]+ -> channel(HIDDEN);
ERRCHAR :	. -> channel(HIDDEN);

