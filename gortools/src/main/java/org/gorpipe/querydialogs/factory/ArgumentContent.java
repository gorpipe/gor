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

package org.gorpipe.querydialogs.factory;

import org.gorpipe.querydialogs.util.ValueFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for perspective dialog argument content.
 */
public class ArgumentContent {
    private static final String STORE_FORMAT_LIST = "list";
    private static final String STORE_FORMAT_KEYWORDS = "keyw";
    /**
     * The value.
     */
    public final String value;
    /**
     * The format.
     */
    public final Format format;

    /**
     * Constructor.
     *
     * @param value argument value
     */
    public ArgumentContent(final String value) {
        this(value, null);
    }

    /**
     * Constructor.
     *
     * @param value  argument value
     * @param format argument format
     */
    private ArgumentContent(final String value, final Format format) {
        this.value = value;
        this.format = format;
    }

    /**
     * Parse content string.
     *
     * @param content the content string to parse
     * @return new argument content
     */
    public static ArgumentContent parseContent(final String content) {
        String tmpValue = content;
        Format format = null;

        if (content.startsWith(STORE_FORMAT_LIST)) {
            format = Format.LIST;
            tmpValue = format.extractValue(tmpValue);
        } else if (content.startsWith(STORE_FORMAT_KEYWORDS)) {
            format = Format.KEYWORDS;
            tmpValue = format.extractValue(tmpValue);
        }
        return new ArgumentContent(tmpValue, format);
    }

    /**
     * Definition of argument content formats.
     */
    public enum Format {
        /**
         * List format.
         */
        LIST(ValueFormatter.VALUES_FORMAT, STORE_FORMAT_LIST),
        /**
         * Keywords format.
         */
        KEYWORDS(ValueFormatter.KEYWORDS_FORMAT, STORE_FORMAT_KEYWORDS);

        private static final Map<String, Format> valueFormat2Format;

        static {
            valueFormat2Format = new HashMap<>();
            for (Format format : values()) {
                valueFormat2Format.put(format.valueFormat, format);
            }
        }

        /**
         * Value format key.
         */
        public final String valueFormat;
        /**
         * Store format key.
         */
        public final String storeFormat;

        Format(final String valueFormat, final String storeFormat) {
            this.valueFormat = valueFormat;
            this.storeFormat = storeFormat;
        }

        /**
         * Get format by value format key.
         *
         * @param valueFormat the value format key
         * @return the format that corresponds to the value format key
         */
        public static Format getFormatByValueFormat(final String valueFormat) {
            return valueFormat2Format.get(valueFormat);
        }

        /**
         * Extract value from content string.
         *
         * @param content the content string
         * @return the value extracted form content string
         */
        public String extractValue(final String content) {
            return content.replace(storeFormat + "(", "").replace(")", "");
        }

        /**
         * Format value.
         *
         * @param valueString the value to format
         * @return the formatted value
         */
        public String formatForStore(final String valueString) {
            return storeFormat + "(" + valueString + ")";
        }
    }
}
