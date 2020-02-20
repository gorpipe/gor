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

package gorsat;

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UTestSetColType {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void singleColumn() {
        String query = "gorrow 1,1 | calc data '123' | setcoltype data i | coltype";
        final String result = TestUtils.runGorPipe(query);
        final String expected = "chrom\tpos\tdata\n" +
                "chr1\t1\tI(123)\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void singleColumnWithLongName() {
        String query = "gorrow 1,1 | calc data '123' | setcoltype data integer | coltype";
        final String result = TestUtils.runGorPipe(query);
        final String expected = "chrom\tpos\tdata\n" +
                "chr1\t1\tI(123)\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void multipleColumns() {
        String query = "gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype x,y,z s,s,s | coltype";
        final String result = TestUtils.runGorPipe(query);
        final String expected = "chrom\tpos\tx\ty\tz\n" +
                "chr1\t1\tS(42)\tS(3.14)\tS(45.14)\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void multipleColumnsWithLongNames() {
        String query = "gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype x,y,z string,STRING,string | coltype";
        final String result = TestUtils.runGorPipe(query);
        final String expected = "chrom\tpos\tx\ty\tz\n" +
                "chr1\t1\tS(42)\tS(3.14)\tS(45.14)\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void unspecifiedColumnsRetainTheirType() {
        String query = "gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype z s | coltype";
        final String result = TestUtils.runGorPipe(query);
        final String expected = "chrom\tpos\tx\ty\tz\n" +
                "chr1\t1\tI(42)\tD(3.14)\tS(45.14)\n";
        Assert.assertEquals(expected, result);
    }

    @Test
    public void unknownTypeShouldThrowError() {
        expected.expect(GorParsingException.class);
        String query = "gorrow 1,1 | calc data '123' | setcoltype data x | coltype";
        TestUtils.runGorPipe(query);
    }

    @Test
    public void unknownLongTypeShouldThrowError() {
        expected.expect(GorParsingException.class);
        String query = "gorrow 1,1 | calc data '123' | setcoltype data longint | coltype";
        TestUtils.runGorPipe(query);
    }

    @Test
    public void numColumnsShouldMatchNumTypes_TypeMissing() {
        expected.expect(GorParsingException.class);
        String query = "gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype x,y,z s,s | coltype";
        TestUtils.runGorPipe(query);
    }

    @Test
    public void numColumnsShouldMatchNumTypes_TypeExtra() {
        expected.expect(GorParsingException.class);
        String query = "gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype x,z s,s,s | coltype";
        TestUtils.runGorPipe(query);
    }

    @Test
    public void chromShouldThrowError() {
        expected.expect(GorParsingException.class);
        String query = "gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype 1 I | coltype";
        TestUtils.runGorPipe(query);
    }
}
