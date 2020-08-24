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

package org.gorpipe.model.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public class UTestUtil {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void isWindowsOS() {
        boolean isWin = Util.isWindowsOS();
        boolean isMac = Util.isOSX();

        Assert.assertTrue((isWin && !isMac) || (!isWin && isMac) || (!isWin && !isMac));
    }

    @Test
    public void nvl() {
        final String bingo = Util.nvl(null, "bingo");
        Assert.assertEquals("bingo", bingo);

        final String bongo = Util.nvl("bongo", "bingo");
        Assert.assertEquals("bongo", bongo);
    }

    @Test
    public void nvlToString() {
        final String bingo = Util.nvlToString(null, "bingo");
        Assert.assertEquals("bingo", bingo);

        final String bongo = Util.nvlToString("bongo", "bingo");
        Assert.assertEquals("bongo", bongo);
    }

    @Test
    public void md5() {
        Assert.assertEquals("3A3795BB61D5377545B4F345FF223E3D", Util.md5("bingo"));
    }

    @Test
    public void md5Bytes() {
        final byte[] expected = {0x3A, 0x37, (byte) 0x95, (byte) 0xBB, 0x61, (byte) 0xD5, 0x37, 0x75, 0x45, (byte) 0xB4, (byte) 0xF3, 0x45, (byte) 0xFF, 0x22, 0x3E, 0x3D};
        final byte[] md5Bytes = Util.md5Bytes("bingo");
        Assert.assertArrayEquals(expected, md5Bytes);
    }

    @Test
    public void isEmpty() {
        Assert.assertTrue(Util.isEmpty(null));
        Assert.assertFalse(Util.isEmpty("this is a test"));
    }

    @Test
    public void convert() throws Throwable {
        final PSQLException exception = new PSQLException("test", PSQLState.CONNECTION_DOES_NOT_EXIST);
        final Throwable converted = Util.convert(exception);
        expectedException.expect(SQLException.class);
        throw converted;
    }

    @Test
    public void readStream() throws IOException {
        String initialString = "this is the input";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
        final String result = Util.readStream(targetStream);
        Assert.assertEquals(initialString, result);
    }

    @Test
    public void readAndCloseStream() throws IOException {
        String initialString = "this is the input";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
        final String result = Util.readAndCloseStream(targetStream);
        Assert.assertEquals(initialString, result);
    }
}