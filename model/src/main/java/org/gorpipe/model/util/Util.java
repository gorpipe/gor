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

package org.gorpipe.model.util;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.util.collection.extract.Extract;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Utility methods to use in gor
 */
public class Util {

    /**
     * The Utf8 charset
     */
    public static final Charset utf8Charset = Charset.forName("UTF-8");

    private Util() {
        // Prevent creation of instances of this class - all methods are static
    }

    /**
     * @return True if we are running on the windows OS
     */
    public static boolean isWindowsOS() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * @return True if we are running on the mac OS
     */
    public static boolean isOSX() {
        return System.getProperty("os.name").toLowerCase().contains("os x");
    }

    /**
     * @param value       The value to extract if not null
     * @param valueIfNull The value to extract if value was null
     * @return The value extracted
     */
    public static <T> T nvl(T value, T valueIfNull) {
        return value == null ? valueIfNull : value;
    }

    /**
     * @param value       The value to extract toString from if not null
     * @param valueIfNull The value to extract if value was null or value.toString is null
     * @return The value extracted
     */
    public static String nvlToString(Object value, String valueIfNull) {
        return value == null ? valueIfNull : nvl(value.toString(), valueIfNull);
    }

    /**
     * Query for the MD5 digest for the specified data in the provided byte array
     *
     * @param bytes  The bytes with the data to find digest of
     * @param offset The offset into the byte array to start
     * @param length The number of bytes to include
     * @return The digest bytes
     */
    static byte[] md5Bytes(byte[] bytes, int offset, int length) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes, offset, length);
            return md5.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new GorSystemException("Did not find implementation of MD5 hashing", ex);
        }
    }

    /**
     * Query for the MD5 digest for the specified byte array
     *
     * @param bytes The bytes to find digest of
     * @return The digest bytes
     */
    static byte[] md5Bytes(byte[] bytes) {
        return md5Bytes(bytes, 0, bytes.length);
    }

    /**
     * Query for the MD5 digest for the specified byte array
     *
     * @param s The text string to digest
     * @return The digest as HEX string
     */
    public static byte[] md5Bytes(String s) {
        return md5Bytes(s.getBytes(utf8Charset));
    }

    /**
     * Query for the MD5 digest for the specified String
     *
     * @param s The text string to digest
     * @return The digest as HEX string
     */
    public static String md5(String s) {
        return md5(s.getBytes(utf8Charset));
    }

    /**
     * Query for the MD5 digest for the specified byte array
     *
     * @param bytes The bytes
     * @return The digest as HEX string
     */
    public static String md5(byte[] bytes) {
        return Extract.hex(md5Bytes(bytes));
    }

    /**
     * @param o Object
     * @return true if toString for the object is empty
     */
    public static boolean isEmpty(Object o) {
        if (o == null || o.toString() == null) {
            return true;
        }
        return o.toString().trim().length() == 0;
    }

    /**
     * Read inputstream as String (use only for 'small' streams).
     *
     * @param stream Input stream
     * @return String data
     * @throws IOException on error
     */
    public static String readStream(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        byte[] buf = new byte[10000];
        int read;
        while ((read = stream.read(buf)) > 0) {
            result.append(new String(buf, 0, read));
        }
        return result.toString();
    }

    /**
     * Read and close inputstream as String (use only for 'small' streams).
     *
     * @param stream Input stream
     * @return String data
     * @throws IOException on error
     */
    public static String readAndCloseStream(InputStream stream) throws IOException {
        try {
            return readStream(stream);
        } finally {
            stream.close();
        }
    }

    /**
     * @param fileName The name of file
     * @return True if the specified fileName is an absolute reference or false if it is a relative reference
     */
    public static boolean isAbsoluteFilePath(String fileName) {
        return (fileName != null && fileName.length() > 2 && (fileName.charAt(0) == '/' || fileName.charAt(0) == '\\' || fileName.charAt(1) == ':'));
    }

    /**
     * Convert exception from database specific exception into common SQLException. Allows throwing exception from server
     * to client that doesn't have those classes.
     *
     * @param th The exception to convert
     * @return The converted exception
     */
    public static Throwable convert(Throwable th) {
        if (th instanceof SQLException) {
            // Do not throw the vendor specific subclass since there is no guarantee that a remote client has its class
            SQLException s = (SQLException) th;
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement e : th.getStackTrace()) {
                sb.append('\t');
                sb.append("at ");
                sb.append(e);
                sb.append('\n');
            }
            final String msg = th.getMessage() + sb.toString();
            SQLException se = new SQLException(msg, s.getSQLState(), s.getErrorCode());
            se.setStackTrace(new StackTraceElement[0]); // Remove stack trace since we do not want this convert method to show up
            return se;
        }
        return th;
    }
}
