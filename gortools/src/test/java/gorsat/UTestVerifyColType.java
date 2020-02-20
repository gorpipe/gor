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

import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

public class UTestVerifyColType {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void chromPosOnly() {
        String[] lines = {
                "Chrom\tPos",
                "chr1\t1",
                "chr2\t23"};

        assertQuery(lines, lines, "gor %s | verifycoltype");
    }

    @Test
    public void typesNotDetermined() {
        String[] lines = {
                "Chrom\tPos\tData",
                "chr1\t1\t1",
                "chr2\t23\t2"};

        assertQuery(lines, lines, "gor %s | verifycoltype");
    }

    @Test
    public void typesMatch() {
        String[] lines = {
                "Chrom\tPos\tData",
                "chr1\t1\t1",
                "chr2\t23\t2"};

        String[] expected = {
                "Chrom\tPos\tData\tx",
                "chr1\t1\t1\t2",
                "chr2\t23\t2\t4"};

        assertQuery(lines, expected, "gor %s | calc x Data*2 | verifycoltype");
    }

    @Test
    public void typesDontMatchInt() {
        String[] lines = {
                "Chrom\tPos\tData",
                "chr1\t1\t1",
                "chr2\t23\t2"};

        String[] expected = {
                "Chrom\tPos\tData\tx",
                "chr1\t1\t1\t2",
                "chr2\t23\t2\t4"};

        exception.expect(GorDataException.class);
        assertQuery(lines, expected, "gor %s | calc x 'bingo' | setcoltype x i | verifycoltype");
    }

    @Test
    public void typesDontMatchLong() {
        String[] lines = {
                "Chrom\tPos\tData",
                "chr1\t1\t1",
                "chr2\t23\t2"};

        String[] expected = {
                "Chrom\tPos\tData\tx",
                "chr1\t1\t1\t2",
                "chr2\t23\t2\t4"};

        exception.expect(GorDataException.class);
        assertQuery(lines, expected, "gor %s | calc x 'bingo' | setcoltype x l | verifycoltype");
    }

    @Test
    public void typesDontMatchDouble() {
        String[] lines = {
                "Chrom\tPos\tData",
                "chr1\t1\t1",
                "chr2\t23\t2"};

        String[] expected = {
                "Chrom\tPos\tData\tx",
                "chr1\t1\t1\t2",
                "chr2\t23\t2\t4"};

        exception.expect(GorDataException.class);
        assertQuery(lines, expected, "gor %s | calc x 'bingo' | setcoltype x d | verifycoltype");
    }

    private void assertQuery(String[] lines, String[] expected, String formatString) {
        final File gorFile = TestUtils.createGorFile("UTestVerifyColType", lines);
        String query = String.format(formatString, gorFile);
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals(String.join("\n", expected) + "\n", result);
    }
}
