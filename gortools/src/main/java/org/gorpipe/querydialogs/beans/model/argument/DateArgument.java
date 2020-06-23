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
package org.gorpipe.querydialogs.beans.model.argument;

import org.gorpipe.querydialogs.beans.model.Argument;
import org.gorpipe.querydialogs.beans.model.ArgumentDescription;
import org.gorpipe.querydialogs.beans.model.ArgumentType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Represents an argument that expects a date value.
 *
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings({"javadoc", "serial"})
public class DateArgument extends Argument {
    /**
     * The property name for the argument's date value.
     */
    public static final String PROPERTY_DATE_VALUE = "dateValue";
    /**
     * The date format used.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final DateFormat FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    /**
     * Constructor.
     */
    public DateArgument(ArgumentDescription argDescr, Boolean optional, Object defaultValue, List<? extends Object> allowedValues,
                        List<String> operators, Boolean advanced, Integer displayWidth) {
        super(ArgumentType.DATE, argDescr, optional, defaultValue, allowedValues, null, operators, advanced, displayWidth);
    }

    /**
     * Constructor that copies the input date argument.
     *
     * @param arg the date argument to copy
     */
    public DateArgument(final DateArgument arg) {
        super(arg);
    }

    @Override
    protected Object parseValue(String val) {
        try {
            synchronized (FORMATTER) {
                return FORMATTER.parseObject(val);
            }
        } catch (ParseException e) {
            throw new RuntimeException("Invalid date format", e);
        }
    }

    @Override
    public DateArgument copyArgument() {
        return new DateArgument(this);
    }
}
