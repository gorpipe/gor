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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of static string utility routines
 */
public class StringUtil {

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extract a substring from the text, that starts after the prefix and ends prior to the suffix	(wich is
     * the rest of the text string if the suffix is not found).
     *
     * @param text   The text to extract substring from
     * @param prefix The prefix text
     * @param suffix The suffix text
     * @return The substring
     */
    public static String substring(String text, String prefix, String suffix) {
        if (text != null) {
            final int prefixIdx = text.indexOf(prefix);
            if (prefixIdx >= 0) {
                final int suffixIdx = text.indexOf(suffix, prefixIdx);
                return text.substring(prefixIdx + prefix.length(), suffixIdx > 0 ? suffixIdx : text.length());
            }
        }

        return null;
    }

    /**
     * @param text The chars to look through
     * @param ch   The char to find
     * @return First index of the specified character after the specified startIndex in the provided char array
     */
    public static int indexOf(char[] text, char ch) {
        return indexOf(text, ch, 0);
    }

    /**
     * @param text       The chars to look through
     * @param ch         The char to find
     * @param startIndex The first position to read from
     * @return First index of the specified character after the specified startIndex in the provided char array
     */
    public static int indexOf(char[] text, char ch, int startIndex) {
        final int len = text.length;
        for (int i = startIndex; i < len; i++) {
            if (text[i] == ch) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Prints the exception stacktrace and its backtrace to the specified print stream.
     *
     * @param exception the exception to list
     * @param s         <code>PrintWriter</code> to use for output
     */
    private static void printStackTrace(Throwable exception, PrintWriter s) {
        s.println(exception.toString());
        StackTraceElement[] trace = exception.getStackTrace();
        for (StackTraceElement element : trace) {
            s.println("\tat " + element);
        }

        Throwable ourCause = exception.getCause();
        if (ourCause != null) {
            s.print("Caused by: ");
            printStackTrace(ourCause, s);
        }
    }

    /**
     * Check if a string holds a valid integer value
     *
     * @param s the string to check
     * @return true if the string can be converted to a int
     */
    public static final boolean isValidInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException n) {
            return false;
        }
        return true;
    }

    /**
     * Check if a string contains only digits
     *
     * @param s the string to check
     * @return true if the characters in the string are all digits
     */
    public final static boolean isAllDigit(String s) {
        for (int i = 0; i < s.length(); i++)
            if (!Character.isDigit(s.charAt(i)))
                return false;

        return true;
    }

    /**
     * Convert the byte text into an integer
     *
     * @param seq   The CharSequence to parse from
     * @param begin The starting position in the array for the value, inclusive
     * @param end   The end position in the array for the value, exclusive
     * @return The integer value
     */
    private static int toInt(CharSequence seq, int begin, int end) {
        int nr = 0;
        final boolean isNegative = end > begin && seq.charAt(begin) == '-';

        for (int p = isNegative ? begin + 1 : begin; p < end; p++) {

            final char ch = seq.charAt(p);
            if (ch < '0' || ch > '9') {
                break;
            }
            nr = 10 * nr + (ch - '0');
        }

        return isNegative ? -nr : nr;
    }

    /**
     * Compares two strings, with regards to whether they include numbers.
     * When used instead of s1.compareTo(s2) when ordering strings, "String 9" will precede "String 10".
     *
     * @param s1 First String to compare
     * @param s2 Second String to compare
     * @return A number > 0 if s1 is precedes s2, less than 0 if s2 precedes s1, 0 if they are equal
     */
    public final static int compareNumStrings(String s1, String s2) {
        int lesserLength = Math.min(s1.length(), s2.length());
        int index = 0;
        while (index < lesserLength - 1 && Character.toUpperCase(s1.charAt(index)) == Character.toUpperCase(s2.charAt(index))) {
            int s1Index = index + 1;
            while (s1Index < s1.length() && Character.isDigit(s1.charAt(s1Index)) && s1Index - index < 9) {
                s1Index++;
            }
            int s2Index = index + 1;
            while (s2Index < s2.length() && Character.isDigit(s2.charAt(s2Index)) && s2Index - index < 9) {
                s2Index++;
            }
            if (s1Index - index > 1 || s2Index - index > 1) {
                final int s1Number = s1Index - index > 1 ? StringUtil.toInt(s1, index + 1, s1Index) : 0;
                final int s2Number = s2Index - index > 1 ? StringUtil.toInt(s2, index + 1, s2Index) : 0;

                if (s1Number != s2Number) {
                    return s1Number < s2Number ? -1 : 1;
                }
                index = Math.min(s1Index, s2Index);
            } else {
                index++;
            }
        }
        return s1.compareToIgnoreCase(s2);
    }
    /**
     * Query if the provided string is a valid double number
     * NOTE calling this method frequently is not optimal since it will using Double.parseDouble and catch exception is rather slow
     *
     * @param s The string to check
     * @return True if string is a valid double
     */
    public static final boolean isValidDouble(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException n) {
            return false;
        }
        return true;
    }

    /**
     * Join a collection of strings (or toString() result of objects) using a
     * separator.
     *
     * @param separator Separator
     * @param items     Collection of items.
     * @return joined string.
     */
    public static String join(String separator, Iterable<?> items) {
        if (items == null) return null;
        StringBuilder result = new StringBuilder();
        join(separator, items, result);
        return result.toString();
    }

    /**
     * Join a collection of strings (or toString() result of objects) using a
     * seperator.
     *
     * @param seperator Separator
     * @param items     Collection of items.
     * @param result    Buffer to add joined string to.
     */
    public static void join(String seperator, Iterable<?> items, Appendable result) {
        try {
            if (items == null)
                return;
            Iterator<?> iter = items.iterator();
            for (int i = 0; iter.hasNext(); i++) {
                Object item = iter.next();
                if (i > 0)
                    result.append(seperator);
                if (item == null) item = "(null)";
                result.append(item.toString());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Join an array of strings (or toString() result of objects) using a
     * separator.
     *
     * @param seperator Separator
     * @param items     Array of items.
     * @return joined string.
     */
    public static String join(String seperator, Object... items) {
        if (items == null) return null;
        return join(seperator, Arrays.asList(items));
    }

    /**
     * Join an array of strings (or toString() result of objects) using a
     * separator.
     *
     * @param seperator Separator
     * @param items     Array of items.
     * @return joined string.
     */
    public static String join(String seperator, String... items) {
        if (items == null) return null;
        return join(seperator, Arrays.asList(items));
    }

    /**
     * Split the specified line into substring based on the specified separator
     *
     * @param line The line to split
     * @param sep  The separator to use
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line, char sep) {
        return split(line, 0, sep);
    }

    /**
     * Split the specified line into substring based on the specified separator
     *
     * @param line     The line to split
     * @param startPos The starting position to split from
     * @param sep      The separator to use
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line, int startPos, char sep) {
        final ArrayList<String> list = new ArrayList<String>();
        int lastPos = startPos;
        assert line != null;
        while (true) {
            final int pos = line.indexOf(sep, lastPos);
            if (pos > -1) {
                list.add(line.substring(lastPos, pos));
                lastPos = pos + 1;
            } else {
                list.add(line.substring(lastPos));
                return list;
            }
        }
    }

    /**
     * Split the specified line into substring based on the specified separator
     *
     * @param line     The line to split
     * @param startPos The starting position to split from
     * @param endPos   The end position to include
     * @param sep      The separator to use
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line, int startPos, int endPos, char sep) {
        final ArrayList<String> list = new ArrayList<String>();
        int lastPos = startPos;
        assert line != null;
        while (lastPos < endPos) {
            final int pos = line.indexOf(sep, lastPos);
            if (pos > -1) {
                list.add(line.substring(lastPos, Math.min(pos, endPos)));
                lastPos = pos + 1;
            } else {
                list.add(line.substring(lastPos, endPos));
                break;
            }
        }
        return list;
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line The line to split
     * @return ArrayList of the substrings
     */
    public static ArrayList<String> split(String line) {
        return split(line, '\t');
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line The line to split
     * @return ArrayList of the substrings
     */
    public static String[] splitReserveQuotesToArray(String line) {
        final ArrayList<String> list = splitReserveQuotes(line, 0);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line     The line to split
     * @param startPos The starting position
     * @return ArrayList of the substrings
     */
    private static ArrayList<String> splitReserveQuotes(String line, int startPos) {
        final ArrayList<String> list = new ArrayList<String>();
        assert line != null;
        boolean inQuote = false, inDoubleQuote = false;
        final StringBuilder sb = new StringBuilder();
        for (int i = startPos; i < line.length(); i++) {
            if (inQuote && line.charAt(i) == '\'' || inDoubleQuote && line.charAt(i) == '\"') {
                inQuote = inDoubleQuote = false;
            } else {
                if (line.charAt(i) == '\'' && !inDoubleQuote) {
                    inQuote = true;
                } else if (line.charAt(i) == '\"' && !inQuote) {
                    inDoubleQuote = true;
                } else if (line.charAt(i) == ' ' && !inQuote && !inDoubleQuote) {
                    if (sb.length() > 0) {
                        list.add(sb.toString());
                        sb.setLength(0);
                    }
                    continue;
                }
            }
            sb.append(line.charAt(i));
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line The line to split
     * @return Array of the substrings
     */
    public static String[] splitToArray(String line) {
        final ArrayList<String> list = split(line);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line The line to split
     * @param sep  The separator to use
     * @return Array of the substrings
     */
    public static String[] splitToArray(String line, char sep) {
        final ArrayList<String> list = split(line, sep);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming tab char as separator
     *
     * @param line     The line to split
     * @param startPos The starting position in the line
     * @param sep      The separator to use
     * @return Array of the substrings
     */
    public static String[] splitToArray(String line, int startPos, char sep) {
        final ArrayList<String> list = split(line, startPos, sep);
        return list.toArray(new String[list.size()]);
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings and string in parenthesis as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line The line to split
     * @return ArrayList of the substrings
     */
    public static String[] splitReserveQuotesAndParenthesesToArray(String line) {
        final List<String> list = splitReserveQuotesAndParentheses(line, 0);
        return list.toArray(new String[0]);
    }

    /**
     * Split the specified line into substring assuming space is separating char, but keep quoted strings as one token.
     * i.e. "a 'a is a' b" is splitted into 3 elments, [a], [a is a], [b]
     *
     * @param line     The line to split
     * @param startPos The starting position
     * @return ArrayList of the substrings
     */
    public static List<String> splitReserveQuotesAndParentheses(String line, int startPos) {
        final ArrayList<String> list = new ArrayList<>();
        boolean inQuote = false;
        boolean inDoubleQuote = false;
        int parCount = 0;
        final StringBuilder sb = new StringBuilder();
        for (int i = startPos; i < line.length(); i++) {
            if (inQuote && line.charAt(i) == '\'' || inDoubleQuote && line.charAt(i) == '\"') {
                inQuote = inDoubleQuote = false;
            } else if (line.charAt(i) == '(') parCount++;
            else if (line.charAt(i) == ')') parCount--;
            else {
                if (line.charAt(i) == '\'' && !inDoubleQuote) {
                    inQuote = true;
                } else if (line.charAt(i) == '\"' && !inQuote) {
                    inDoubleQuote = true;
                } else if (line.charAt(i) == ' ') {
                    if (!inQuote && !inDoubleQuote && parCount == 0) {
                        if (sb.length() > 0) {
                            list.add(sb.toString());
                            sb.setLength(0);
                        }
                        continue;
                    }
                }
            }
            sb.append(line.charAt(i));
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }

    /**
     * Join an array of strings (or toString() result of objects) using a
     * seperator.
     *
     * @param seperator Separator
     * @param items     Array of items.
     * @param result    Buffer to add joined string to.
     */
    public static void join(String seperator, Object[] items, Appendable result) {
        if (items == null) return;
        join(seperator, Arrays.asList(items), result);
    }

    /**
     * Parse patterns from string using regular expressions.
     * Example:
     * StringUtil.parse("File: abc.txt  Size: 300","File:\\s+(\\w+)\\s+Size:\\s+([0-9]+)")
     * returns string array with {"abc.txt","300"}
     *
     * @param str     String to parse from
     * @param pattern Pattern describing string format
     * @return Array of Strings, one for each matched group in pattern.
     */

    public static String[] parse(String str, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(str);
        int n = 0;
        if (m.matches()) {
            n = m.groupCount();
        }
        String[] result = new String[n];
        for (int i = 0; i < n; i++) {
            result[i] = m.group(i + 1);
        }
        return result;
    }

    /**
     * Return parent part of a path by removing separator and leaf name.
     * Example: parent("a/b/c","/") returns "a/b".  parent("noslash","/") returns null.
     *
     * @param path      Path.
     * @param seperator Path separator
     * @return Parent name or null if no parent.
     */
    public static String parent(String path, String seperator) {
        if (path == null) return null;
        int lastPos = path.lastIndexOf(seperator);
        if (lastPos < 0) {
            return null;     // A root node
        }
        return path.substring(0, lastPos);
    }

    /**
     * Return parent part of a dot separated path by removing separator and leaf name.
     * Example: parent("a.b.c") returns "a.b".  parent("nodot") returns null.
     *
     * @param path Path.
     * @return Parent name or null if no parent.
     */
    static String dotParent(String path) {
        return parent(path, ".");
    }

    /**
     * Return last (leaf) part of path.
     * Example: leaf("p1.p2.p3",".") returns "p3".  leaf("noDots",".") returns "noDots".
     *
     * @param path      path.
     * @param separator Path separator
     * @return last part of path.
     */
    static String leaf(String path, String separator) {
        if (path == null) return null;
        int lastDot = path.lastIndexOf(separator);
        // Note if dot not found, lastDot will be -1 - full string is returned.
        return path.substring(lastDot + 1);
    }

    /**
     * Return last (leaf) part of dot separated path.
     * Example: leaf("p1.p2.p3") returns "p3".  dotLeaf("noDots") returns "noDots".
     *
     * @param dotPath dot separated path.
     * @return last part of path.
     */
    public static String dotLeaf(String dotPath) {
        return leaf(dotPath, ".");
    }

    /**
     * Return head of a path by removing separator and tail.
     * Example:   head("a/b/c","/") returns "a".  head("noslash","/") returns "noslash";
     *
     * @param path      Path
     * @param separator Path separator
     * @return Head or null if path is null.
     */
    private static String head(String path, String separator) {
        if (path == null) return null;
        int firstDot = path.indexOf(separator);
        if (firstDot >= 0) {
            return path.substring(0, firstDot);
        }
        return path;
    }

    /**
     * Return head of a dot separated pathl.
     * Example:   dotHead("a.b.c") returns "a".  head("nodot) returns "nodot";
     *
     * @param path Path
     * @return Head or null if path is null.
     */
    static String dotHead(String path) {
        return head(path, ".");
    }

    /**
     * Return tail of a path.
     * Example:  tail("a/b/c","/") returns "b/c".  tail("noslash","/") returns null;
     *
     * @param path      Path
     * @param separator Path separator
     * @return Tail or null if path is null or same as head.
     */
    private static String tail(String path, String separator) {
        if (path == null) return null;
        int firstDot = path.indexOf(separator);
        if (firstDot >= 0) {
            return path.substring(firstDot + 1);
        }
        return null;
    }

    /**
     * Return tail of a dot separated pathl.
     * Example:   dotTail("a.b.c") returns "b.c".  diotTail("nodot) returns null;
     *
     * @param path Path
     * @return Tail  or null if path is null or same as head.
     */
    static String dotTail(String path) {
        return tail(path, ".");
    }

    /**
     * Join two strings with dot.
     * Example:  dotJoin("a.b.c","d") returns "a.b.c.d".  dotJoin(null,"leaf") returns "leaf".
     *
     * @param parent Parent path.
     * @param child  Child name.
     * @return dot separated path.
     */
    static String dotJoin(String parent, String child) {
        if (parent == null) return child;
        if (child == null) return parent;
        return join(".", (Object[]) new String[]{parent, child});
    }

    /**
     * Find (distictly) all strings in a that are not in b
     *
     * @param a String array
     * @param b String array
     * @return An array with the distinct strings from a not in b
     */
    public static String[] diff(String[] a, String[] b) {
        HashSet<String> diff = new HashSet<String>();
        HashSet<String> bstrings = new HashSet<String>();
        Collections.addAll(bstrings, b);
        for (String s : a) {
            if (!bstrings.contains(s))
                diff.add(s);
        }

        return diff.toArray(new String[diff.size()]);
    }

    /**
     * Base64 encode binary data.  Data is encoded using printable ascii characters.
     *
     * @param data Input data
     * @return Base64 encoded string.
     */
    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decode base64 encoded data.
     *
     * @param base64 Base64 encoded string.
     * @return Original data.
     */
    public static byte[] base64Decode(String base64) {
        return Base64.getDecoder().decode(base64);
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

    /**
     * Create a sentence from a 'programmer' string.  This will take strings like "firstValue" or "BASE_QUERY" and
     * return "First Value" and "Base Query" respectively.  Word boundaries are detected on switch from lower to upper
     * case or underscore/whitespace.  All words are capitalized.
     *
     * @param name Camel case or underscored name.
     * @return Sentence
     */
    static String toSentence(String name) {
        if (name == null) return null;
        name = name.trim();
        StringBuilder result = new StringBuilder();
        boolean cap = true;
        boolean lastUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == ' ' || c == '\t' || c == '\n') {
                result.append(' ');
                cap = true;
            } else {
                boolean upper = false;
                if (c == Character.toUpperCase(c)) {
                    upper = true;
                }
                if (!lastUpper && upper) {
                    result.append(' ');
                    cap = true;
                }
                if (cap) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(Character.toLowerCase(c));
                }
                cap = false;
                lastUpper = upper;
            }
        }

        return result.toString().replaceAll("\\s+", " ");
    }

    /**
     * Create a Camel case name from a sentence.
     * This will take strings like "First value" or " To  Camel case " and
     * return "firstValue" and "toCamelCase".
     * Note that the expression toCamelCase(toSentence(name)) should return the original
     * name if it is in camel case to start with (the reverse is not neccesarily true).
     *
     * @param sentence Sentence
     * @return Camel case name.
     */
    static String toCamelCase(String sentence) {
        if (sentence == null) return null;
        String n = sentence.trim();
        StringBuilder result = new StringBuilder();
        boolean cap = false;

        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.isWhitespace(c)) {
                cap = true;
            } else {
                if (cap) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(Character.toLowerCase(c));
                }
                cap = false;
            }
        }
        return result.toString();
    }

    /**
     * Create an identifier from input string.
     * The result will only contain upper-case alphanumerics and underscore ( i.e matching [A-Z0-9_]*).
     * <p>
     * Details:
     * Trims leading/trailing spaces.  Converts other spaces/non-alphanunerical characters to underscores.
     * Underscores are also added on switch from lower to upper case.
     * Result will be all uppercase.
     * <p>
     * Example:  "My String" -> "MY_STRING"
     * "firstValue" -> "FIRST_VALUE"
     *
     * @param sentence Input
     * @return Identifier.
     */
    static String toIdentifier(String sentence) {
        sentence = sentence.trim();
        sentence = sentence.replaceAll("[^a-zA-Z0-9_]+", "_");
        StringBuilder result = new StringBuilder();

        boolean addOnUpper = false;
        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if (c == '_') {
                addOnUpper = false;
            } else if (addOnUpper && Character.toUpperCase(c) == c) {
                addOnUpper = false;
                result.append('_');
            } else if (!addOnUpper && Character.toLowerCase(c) == c) {
                addOnUpper = true;
            }
            result.append(c);
        }
        return result.toString().toUpperCase();
    }

    /**
     * Test if string starts with regex pattern.
     * If string is long, this is more efficient than adding .* to pattern.
     *
     * @param s       String to test.
     * @param pattern Regex pattern to match.
     * @return True if string starts with patterns.
     */
    static boolean startsWithPattern(String s, String pattern) {
        String fixedPattern = pattern;
        if (pattern.length() == 0 || pattern.charAt(0) != '^') {
            fixedPattern = "^" + fixedPattern;
        }
        Matcher matcher = Pattern.compile(fixedPattern).matcher(s);
        if (matcher.find()) return true;
        return false;
    }

    /**
     * If given null value return empty string, else return the given string
     *
     * @param st The string to work with (can be null)
     * @return The non null string
     */
    static String toNotNullString(String st) {
        return st == null ? "" : st;
    }

    /**
     * Truncate string to max length.
     *
     * @param in           Input string
     * @param maxLength    Maximum length
     * @param trailingDots If true, resulting string will have "..." at the end if it was truncated.
     * @return A String that is no longer than maxLength
     */
    public static String truncate(String in, int maxLength, boolean trailingDots) {
        if (in == null) return null;
        if (in.length() <= maxLength) return in;
        if (trailingDots) return in.substring(0, maxLength - 3) + "...";
        return in.substring(0, maxLength);
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
     * @param map map of objects
     * @return comma separated string of keys and values
     */
    public static String toString(Map<Object, Object> map) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (i > 0) {
                result.append(",");
            }
            result.append(entry.getKey() + "=" + entry.getValue());
            i++;
        }

        return result.toString();
    }

    /**
     * @param set collection of objects
     * @return comma separated string of values
     */
    public static <T extends Object> String toString(Collection<T> set) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Object o : set.toArray()) {
            if (i > 0) result.append(",");
            result.append(o);
            i++;
        }
        return result.toString();
    }


    /**
     * @param s
     * @return returns true if the String is an int number.
     */
    public static boolean isStringInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * @param s
     * @return returns true if the String is a long number.
     */
    public static boolean isStringLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * @param s
     * @return returns null if the String is null or empty after trimming. Otherwise returns same String.
     */
    public static String blankNull(String s) {
        if (s != null && s.trim().isEmpty()) {
            return null;
        }
        return s;
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
                encodedStringMap.put(key, new String(StringUtil.base64Encode(value)));
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
                decodedStringMap.put(key, new String(StringUtil.base64Decode(value)));
            } else {
                decodedStringMap.put(key, value);
            }
        }
        return decodedStringMap;
    }

}
