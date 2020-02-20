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

import org.gorpipe.exceptions.GorDataException;
import gorsat.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class UTestDecodeFunction {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testDecodeMappedCol() {
        String query = "gorrow chr1,1,1 | calc foo 'a' | calc x decode(foo,'a,1,b,2,c,3,4')";
        String expected = "chrom\tbpStart\tbpStop\tfoo\tx\nchr1\t1\t1\ta\t1\n";
        TestUtils.assertGorpipeResults(expected, query);
        query = "gorrow chr1,1,1 | calc foo 'c' | calc x decode(foo,'a,1,b,2,c,3,4')";
        expected = "chrom\tbpStart\tbpStop\tfoo\tx\nchr1\t1\t1\tc\t3\n";
        TestUtils.assertGorpipeResults(expected, query);
    }

    @Test
    public void testDecodeMappedVal() {
        String query = "gorrow chr1,1,1 | calc x decode('a','a,1,b,2,c,3,4')";
        String expected = "chrom\tbpStart\tbpStop\tx\nchr1\t1\t1\t1\n";
        TestUtils.assertGorpipeResults(expected, query);
        query = "gorrow chr1,1,1 | calc x decode('c','a,1,b,2,c,3,4')";
        expected = "chrom\tbpStart\tbpStop\tx\nchr1\t1\t1\t3\n";
        TestUtils.assertGorpipeResults(expected, query);
    }

    @Test
    public void testDecodeNotFound() {
        String query = "gorrow chr1,1,1 | calc x decode('SomeVal','a,1,b,2,c,3,4')";
        String expected = "chrom\tbpStart\tbpStop\tx\nchr1\t1\t1\t4\n";
        TestUtils.assertGorpipeResults(expected, query);
    }

    @Test
    public void testDecodePassThrough() {
        String query = "gorrow chr1,1,1 | calc x decode('SomeVal','a,1,b,2,c,3')";
        String expected = "chrom\tbpStart\tbpStop\tx\nchr1\t1\t1\tSomeVal\n";
        TestUtils.assertGorpipeResults(expected, query);
    }

    @Test
    public void testDecodeNoMap() {
        exception.expect(GorDataException.class);
        exception.expectMessage("Error in step: CALC x decode(foo,'')");
        exception.expectMessage("The map parameter needs at least two values. Example: ...|calc x DECODE(col,'a,1')");
        TestUtils.runGorPipe("gorrow chr1,1,1 | calc foo = 'a' | calc x decode(foo,'')");
    }

    @Test
    public void testDecodeNoValue() {
        exception.expect(GorDataException.class);
        TestUtils.runGorPipe("gorrow chr1,1,1 | calc x decode('a','')");
    }


}
