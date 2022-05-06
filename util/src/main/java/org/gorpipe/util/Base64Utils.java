package org.gorpipe.util;

import java.util.*;

public class Base64Utils {

    /**
     * Decode s encoded data.
     *
     * @param s Base64 encoded string.
     * @return Original data.
     */
    public static byte[] base64Decode(String s) {
        return Base64.getDecoder().decode(s);
    }

    /**
     * Method to base 64 encode a string
     *
     * @param s
     * @return
     */
    public static byte[] base64Encode(String s) {
        return Base64.getEncoder().encode(s.getBytes());
    }

    /**
     * Method to take a key value pair string map and encode the values but leave the keys as is.
     * This values for keys as given as parameters are encoded
     *
     * @param stringMap
     * @return
     */
    public static Map<String, String> base64Encode(Map<String, String> stringMap, Set<String> keys) {
        Map<String, String> encodedStringMap = new HashMap<>(stringMap);
        Iterator it = stringMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String key = (String) pair.getKey();
            String value = (String) pair.getValue();
            if (keys.contains(key) && value != null) {
                // Add to map since this field should be encoded and is not null
                encodedStringMap.put(key, new String(base64Encode(value)));
            } else {
                encodedStringMap.put(key, value);
            }
        }
        return encodedStringMap;
    }

    /**
     * Method to take a key value pair string map and decode the values but leave the keys as is.
     * This values for keys as given as parameters are decoded
     *
     * @param stringMap
     * @return
     */
    public static Map<String, String> base64Decode(Map<String, String> stringMap, Set<String> keys) {
        Map<String, String> decodedStringMap = new HashMap<>(stringMap);
        Iterator it = stringMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String key = (String) pair.getKey();
            String value = (String) pair.getValue();
            if (keys.contains(key) && value != null && isBase64(value)) {
                // Add to map since this field should be decoded is not null and the value is encoded
                decodedStringMap.put(key, new String(base64Decode(value)));
            } else {
                decodedStringMap.put(key, value);
            }
        }
        return decodedStringMap;
    }
    /**
     * Check if the specified string is a valid base64 string
     *
     * @param s string to check
     * @return true if the string is a valid base64 string, else false
     */
    public static boolean isBase64(String s) {
        if (s.length() % 4 != 0) {
            return false;
        }
        String regEx = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        return s.matches(regEx);
    }
}