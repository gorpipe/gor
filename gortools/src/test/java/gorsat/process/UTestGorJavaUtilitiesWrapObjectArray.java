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

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;

public class UTestGorJavaUtilitiesWrapObjectArray {

    @Test
    public void shouldHandleNormalValuesInArray() {
        List<Object[]> list = Arrays.asList(new Object[]{"", 1.2}, new Object[]{"a", 1.3});
        Iterator<Object[]> iter = list.iterator();
        Stream<String> stream = GorJavaUtilities.wrapObjectArrayIterator(iter);
        stream.forEach(UTestGorJavaUtilitiesWrapObjectArray::testString);
    }

    @Test
    public void shouldHandleNullValuesInArray() {
        List<Object[]> list = Arrays.asList(new Object[]{"", 1.2}, new Object[]{"", null});
        Iterator<Object[]> iter = list.iterator();
        Stream<String> stream = GorJavaUtilities.wrapObjectArrayIterator(iter);
        stream.forEach(UTestGorJavaUtilitiesWrapObjectArray::testString);
    }

    private static void testString(String s) {
        assertNotNull(s);
    }
}
