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
package org.gorpipe.querydialogs.beans.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * A base class for all arguments.
 * <p>
 * <p>Aside from being a bean this class also provides a ListModel interface
 * on the allowed values for this argument</p>
 *
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings({"javadoc", "serial"})
public abstract class Argument extends AbstractListBean {
    /**
     * The property name for the argument's raw value.
     */
    public static final String PROPERTY_VALUE = "value";
    /**
     * The property name for the argument's current operator.
     */
    public static final String PROPERTY_OPERATOR = "operator";

    /***/
    public static final List<Object> DEFERRED_LIST = new ArrayList<>();
    public static final HashMap<String, List<Object>> path2LoadedValues = new HashMap<>();
    public static final HashMap<String, String[]> path2LoadedValuesHeader = new HashMap<>();

    private final ArgumentDescription argDescr;
    protected final ArgumentType type;
    protected Object value;
    protected final Object defaultValue;
    protected List<Object> allowedValues;
    private final boolean optional;
    private String[] valuesHeader;
    private String currentOperator;
    private final List<String> operators;
    private final URI valuesPath;
    private final boolean advanced;
    private Integer displayWidth;

    /**
     * Base constructor for all arguments.
     * <p>
     * <p>All extensions of this abstract type are recommended to build via this constructor</p>
     *
     * @param type          - the type of argument
     * @param argDescr      - the argument description
     * @param optional      - true if this argument is not required, false otherwise
     * @param defaultValue  - the initial value for this argument
     * @param allowedValues - a list of allowed values for this argument
     * @param operators     - a list of operators supported by this argument
     * @param advanced      - <code>true</code> if argument is advanced, otherwise false. Can be used to determine visibility mode of argument.
     */
    public Argument(ArgumentType type, ArgumentDescription argDescr, Boolean optional, Object defaultValue,
                    List<? extends Object> allowedValues, URI valuesPath, List<String> operators, Boolean advanced, Integer displayWidth) {
        this.valuesPath = valuesPath;
        if (allowedValues == null) {
            this.allowedValues = Collections.emptyList();
        } else if (allowedValues == DEFERRED_LIST) {
            this.allowedValues = DEFERRED_LIST;
        } else {
            this.allowedValues = new ArrayList<>(allowedValues);
        }
        if (operators == null) {
            this.operators = Collections.emptyList();
        } else {
            this.operators = new ArrayList<>(operators);
        }
        this.type = type;
        this.optional = optional != null && optional.booleanValue();
        this.advanced = advanced != null && advanced.booleanValue();
        this.displayWidth = displayWidth;
        this.argDescr = argDescr;
        this.defaultValue = defaultValue;
        setValue(defaultValue);
        if (!this.operators.isEmpty()) {
            setOperator(this.operators.get(0));
        }
    }

    /**
     * Constructor that copies the input argument.
     *
     * @param arg the argument to copy
     */
    public Argument(final Argument arg) {
        this(arg.type, arg.argDescr, arg.optional, arg.defaultValue, arg.allowedValues, arg.valuesPath, arg.operators,
                arg.advanced, arg.displayWidth);
        this.currentOperator = arg.currentOperator;
        this.value = arg.value;
        this.valuesHeader = arg.valuesHeader;
    }

    /**
     * Get argument name.
     *
     * @return the argument name
     */
    public String getName() {
        return argDescr.name;
    }

    /**
     * Get argument description.
     *
     * @return the argument description
     */
    public String getDescription() {
        return argDescr.shortDescr;
    }

    /**
     * Get argument tooltip.
     *
     * @return the argument tooltip
     */
    public String getTooltip() {
        return argDescr.tooltip;
    }

    /**
     * Get if argument is optional.
     *
     * @return <code>true</code> if argument is optional, otherwise <code>false</code>
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Get if argument is empty.
     *
     * @return <code>true</code> if argument is empty, otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * Get if argument is advanced.
     *
     * @return <code>true</code> if argument is advanced, otherwise <code>false</code>
     */
    public boolean isAdvanced() {
        return advanced;
    }

    /**
     * Get the display width for argument.
     *
     * @return the display width for argument
     */
    public Integer getDisplayWidth() {
        return displayWidth;
    }

    /**
     * Set the display width for argument.
     *
     * @param displayWidth the display width to set
     */
    public void setDisplayWidth(final Integer displayWidth) {
        this.displayWidth = displayWidth;
    }

    /**
     * Get the name to display for argument.
     *
     * @return display name
     */
    public String getDisplayName() {
        return argDescr.getDisplayName();
    }

    public ArgumentType getType() {
        return type;
    }

    public String getValuesPath() {
        return valuesPath.toString();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    protected void checkAllowed(Object theValue) {
        if (theValue != null && !allowedValues.isEmpty() && valuesPath == null && !allowedValues.contains(theValue)) {
            throw new RuntimeException("Invalid value: " + theValue + " for argument " + argDescr.name);
        }
    }

    public void setValue(final Object value, final boolean checkCommaSeparatedValues) {
        if (checkCommaSeparatedValues) {
            if (value instanceof String && ((String) value).length() > 0) {
                if (!allowedValues.isEmpty() && valuesPath == null) {
                    final String[] splitValues = ((String) value).split(",");
                    for (String splitValue : splitValues) {
                        if (!allowedValues.contains(splitValue)) {
                            throw new RuntimeException("Invalid value: " + value);
                        }
                    }
                }
            }
        } else {
            checkAllowed(value);
        }
        Object oldValue = this.value;
        this.value = value;
        firePropertyChange(PROPERTY_VALUE, oldValue, value);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        setValue(value, false);
    }

    public String getOperator() {
        return currentOperator;
    }

    public void setOperator(String operator) {
        firePropertyChange(PROPERTY_OPERATOR, this.currentOperator, this.currentOperator = operator);
    }

    /**
     * @return a list of available operators
     */
    public List<String> getOperators() {
        return Collections.unmodifiableList(operators);
    }

    @Override
    public Object getElementAt(int index) {
        return allowedValues.get(index);
    }

    @Override
    public int getSize() {
        return allowedValues != null ? allowedValues.size() : 0;
    }

    @Override
    public String toString() {
        return getValue() == null ? "" : getValue().toString();
    }

    /***/
    public boolean hasDeferredValues() {
        return valuesPath != null;
    }

    /***/
    public boolean deferredValuesLoaded() {
        return allowedValues != DEFERRED_LIST;
    }

    /**
     * Get the values header.
     *
     * @return the values header
     */
    public String[] getValuesHeader() {
        return valuesHeader;
    }

    /**
     * Possibly long running, should not be called on the EDT.
     *
     * @throws IOException
     */
    public void loadDeferredValues(final boolean checkForHeader, Function<String, BufferedReader> fileResolver) throws IOException {
        if (deferredValuesLoaded()) return;
        BufferedReader reader = null;

        final String valuesPathString = valuesPath.toString();
        try {
            List<Object> values;
            if (path2LoadedValues.containsKey(valuesPathString)) {
                values = path2LoadedValues.get(valuesPathString);
                valuesHeader = path2LoadedValuesHeader.get(valuesPathString);
            } else {
                reader = fileResolver.apply(valuesPathString);
                String line = reader.readLine();
                if (valuesPathString.endsWith(".link")) {
                    if (line.startsWith("file://")) {
                        reader.close();
                        reader = fileResolver.apply(line.substring(7));
                        line = reader.readLine();
                    }
                }

                values = new ArrayList<>();

                // Check for header in first line
                if (line != null && line.length() > 0) {
                    if (checkForHeader && line.startsWith("#")) {
                        valuesHeader = (String[]) parseValue(line);
                        valuesHeader[0] = valuesHeader[0].substring(1);
                    } else {
                        values.add(parseValue(line));
                    }
                }

                while ((line = reader.readLine()) != null && line.length() > 0) {
                    values.add(parseValue(line));
                }
                path2LoadedValues.put(valuesPathString, values);
                path2LoadedValuesHeader.put(valuesPathString, valuesHeader);
            }
            allowedValues = values;
            fireContentsChanged(0, allowedValues.size());
        } catch (IOException e) {
            if (e.toString().endsWith("not found")) {
                path2LoadedValues.put(valuesPathString, null);
                path2LoadedValuesHeader.put(valuesPathString, null);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    protected Object parseValue(String val) {
        return val.split("\\t", -1);
    }

    /**
     * Make a copy of the argument.
     *
     * @return the argument copy
     */
    public abstract Argument copyArgument();
}
