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

import org.gorpipe.base.config.bytesize.ByteSize;
import org.gorpipe.base.config.bytesize.ByteSizeUnit;
import org.aeonbits.owner.Converter;

import java.lang.reflect.Method;
import java.math.BigDecimal;

public class ByteSizeConverter implements Converter<ByteSize> {

    @Override
    public ByteSize convert(Method method, String input) {
        return parse(input);
    }

    public static ByteSize parse(String input) {
        String[] parts = ConverterUtil.splitNumericAndChar(input);
        String value = parts[0];
        String unit = parts[1];

        BigDecimal bdValue = new BigDecimal(value);
        ByteSizeUnit bsuUnit = ByteSizeUnit.parse(unit);

        if (bsuUnit == null) {
            throw new IllegalArgumentException("Invalid unit string: '" + unit + "'");
        }

        return new ByteSize(bdValue, bsuUnit);
    }
}
