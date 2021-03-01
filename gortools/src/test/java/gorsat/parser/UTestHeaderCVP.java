/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class UTestHeaderCVP {
    HeaderCVP cvp;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        cvp = new HeaderCVP("Chrom\tPos\tData");
    }

    @Test
    public void intValueColnum() {
        cvp.setCurrentColumn(2);
        int result = cvp.intValue(HeaderCVP.COLNUM);
        int expected = 2;
        Assert.assertEquals(expected, result);
    }

    @Test
    public void intValueColname() {
        cvp.setCurrentColumn(2);
        thrown.expect(GorDataException.class);
        cvp.intValue(HeaderCVP.COLNAME);
    }

    @Test
    public void longValueColnum() {
        cvp.setCurrentColumn(2);
        long result = cvp.longValue(HeaderCVP.COLNUM);
        long expected = 2L;
        Assert.assertEquals(expected, result);
    }

    @Test
    public void longValueColname() {
        cvp.setCurrentColumn(2);
        thrown.expect(GorDataException.class);
        cvp.longValue(HeaderCVP.COLNAME);
    }

    @Test
    public void doubleValueColnum() {
        cvp.setCurrentColumn(2);
        double result = cvp.doubleValue(HeaderCVP.COLNUM);
        double expected = 2.0;
        Assert.assertEquals(expected, result, 1e-6);
    }

    @Test
    public void doubleValueColname() {
        cvp.setCurrentColumn(2);
        thrown.expect(GorDataException.class);
        cvp.doubleValue(HeaderCVP.COLNAME);
    }

    @Test
    public void stringValueColnum() {
        cvp.setCurrentColumn(2);
        String result = cvp.stringValue(HeaderCVP.COLNUM);
        String expected = "2";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void stringValueColname() {
        cvp.setCurrentColumn(2);
        String result = cvp.stringValue(HeaderCVP.COLNAME);
        String expected = "Pos";
        Assert.assertEquals(expected, result);
    }
}