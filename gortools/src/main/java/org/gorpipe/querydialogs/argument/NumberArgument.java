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
package org.gorpipe.querydialogs.argument;

import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.RangedNumberFormatter;

import java.net.URI;
import java.text.ParseException;
import java.util.List;

/**
 * Represents an argument that expects a numeric value.
 * <p>Provides support for specifying a minimum and/or a maximum allowed value</p>
 *
 * @author arnie
 * @version $Id$
 */
public class NumberArgument extends Argument {
    /**
     * The property name for the argument's numeric value.
     */
    public static final String PROPERTY_NUMBER_VALUE = "numberValue";

    private RangedNumberFormatter formatter;
    private int valueTextFieldMinWidth;
    private int minTextFieldMinWidth;
    private int maxTextFieldMinWidth;

    /**
     * @param min - the minimum allowed value
     * @param max - the maximum allowed value
     */
    public NumberArgument(ArgumentDescription argDescr, Boolean optional, Object defaultValue, List<? extends Object> allowedValues,
                          URI valuesPath, List<String> operators, Boolean advanced, Integer displayWidth, Number min, Number max, ArgumentType argType) {
        super(argType, argDescr, optional, defaultValue, allowedValues, valuesPath, operators, advanced, displayWidth);
        Double dmin = null, dmax = null;
        if (min != null) dmin = min.doubleValue();
        if (max != null) dmax = max.doubleValue();
        this.setMin(dmin);
        this.setMax(dmax);
    }

    /**
     * Constructor that copies the input number argument.
     *
     * @param numbArg the number argument to copy
     */
    public NumberArgument(final NumberArgument numbArg) {
        super(numbArg);
        this.formatter = numbArg.formatter;
        this.valueTextFieldMinWidth = numbArg.valueTextFieldMinWidth;
        this.minTextFieldMinWidth = numbArg.minTextFieldMinWidth;
        this.maxTextFieldMinWidth = numbArg.maxTextFieldMinWidth;
    }

    private RangedNumberFormatter getFormatter() {
        if (formatter == null) {
            formatter = new RangedNumberFormatter(getMin(), getMax());
        }
        return formatter;
    }

    public Double getMin() {
        if (formatter == null) return null;
        return (Double) getFormatter().getMinimum();
    }

    public void setMin(Double min) {
        getFormatter().setMinimum(min);
        if (getNumberValue() != null && min != null && min.compareTo(getNumberValue().doubleValue()) > 0) {
            setNumberValue(min);
        }
    }

    public Double getMax() {
        if (formatter == null) return null;
        return (Double) getFormatter().getMaximum();
    }

    public void setMax(Double max) {
        getFormatter().setMaximum(max);
        if (getNumberValue() != null && max != null && max.compareTo(getNumberValue().doubleValue()) < 0) {
            setNumberValue(max);
        }
    }

    @Override
    public void setValue(Object value) {
        if (value != null && !(value instanceof Number)) {
            try {
                value = getFormatter().stringToValue(value.toString());
            } catch (ParseException e) {
                value = null;
            }
        }
        setNumberValue((Number) value);
    }

    public Number getNumberValue() {
        if (getValue() == null) return null;
        try {
            return (Number) getFormatter().stringToValue((String) getValue());
        } catch (ParseException e) {
            return null;
        }
    }

    public void setNumberValue(Number value) {
        try {
            super.setValue(getFormatter().valueToString(value));
        } catch (ParseException e) {
            super.setValue(null);
        }
    }

    @Override
    protected Object parseValue(String val) {
        try {
            return Integer.valueOf(val);
        } catch (NumberFormatException e) {
            return Double.valueOf(val);
        }
    }

    /**
     * The minium width of a text field for the argument value. Used for example if the argument belongs to a group.
     *
     * @return the minium width of a text field for the argument value
     */
    public int getValueTextFieldMinWidth() {
        return valueTextFieldMinWidth;
    }

    /**
     * Set the minium width of a text field for the argument value. Used for example if the argument belongs to a group.
     *
     * @param valueTextFieldMinWidth the width value to set
     */
    public void setValueTextFieldMinWidth(final int valueTextFieldMinWidth) {
        this.valueTextFieldMinWidth = valueTextFieldMinWidth;
    }

    /**
     * Get the minium width of a text field for the argument minimum value. Used for example if the argument belongs to a group.
     *
     * @return the minium width of a text field for the argument minimum value
     */
    public int getMinTextFieldMinWidth() {
        return minTextFieldMinWidth;
    }

    /**
     * Set the minium width of a text field for the argument minimum value. Used for example if the argument belongs to a group.
     *
     * @param minTextFieldMinWidth the width to set
     */
    public void setMinTextFieldMinWidth(final int minTextFieldMinWidth) {
        this.minTextFieldMinWidth = minTextFieldMinWidth;
    }

    /**
     * Get the minium width of a text field for the argument maximum value. Used for example if the argument belongs to a group.
     *
     * @return the minium width of a text field for the argument maximum value
     */
    public int getMaxTextFieldMinWidth() {
        return maxTextFieldMinWidth;
    }

    /**
     * Set the minium width of a text field for the argument maximum value. Used for example if the argument belongs to a group.
     *
     * @param maxTextFieldMinWidth the width to set
     */
    public void setMaxTextFieldMinWidth(final int maxTextFieldMinWidth) {
        this.maxTextFieldMinWidth = maxTextFieldMinWidth;
    }

    @Override
    public NumberArgument copyArgument() {
        return new NumberArgument(this);
    }
}
