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

package gorsat.process;

import org.gorpipe.exceptions.GorSystemException;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class UTestNorStreamIterator {
    @Test
    public void shouldThrowWithEmptyStreamDueToHeaders(){
        Assert.assertThrows("Unable to initialize iterator from stream.", GorSystemException.class, () -> {
            Supplier<Stream<String>> emptyStreamSupplier = () -> Stream.of();
            NorStreamIterator iterator = new NorStreamIterator(emptyStreamSupplier);
        });
    }

    @Test
    public void shouldTreatOneRowAsHeader(){
        Supplier<Stream<String>> oneStreamSupplier = () -> Stream.of("colname1\tcolname2");
        NorStreamIterator iterator = new NorStreamIterator(oneStreamSupplier);

        String header = iterator.getHeader();
        assertEquals("ChromNOR\tPosNOR\tcolname1\tcolname2", header);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldGetDataWithPaddedChromPos(){
        Supplier<Stream<String>> twoStreamSupplier = () -> Stream.of("colname1\tcolname2", "val1\tval2");
        NorStreamIterator iterator = new NorStreamIterator(twoStreamSupplier);

        assertTrue(iterator.hasNext());
        assertEquals("chrN\t0\tval1\tval2",iterator.next());
        assertFalse(iterator.hasNext());
    }
}
