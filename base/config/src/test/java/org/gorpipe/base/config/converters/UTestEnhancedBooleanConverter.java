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

package org.gorpipe.base.config.converters;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.gorpipe.base.config.converters.EnhancedBooleanConverter;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UTestEnhancedBooleanConverter {
    private interface EnhancedBooleanConfig extends Config {
        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("yes")
        Boolean yesBooleanClass();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("no")
        Boolean noBooleanClass();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("true")
        Boolean trueBooleanClass();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("false")
        Boolean falseBooleanClass();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("enabled")
        Boolean enabledBooleanClass();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("disabled")
        Boolean disabledBooleanClass();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("true")
        boolean trueBooleanPrimitive();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("false")
        boolean falseBooleanPrimitive();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("42")
        boolean integerValue();

        @ConverterClass(EnhancedBooleanConverter.class)
        @DefaultValue("this is a random string")
        boolean randomStringValue();
    }

    @Test
    public void testEnhancedBooleanConverter() {
        EnhancedBooleanConfig cfg = ConfigFactory.create(EnhancedBooleanConfig.class);

        assertTrue(cfg.yesBooleanClass());
        assertFalse(cfg.noBooleanClass());

        assertTrue(cfg.trueBooleanClass());
        assertFalse(cfg.falseBooleanClass());

        assertTrue(cfg.enabledBooleanClass());
        assertFalse(cfg.disabledBooleanClass());

        assertTrue(cfg.trueBooleanPrimitive());
        assertFalse(cfg.falseBooleanPrimitive());

        assertFalse("Integer values should be evaluated to false.", cfg.integerValue());
        assertFalse("Random string values should be evaluated to false.", cfg.randomStringValue());
    }
}

