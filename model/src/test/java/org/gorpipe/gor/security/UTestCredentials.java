/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.gor.security;

import org.gorpipe.gor.security.Credentials;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class UTestCredentials {

    private Credentials credentials;
    private Instant expiryDate;

    @Before
    public void setUp() {
        credentials = getCredentials();
    }

    @Test
    public void isNull() {
        assertTrue(Credentials.NULL.isNull());
        assertFalse(credentials.isNull());
    }
    @Test
    public void getService() {
        assertNull(Credentials.NULL.getService());
        assertEquals("bingo", credentials.getService());
    }

    @Test
    public void getLookupKey() {
        assertNull(Credentials.NULL.getLookupKey());
        assertEquals("bongo", credentials.getLookupKey());
    }

    @Test
    public void getOwnerType() {
        assertNull(Credentials.NULL.getOwnerType());
        assertEquals(Credentials.OwnerType.User, credentials.getOwnerType());
    }

    @Test
    public void getOwnerId() {
        assertNull(Credentials.NULL.getOwnerId());
        assertEquals("foo", credentials.getOwnerId());
    }

    @Test
    public void get_byName() {
        assertNull(Credentials.NULL.get("realm"));
        assertEquals("bar", credentials.get("realm"));
    }

    @Test
    public void get() {
        assertNull(Credentials.NULL.get(Credentials.Attr.REALM));
        assertEquals("bar", credentials.get(Credentials.Attr.REALM));
    }

    @Test
    public void keys() {
        final Set<String> keys = credentials.keys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("realm"));
        assertTrue(keys.contains("myAttribute"));
    }

    @Test
    public void containsKey() {
        assertTrue(credentials.containsKey(Credentials.Attr.REALM));
        assertFalse(credentials.containsKey(Credentials.Attr.SECRET));
    }

    @Test
    public void expires() {
        assertEquals(expiryDate, credentials.expires());
    }

    @Test
    public void isValidFor() {
        assertFalse(Credentials.NULL.isValidFor(1, ChronoUnit.SECONDS));
        assertFalse(credentials.isValidFor(1, ChronoUnit.SECONDS));
    }

    @Test
    public void isValid() {
        assertFalse(Credentials.NULL.isValid());
        assertFalse(credentials.isValid());
    }

    @Test
    public void isUserDefault() {
        assertFalse(Credentials.NULL.isUserDefault());
        assertTrue(credentials.isUserDefault());
    }

    @Test
    public void testToString() {
        assertFalse(Credentials.NULL.toString().isEmpty());
        assertFalse(credentials.toString().isEmpty());
    }

    @Test
    public void toMap() {
        final Map<String, Object> map = credentials.toMap();
        assertTrue(map.containsKey("service"));
    }

    @Test
    public void ownerType_resolve() {
        assertEquals(Credentials.OwnerType.System, Credentials.OwnerType.resolve("System"));
    }

    @Test
    public void testEquals() {
        final Credentials myCred = new Credentials("bingo", "bongo",
                Credentials.OwnerType.Organization, "foo", Instant.now(), false,
                "key1", "value1", "key2", "value2");

        assertFalse(credentials.equals(myCred));
        assertFalse(credentials.equals(Credentials.NULL));
        assertFalse(credentials.equals(this));
        assertTrue(credentials.equals(credentials));
    }

    @Test
    public void constructorWithAttributes() {
        final Credentials myCred = new Credentials("bingo", "bongo",
                Credentials.OwnerType.Organization, "foo", Instant.now(), false,
                "key1", "value1", "key2", "value2");
        assertEquals("value1", myCred.get("key1"));
        assertEquals("value2", myCred.get("key2"));
    }

    private Credentials getCredentials() {
        expiryDate = Instant.now();
        return new Credentials.Builder().
                service("bingo").
                lookupKey("bongo").
                ownerType(Credentials.OwnerType.User).
                ownerId("foo").
                expires(expiryDate).
                set(Credentials.Attr.REALM, "bar").
                set("myAttribute", "myValue").
                setUserDefault(true).build();
    }
}