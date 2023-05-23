package org.gorpipe.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class JsonUtils {

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