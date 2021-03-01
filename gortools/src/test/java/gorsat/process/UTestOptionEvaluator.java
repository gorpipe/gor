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

package gorsat.process;

import gorsat.TestUtils;
import org.gorpipe.gor.session.GorContext;
import org.junit.Assert;
import org.junit.Test;

public class UTestOptionEvaluator {
    @Test
    public void getValue() {
        GorContext gorContext = new GenericSessionFactory().create().getGorContext();
        OptionEvaluator optionEvaluator = new OptionEvaluator(gorContext);
        String value = optionEvaluator.getValue("gorrow 1,1 | calc x 42", 3);
        Assert.assertEquals("42", value);
    }

    @Test
    public void getValueAsInputSourceArgument() {
        String query = "norrows getvalue(gorrow 1,1 | calc x 2, 3)";
        String result = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\n" +
                "chrN\t0\t0\n" +
                "chrN\t0\t1\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getValueAsPipeCommandArgument() {
        String query = "norrows 100 | top getvalue(gorrow 1,1 | calc x 2, 3)";
        String result = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\n" +
                "chrN\t0\t0\n" +
                "chrN\t0\t1\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getValueAsPipeCommandArgumentInsideNestedQuery() {
        String query = "nor <(norrows 100 | top getvalue(gorrow 1,1 | calc x 2, 3))";
        String result = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\n" +
                "chrN\t0\t0\n" +
                "chrN\t0\t1\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getValueFromCreateStep() {
        String query = "create xxx = gorrow 1,1 | calc x 2; nor <(norrows 100 | top getvalue([xxx], 3))";
        String result = TestUtils.runGorPipe(query);
        String expected = "ChromNOR\tPosNOR\tRowNum\n" +
                "chrN\t0\t0\n" +
                "chrN\t0\t1\n";
        Assert.assertEquals(expected, result);
    }
}