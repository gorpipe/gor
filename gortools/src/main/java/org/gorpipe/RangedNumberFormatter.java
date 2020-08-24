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
package org.gorpipe;

import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * A number formatter that can constrain values to a minimum and/or maximum value.
 *
 * @author arnie
 * @version $Id$
 */
public class RangedNumberFormatter extends NumberFormatter {
    private static final NumberFormat DEFAULT_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        DEFAULT_FORMAT.setGroupingUsed(false);
        DEFAULT_FORMAT.setParseIntegerOnly(false);
        if (DEFAULT_FORMAT instanceof DecimalFormat) {
            ((DecimalFormat) DEFAULT_FORMAT).setDecimalSeparatorAlwaysShown(false);
        }
    }

    /**
     * @param min the minimum allowed value (null permitted)
     * @param max the maximum allowed value (null permitted)
     */
    public RangedNumberFormatter(Double min, Double max) {
        super(DEFAULT_FORMAT);
        setAllowsInvalid(true);
        setCommitsOnValidEdit(true);
        if (min != null) setMinimum(min);
        if (max != null) setMaximum(max);
        setValueClass(Double.class);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        if (text == null || text.trim().isEmpty()) return null;
        return super.stringToValue(text);
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value == null) return "";
        return super.valueToString(value);
    }
}