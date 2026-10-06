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
import org.junit.Test;

public class UTestThrowIf {

    private static GorDataException runExpectingThrow(String query) {
        try {
            TestUtils.runGorPipe(query);
        } catch (GorDataException e) {
            return e;
        }
        Assert.fail("Expected GorDataException for: " + query);
        return null;
    }

    @Test
    public void defaultMessageUnchanged() {
        GorDataException e = runExpectingThrow("gorrow chr1,1 | calc status 'unmapped' | throwif status != 'mapped'");
        Assert.assertEquals("Gor throw on: status != 'mapped'", e.getMessage());
        Assert.assertEquals("", e.getRow());
        Assert.assertFalse(e.isFullRetry());
    }

    @Test
    public void customMessage() {
        GorDataException e = runExpectingThrow("gorrow chr1,1 | calc status 'unmapped' | throwif -m 'liftover failed' status != 'mapped'");
        Assert.assertEquals("liftover failed", e.getMessage());
        Assert.assertEquals("", e.getRow());
    }

    @Test
    public void customMessageWithQuotes() {
        GorDataException e = runExpectingThrow("gorrow chr1,1 | calc status 'unmapped' | throwif -m \"it's 'not' mapped\" status != 'mapped'");
        Assert.assertEquals("it's 'not' mapped", e.getMessage());
    }

    @Test
    public void detailIncludesOnlyOffendingRow() {
        GorDataException e = runExpectingThrow("gorrows -p chr1:1-4 | calc status if(pos=2,'unmapped','mapped') | throwif -d status != 'mapped'");
        Assert.assertEquals("Gor throw on: status != 'mapped'\nHeader: chrom\tpos\tstatus\nRow: chr1\t2\tunmapped", e.getMessage());
        Assert.assertEquals("chr1\t2\tunmapped", e.getRow());
    }

    @Test
    public void customMessageAndDetail() {
        GorDataException e = runExpectingThrow("gorrow chr1,1 | calc status 'unmapped' | throwif -m 'liftover failed' -d status != 'mapped'");
        Assert.assertEquals("liftover failed\nHeader: chrom\tpos\tstatus\nRow: chr1\t1\tunmapped", e.getMessage());
    }

    @Test
    public void customMessageAndDetailWithRetriable() {
        GorDataException e = runExpectingThrow("gorrow chr1,1 | calc status 'unmapped' | throwif -retriable -d -m 'liftover failed' status != 'mapped'");
        Assert.assertEquals("liftover failed\nHeader: chrom\tpos\tstatus\nRow: chr1\t1\tunmapped", e.getMessage());
        Assert.assertTrue(e.isFullRetry());
    }

    @Test
    public void norContext() {
        GorDataException e = runExpectingThrow("norrows 3 | calc status if(rownum=1,'unmapped','mapped') | throwif -m 'liftover failed' -d status != 'mapped'");
        Assert.assertEquals("liftover failed\nHeader: RowNum\tstatus\nRow: 1\tunmapped", e.getMessage());
        Assert.assertEquals("1\tunmapped", e.getRow());
    }
}
