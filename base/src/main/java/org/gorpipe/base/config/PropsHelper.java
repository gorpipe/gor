package org.gorpipe.base.config;

/**
 * Helper class for system properties related operations.
 *
 */
public class PropsHelper {
    /**
     * Parse boolan system property.
     * @param name  property name
     * @param defValue default value
     * @return boolean value of the property or default value if property is not set or has invalid value.
     */
    public static boolean getBoolean(String name, boolean defValue) {
        boolean result = defValue;
        try {
            result = Boolean.parseBoolean(System.getProperty(name));
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return result;
    }
}
