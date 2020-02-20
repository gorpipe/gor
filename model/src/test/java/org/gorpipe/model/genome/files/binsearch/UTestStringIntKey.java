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

package org.gorpipe.model.genome.files.binsearch;

import org.gorpipe.model.genome.files.binsearch.StringIntKey;
import org.junit.Assert;
import org.junit.Test;

public class UTestStringIntKey {

    @Test
    public void test_createKey() {
        final StringIntKey comparator = new StringIntKey(0, 1, StringIntKey.cmpLexico);
        final byte[] buffer = "chr1\t1\taaaaaaa".getBytes();
        final StringIntKey key = comparator.createKey(buffer, buffer.length, 0);
        Assert.assertEquals("chr1", key.chr);
        Assert.assertEquals(1, key.bpair);
    }

    @Test
    public void test_createKey_KeysAtEnd() {
        final StringIntKey comparator = new StringIntKey(1, 2, StringIntKey.cmpLexico);
        final byte[] buffer = "aaaaaaa\tchr1\t1".getBytes();
        final StringIntKey key = comparator.createKey(buffer, buffer.length, 0);
        Assert.assertEquals("chr1", key.chr);
        Assert.assertEquals(1, key.bpair);
    }

    @Test
    public void test_createKeyWhenKeyOnly() {
        final StringIntKey comparaTor = new StringIntKey(0, 1, StringIntKey.cmpLexico);
        final byte[] buffer = "chr1\t1\n".getBytes();
        final StringIntKey key = comparaTor.createKey(buffer, buffer.length, 0);
        Assert.assertEquals("chr1", key.chr);
        Assert.assertEquals(1, key.bpair);
    }
}
