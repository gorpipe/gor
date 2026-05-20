package org.gorpipe.base.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for system properties related operations.
 *
 */
public class PropsHelper {

    private static final Logger log = LoggerFactory.getLogger(PropsHelper.class);

    /**
     * Parse boolean system property.
     * @param name  property name
     * @param defaultValue default value
     * @return boolean value of the property or default value if property is not set or has invalid value.
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        String value = System.getProperty(name);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.warn("Invalid boolean for system property {}='{}'. Using default {}.", name, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Parse int system property.
     * @param name  property name
     * @param defaultValue default value
     * @return int value of the property or default value if property is not set or has invalid value.
     */
    public static int getInt(String name, int defaultValue) {
        String value = System.getProperty(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer for system property {}='{}'. Using default {}.", name, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
