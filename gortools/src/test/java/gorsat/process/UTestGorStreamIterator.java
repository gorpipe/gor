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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UTestGorStreamIterator {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowWithEmptyStreamDueToHeaders(){
        expectedException.expect(GorSystemException.class);
        expectedException.expectMessage("Unable to initialize iterator from stream.");

        Supplier<Stream<String>> emptyStreamSupplier = () -> Stream.of();
        GorStreamIterator iterator = new GorStreamIterator(emptyStreamSupplier, false);
        //we expect this to fail in constructor as is, if we get passed constructor we need to fail the test
        fail("Expected to get exception in constructor!");
    }

    @Test
    public void shouldTreatFirstRowAsHeaderWithoutScope(){
        Supplier<Stream<String>> oneStreamSupplier = () -> Stream.of("colname1\tcolname2");
        GorStreamIterator iterator = new GorStreamIterator(oneStreamSupplier, false);

        String header = iterator.getHeader();
        assertEquals("colname1\tcolname2", header);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldGetSameDataWithoutScope(){
        Supplier<Stream<String>> twoStreamSupplier = () -> Stream.of("colname1\tcolname2", "val1\tval2");
        GorStreamIterator iterator = new GorStreamIterator(twoStreamSupplier, false);

        assertTrue(iterator.hasNext());
        assertEquals("val1\tval2",iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldTreatFirstRowAsHeaderAndRemoveFirstColumnWithScope(){
        Supplier<Stream<String>> oneStreamSupplier = () -> Stream.of("project\tcolname1\tcolname2");
        GorStreamIterator iterator = new GorStreamIterator(oneStreamSupplier, true);

        String header = iterator.getHeader();
        assertEquals("colname1\tcolname2", header);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldGetRemoveFirstColumnDataWithScope(){
        Supplier<Stream<String>> twoStreamSupplier = () -> Stream.of("project\tcolname1\tcolname2", "val0\tval1\tval2");
        GorStreamIterator iterator = new GorStreamIterator(twoStreamSupplier, true);

        assertTrue(iterator.hasNext());
        assertEquals("val1\tval2",iterator.next());
        assertFalse(iterator.hasNext());
    }
}
