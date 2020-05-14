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

package org.gorpipe.util.collection.extract;

import org.gorpipe.util.collection.IntHashSet;
import org.gorpipe.util.collection.MultiMap;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


/**
 * Test the Extract class
 *
 * @version $Id$
 */
public class UTestExtract {

    private static final Logger log = LoggerFactory.getLogger(UTestExtract.class);

    /**
     * Test extract dates and date strings
     */
    @Test
    public void testDates() {
        final Date now = new Date();

        final long lnow = Extract.datetime(now);
        final long lnull = Extract.datetime(null);

        final String snow = Extract.dateString(lnow);
        final String sdtnow = Extract.datetimeString(lnow);
        final String snull = Extract.dateString(lnull);

        Assert.assertEquals("", snull);
        Assert.assertTrue(sdtnow.startsWith(snow));

        final String tnow = Extract.timeString(lnow);
        Assert.assertNotNull(tnow);

        final String dur = Extract.durationString(1500);
        log.debug("{}", dur);
        Assert.assertNotNull(dur);
        Assert.assertEquals("1s 500ms.", dur);

        final long testCurrentTimeMillis = System.currentTimeMillis();
        Extract.currentTimeProvider = () -> testCurrentTimeMillis;
        final String s = Extract.durationStringSince(testCurrentTimeMillis - 1500);
        Extract.currentTimeProvider = () -> System.currentTimeMillis();
        assert s != null;
        Assert.assertEquals("1s 500ms.", s);
    }

    /**
     * Test nvl functions
     */
    @Test
    public void testNvl() {
        Assert.assertEquals("-42", Extract.nvl(null, "-42"));
        Assert.assertEquals("42", Extract.nvl("42", "-42"));

        final int[] iarr = {10, 4, -12};
        Assert.assertEquals("[10, 4, -12]", Extract.nvlToString(iarr, ""));
        Assert.assertEquals("", Extract.nvlToString(null, ""));
        final Integer ival = -344;
        Assert.assertEquals("-344", Extract.nvlToString(ival, ""));
        Assert.assertEquals("", Extract.nvlToString(null, ""));
    }

    /**
     * Test into function
     */
    @Test
    public void testInto() {
        List<Integer> a = Arrays.asList(0);
        List<Integer> b = new ArrayList<Integer>();
        List<Integer> c = Arrays.asList(1, 2, 3);
        List<Integer> d = Arrays.asList(4, 5, 6);
        List<Integer> e = Arrays.asList(7, 8, 9);

        Integer[] values = new Integer[10];
        Extract.into(values, 0, a);
        Extract.into(values, 1, b);
        Extract.into(values, 1, c);
        Extract.into(values, 4, d);
        Extract.into(values, 7, e);

        for (int i = 0; i < values.length; i++) {
            Assert.assertEquals(i, values[i].intValue());
        }
    }

    /**
     * Test simple array and set operations
     */
    @Test
    public void testSimpeOps() {
        final int[] a = Extract.arrayI(1, 2, 3, 4, 5, 6);
        Assert.assertEquals(6, a.length);
        for (int i = 0; i < a.length; i++) {
            Assert.assertEquals(i + 1, a[i]);
        }

        final IntHashSet s = Extract.setI(1, 2, 3, 4, 5, 6);
        Assert.assertEquals(6, s.size());
        for (int i = 0; i < s.size(); i++) {
            Assert.assertTrue(s.contains(i + 1));
        }

        final String[] b = Extract.array("one", "two", "three", "four");
        Assert.assertEquals(4, b.length);
        Assert.assertEquals("one", b[0]);
        Assert.assertEquals("two", b[1]);
        Assert.assertEquals("three", b[2]);
        Assert.assertEquals("four", b[3]);

        final HashSet<String> t = Extract.set("one", "two", "three", "four");
        Assert.assertEquals(4, t.size());
        Assert.assertTrue(t.contains("one"));
        Assert.assertTrue(t.contains("two"));
        Assert.assertTrue(t.contains("three"));
        Assert.assertTrue(t.contains("four"));
    }

    /**
     * Test extract ints from objects
     */
    @Test
    public void testInts() {
        final GetInt<MyObj> iget = new GetInt<MyObj>() {
            public int get(MyObj v) {
                return v.ival;
            }
        };
        final MyObj[] avalues = {new MyObj(0), new MyObj(1), new MyObj(2),
                new MyObj(3), new MyObj(4), new MyObj(5),
                new MyObj(6), new MyObj(7), new MyObj(8), new MyObj(9), null
        };

        int[] ivalues = Extract.array(avalues, iget);
        Assert.assertEquals(ivalues.length, avalues.length - 1);
        for (int i = 0; i < ivalues.length; i++) {
            Assert.assertEquals(avalues[i].ival, ivalues[i]);
        }

        final List<MyObj> lvalues = Arrays.asList(avalues);
        ivalues = Extract.array(lvalues, iget);
        Assert.assertEquals(ivalues.length, avalues.length - 1);
        for (int i = 0; i < ivalues.length; i++) {
            Assert.assertEquals(lvalues.get(i).ival, ivalues[i]);
        }

        final MyObj[] a2values = new MyObj[avalues.length * 2];
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < avalues.length; i++) {
                a2values[k * avalues.length + i] = avalues[i];
            }
        }

        IntHashSet set = Extract.set(a2values, iget);
        assert set != null;
        Assert.assertEquals(avalues.length - 1, set.size());

        final List<MyObj> lsetvalues = Arrays.asList(a2values);
        set = Extract.set(lsetvalues, iget);
        assert set != null;
        Assert.assertEquals(avalues.length - 1, set.size());

        final Map<Integer, MyObj> mymap = Extract.map(avalues, iget);
        assert mymap != null;
        Assert.assertEquals(avalues.length - 1, mymap.size());
        for (MyObj o : avalues) {
            if (o != null) {
                final MyObj o2 = mymap.get(o.ival);
                Assert.assertEquals(o, o2);
            }
        }

        final MultiMap<Integer, MyObj> mmap = Extract.multimap(a2values, iget);
        assert mmap != null;
        Assert.assertEquals(10, mmap.size());
        Assert.assertEquals(20, mmap.countValues());

        final Map<String, String> map = Extract.mapKeyValues("key1", "value1", "key2", "value2", "key3", "value3");
        Assert.assertEquals(3, map.size());
        Assert.assertEquals("value1", map.get("key1"));
        Assert.assertEquals("value2", map.get("key2"));
        Assert.assertEquals("value3", map.get("key3"));
    }

    /**
     * Test extract Strings from objects
     */
    @Test
    public void testStrings() {
        final GetString<MyObj> sget = new GetString<MyObj>() {
            public String get(MyObj v) {
                return String.valueOf(v.ival);
            }
        };
        final MyObj[] avalues = {new MyObj(0), new MyObj(1), new MyObj(2),
                new MyObj(3), new MyObj(4), new MyObj(5),
                new MyObj(6), new MyObj(7), new MyObj(8), new MyObj(9), null
        };

        final HashSet<String> dups = Extract.duplicates(avalues, sget);
        Assert.assertEquals(0, dups.size());

        String[] ivalues = Extract.array(avalues, sget);
        Assert.assertEquals(ivalues.length, avalues.length - 1);
        for (int i = 0; i < ivalues.length; i++) {
            Assert.assertEquals(avalues[i].ival, Integer.parseInt(ivalues[i]));
        }

        final List<MyObj> lvalues = Arrays.asList(avalues);
        ivalues = Extract.array(lvalues, sget);
        Assert.assertEquals(ivalues.length, avalues.length - 1);
        for (int i = 0; i < ivalues.length; i++) {
            Assert.assertEquals(lvalues.get(i).ival, Integer.parseInt(ivalues[i]));
        }

        final MyObj[] a2values = new MyObj[avalues.length * 2];
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < avalues.length; i++) {
                a2values[k * avalues.length + i] = avalues[i];
            }
        }

        HashSet<String> set = Extract.set(a2values, sget);
        assert set != null;
        Assert.assertEquals(avalues.length - 1, set.size());

        final List<MyObj> lsetvalues = Arrays.asList(a2values);
        set = Extract.set(lsetvalues, sget);
        assert set != null;
        Assert.assertEquals(avalues.length - 1, set.size());

        final Map<String, MyObj> mymap = Extract.map(avalues, sget);
        assert mymap != null;
        Assert.assertEquals(avalues.length - 1, mymap.size());
        for (MyObj o : avalues) {
            if (o != null) {
                final MyObj o2 = mymap.get(String.valueOf(o.ival));
                Assert.assertEquals(o, o2);
            }
        }

        final MultiMap<String, MyObj> mmap = Extract.multimap(a2values, sget);
        assert mmap != null;
        Assert.assertEquals(10, mmap.size());
        Assert.assertEquals(20, mmap.countValues());

        Assert.assertEquals("kalli/palli/nalli", Extract.string("/", "kalli", "palli", "nalli"));

        final String[] names = {"A1", "B1", "b2", "a2", "a3", "z39", "qd9", "Palli", "Kalli", "Nalli"};
        final MultiMap<String, Object> m = Extract.multimap(names, new GetString<Object>() {
            public String get(Object v) {
                return String.valueOf(Character.toUpperCase(v.toString().charAt(0)));
            }
        });
        final String[] keys = m.keySet().toArray(new String[m.size()]);
        Arrays.sort(keys);
        for (String k : keys) {
            Assert.assertNotNull(k);
            for (Object v : m.get(k)) {
                Assert.assertNotNull(v);
            }
        }
    }

    /**
     * Testing conversion to ArrayList
     */
    @Test
    public void testArrayList() {
        String[] strings = {"Alpha", "Beta", "Gamma"};
        Integer[] ints = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        ArrayList<String> strList = Extract.arrayList(strings);
        for (int i = 0; i < strings.length; i++) {
            Assert.assertEquals(strings[i], strList.get(i));
        }

        ArrayList<Integer> intList = Extract.arrayList(ints);
        for (int i = 0; i < ints.length; i++) {
            Assert.assertEquals(ints[i], intList.get(i));
        }

        final String[] results = Extract.minus(strings, "Gamma");
        Assert.assertEquals(2, results.length);
        Assert.assertEquals("Alpha", results[0]);
        Assert.assertEquals("Beta", results[1]);

        final String[] distincts = Extract.distinct("Alpha", "Alpha", "Beta", "Beta", "Gamma", "Beta");
        Arrays.sort(distincts);
        Assert.assertEquals(3, distincts.length);
        for (int i = 0; i < distincts.length; i++) {
            Assert.assertEquals(distincts[i], strings[i]);
        }
    }

    /**
     * Test MD5 hashing utilities methods
     *
     * @throws IOException
     */
    @Test
    public void testMd5() throws IOException {
        final String text = "dkdkdk dkjf dkdkdalk39 93 3944kd ikdkdsfjasdlfk afodsf sdf9ds fdsf7423348d 8d8 8d r8s7d d kd kdkkdkddkdkkdkdkdked dfd";
        final String hash1 = Extract.md5(text);
        final String hash2 = Extract.hex(Extract.md5Bytes(text));
        Assert.assertEquals(hash1, hash2);

        final Path file = Files.createTempFile("test", ".txt");
        Files.write(file, text.getBytes());
        final String hash3 = Extract.md5(file);
        Assert.assertEquals(hash1, hash3);
        final String hash4 = Extract.digest(file, "MD5");
        Assert.assertEquals(hash3, hash4);
    }

    /**
     * Test MD5 on directory
     *
     * @throws IOException
     */
    @Test
    public void testMd5Directory() throws IOException {
        final String text = "dkdkdk dkjf dkdkdalk39 93 3944kd ikdkdsfjasdlfk afodsf sdf9ds fdsf7423348d 8d8 8d r8s7d d kd kdkkdkddkdkkdkdkdked dfd";
        Path dir = null;
        try {
            dir = Files.createTempDirectory("testmd5");
            final Path file1 = dir.resolve("test.txt");
            Files.write(file1, text.getBytes());
            final Path file2 = dir.resolve("test2.txt");
            Files.write(file2, text.getBytes());

            byte[] textbytes = text.getBytes();
            byte[] combined = new byte[textbytes.length*2];
            System.arraycopy(textbytes, 0, combined, 0, textbytes.length);
            System.arraycopy(textbytes, 0, combined, textbytes.length, textbytes.length);
            final String hash = Extract.md5(dir);
            final String hashtest = Extract.md5(combined);
            Assert.assertEquals(hash, hashtest);
        } finally {
            if(dir != null && Files.exists(dir)) Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // don't care
                }
            });
        }
    }

    static class MyObj {
        final int ival;

        MyObj(int ival) {
            this.ival = ival;
        }
    }
}
