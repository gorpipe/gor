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

package org.gorpipe.util.string;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by villi on 11/03/16.
 */
public class JsonUtil {

    /**
     * Parse json object.
     *
     * @param json Json data
     * @return Map representing json object.
     * @throws IllegalArgumentException if string is not in json format.
     */
    public static Map<String, Object> parseJson(CharSequence json) throws IllegalArgumentException {
        try {
            return new ObjectMapper().readValue(json.toString(), HashMap.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot parse json string: " + json, e);
        }
    }

    /**
     * Parse simple json object - i.e. an object containing only string values.
     *
     * @param json Json data
     * @return Map representing json object.
     * @throws IllegalArgumentException if string is not in json format or cannot be parsed as a simple json object
     */
    public static Map<String, String> parseSimpleJson(CharSequence json) throws IllegalArgumentException {
        Map<String, Object> parsed = parseJson(json);
        Map<String, String> result = new HashMap<>();
        for (String k : parsed.keySet()) {
            Object val = parsed.get(k);
            if (val == null) {
                result.put(k, null);
            } else {
                result.put(k, val.toString());
            }
        }
        return result;
    }

    /**
     * Method to parse a String which is assumed to contain an array of items.
     *
     * @param string
     * @return
     * @throws IOException
     */
    public static Object[] parseJsonArrayValue(String string) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(string, Object[].class);
    }

    /**
     * Method to parse a String which is assumed to contain an array of items.
     *
     * @param string
     * @return
     * @throws IOException
     */
    public static LinkedList parseJsonArrayValueList(String string) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(string, LinkedList.class);
    }

    /**
     * Convert map object into json string
     */
    public static String toJson(Map<String, ?> data) {
        Writer writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(writer, data);
        } catch (IOException e) {
            // Should not happen
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    public static Instant parseIso8601Timestamp(CharSequence stamp) {
        if (stamp != null) {
            return Instant.from(ZonedDateTime.parse(stamp));
        }
        return null;
    }

    /**
     * Map an array of objects into a JSON string containing an array
     *
     * @param objects
     * @return
     * @throws JsonProcessingException
     */
    public static String writeValueAsArrayString(Object[] objects) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(objects);
    }

    /**
     * Map a map into a JSON string containing an array
     *
     * @param map
     * @return
     * @throws JsonProcessingException
     */
    public static String writeValueAsArrayString(Map map) throws JsonProcessingException {
        return writeValueAsArrayString(new Object[]{map});
    }

    /**
     * Map an object into a JSON string which does not string as an array.
     *
     * @param object
     * @return
     * @throws JsonProcessingException
     */
    public static String writeValueAsString(Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

}
