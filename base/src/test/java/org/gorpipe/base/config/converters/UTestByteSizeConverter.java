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

package org.gorpipe.base.config.converters;

import org.gorpipe.base.config.bytesize.ByteSize;
import org.gorpipe.base.config.bytesize.ByteSizeUnit;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.gorpipe.base.config.converters.ByteSizeConverter;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class UTestByteSizeConverter {
    public interface ByteSizeConfig extends Config {
        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10 byte")
        ByteSize singular10byteWithSpace();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10byte")
        ByteSize singular10byteWithoutSpace();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10 bytes")
        ByteSize plural10byte();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10m")
        ByteSize short10mebibytes();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10mi")
        ByteSize medium10mebibytes();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10mib")
        ByteSize long10mebibytes();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10 megabytes")
        ByteSize full10megabytes();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("10 sillybyte")
        ByteSize invalidSillybyte();

        @ConverterClass(ByteSizeConverter.class)
        @DefaultValue("megabyte")
        ByteSize invalidNoNumber();
    }

    @Test
    public void testValidByteSizeConverter() {
        ByteSizeConfig cfg = ConfigFactory.create(ByteSizeConfig.class);
        ByteSize bs;

        bs = cfg.plural10byte();
        assertEquals(bs, new ByteSize(10, ByteSizeUnit.BYTES));

        bs = cfg.singular10byteWithoutSpace();
        assertEquals(bs, new ByteSize(10, ByteSizeUnit.BYTES));

        bs = cfg.singular10byteWithSpace();
        assertEquals(bs, new ByteSize(10, ByteSizeUnit.BYTES));

        ByteSize compare = new ByteSize(10, ByteSizeUnit.MEBIBYTES);
        assertEquals(compare, cfg.short10mebibytes());
        assertEquals(compare, cfg.medium10mebibytes());
        assertEquals(compare, cfg.long10mebibytes());
        assertNotEquals(compare, cfg.full10megabytes());
        assertEquals(new ByteSize(10, ByteSizeUnit.MEGABYTES), cfg.full10megabytes());
    }

    @Test
    public void testInvalid() throws NoSuchMethodException, IllegalAccessException {
        ByteSizeConfig cfg = ConfigFactory.create(ByteSizeConfig.class);
        ByteSize bs;
        for (String method : new String[]{"invalidSillybyte", "invalidNoNumber"}) {
            Method m = ByteSizeConfig.class.getDeclaredMethod(method);
            try {
                bs = (ByteSize) m.invoke(cfg);
                fail(String.format("Invalid byte size [%s] should have thrown an exception. Instead we parsed: %s", method, bs));
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalArgumentException)) {
                    fail("Got an unexpected exception type when calling method: " + method);
                }
            }
        }
    }

}

