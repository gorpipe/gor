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

package org.gorpipe.base.config.converters;

import org.junit.Test;

import static org.junit.Assert.*;

public class UTestConverterUtil {

    @Test
    public void testNumericAndUnit() {
        String[] parts = ConverterUtil.splitNumericAndChar("100MB");
        assertEquals("100", parts[0]);
        assertEquals("MB", parts[1]);
    }

    @Test
    public void testNumericAndUnitWithSpace() {
        String[] parts = ConverterUtil.splitNumericAndChar("100 MB");
        assertEquals("100", parts[0]);
        assertEquals("MB", parts[1]);
    }

    @Test
    public void testNumericOnly() {
        String[] parts = ConverterUtil.splitNumericAndChar("42");
        assertEquals("42", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    public void testUnitOnly() {
        String[] parts = ConverterUtil.splitNumericAndChar("GB");
        assertEquals("", parts[0]);
        assertEquals("GB", parts[1]);
    }

    @Test
    public void testLeadingWhitespace() {
        String[] parts = ConverterUtil.splitNumericAndChar("  256 KiB");
        assertEquals("256", parts[0]);
        assertEquals("KiB", parts[1]);
    }

    @Test
    public void testDecimalValue() {
        String[] parts = ConverterUtil.splitNumericAndChar("1.5GB");
        assertEquals("1.5", parts[0]);
        assertEquals("GB", parts[1]);
    }

    @Test
    public void testLowerCaseUnit() {
        String[] parts = ConverterUtil.splitNumericAndChar("10mb");
        assertEquals("10", parts[0]);
        assertEquals("mb", parts[1]);
    }

    @Test
    public void testMultiCharUnit() {
        String[] parts = ConverterUtil.splitNumericAndChar("1GiB");
        assertEquals("1", parts[0]);
        assertEquals("GiB", parts[1]);
    }

    @Test
    public void testEmptyString() {
        String[] parts = ConverterUtil.splitNumericAndChar("");
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }
}
