package org.gorpipe.gor.driver.providers.stream;

import junit.framework.TestCase;

/**
 * Created by villi on 26/08/15.
 */
public class UTestRequestRange extends TestCase {

    public void testFromFirstLast() {
        try {
            RequestRange.fromFirstLast(1, 0);
            fail("Should not allow reversed range");
        } catch (IllegalArgumentException e) {
            // Ok
        }
        try {
            RequestRange.fromFirstLast(-1, -2);
            fail("Should not allow negatives");
        } catch (IllegalArgumentException e) {
            // Ok
        }

        try {
            RequestRange.fromFirstLast(-2, -1);
            fail("Should not allow negatives(2)");
        } catch (IllegalArgumentException e) {
            // Ok
        }
        try {
            RequestRange.fromFirstLast(-2, -1);
            fail("Should not allow negatives(2)");
        } catch (IllegalArgumentException e) {
            // Ok
        }

        RequestRange r = RequestRange.fromFirstLast(0, 0);
        assertEquals(0, r.getFirst());
        assertEquals(0, r.getLast());
        assertEquals(1, r.getLength());
        assertEquals("0-0", r.toString());

        r = RequestRange.fromFirstLast(100, 199);
        assertEquals(100, r.getFirst());
        assertEquals(199, r.getLast());
        assertEquals(100, r.getLength());
        assertEquals("100-199", r.toString());
    }

    public void testFromFirstLength() {
        try {
            RequestRange.fromFirstLength(-1, 10);
            fail("Should not allow  negative start");
        } catch (IllegalArgumentException e) {
            // Ok
        }

        try {
            RequestRange.fromFirstLength(1, 0);
            fail("Should not allow  zero length");
        } catch (IllegalArgumentException e) {
            // Ok
        }

        try {
            RequestRange.fromFirstLength(1, -10);
            fail("Should not allow  negative length");
        } catch (IllegalArgumentException e) {
            // Ok
        }

        RequestRange r = RequestRange.fromFirstLength(0, 1);
        assertEquals(0, r.getFirst());
        assertEquals(0, r.getLast());
        assertEquals(1, r.getLength());
        assertEquals("0-0", r.toString());

        r = RequestRange.fromFirstLength(100, 100);
        assertEquals(100, r.getFirst());
        assertEquals(199, r.getLast());
        assertEquals(100, r.getLength());
        assertEquals("100-199", r.toString());

    }

}
