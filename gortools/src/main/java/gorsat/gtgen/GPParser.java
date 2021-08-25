package gorsat.gtgen;

import org.apache.commons.math3.util.FastMath;

/**
 * Utility functions for parsing genotype probability triplets.
 *
 * @author Hjalti Thor Isleifsson
 */
public class GPParser {

    private final static double[] plLookupTable;

    static {
        plLookupTable = new double[100];
        for (int i = 0; i < plLookupTable.length; ++i) {
            plLookupTable[i] = FastMath.pow(10, -0.1 * i);
        }
    }

    private GPParser() {
    }

    public static double[] plToGp(CharSequence plTriplet, char sep) {
        final int[] gls = parseIntTriplet(plTriplet, sep);
        final double x0 = gls[0] < 100 ? plLookupTable[gls[0]] : 0.0;
        final double x1 = gls[1] < 100 ? plLookupTable[gls[1]] : 0.0;
        final double x2 = gls[2] < 100 ? plLookupTable[gls[2]] : 0.0;
        final double xsum = x0 + x1 + x2;
        final double sum_rec = xsum == 0.0 ? 1.0 : 1.0 / xsum;
        return new double[] {x0* sum_rec, x1* sum_rec, x2* sum_rec };
    }

    public static double[] glToGp(CharSequence glTriplet, char sep) {
        final double[] gls = parseDoubleTriplet(glTriplet, sep);
        final double x0 = FastMath.pow(10.0, gls[0]);
        final double x1 = FastMath.pow(10.0, gls[1]);
        final double x2 = FastMath.pow(10.0, gls[2]);
        final double sum_rec = 1.0 / (x0 + x1 + x2);
        gls[0] = x0 * sum_rec;
        gls[1] = x1 * sum_rec;
        gls[2] = x2 * sum_rec;
        return gls;
    }

    /**
     *
     * @param gl An integer representing the difference of log of probability of most likely genotype and the log
     *           of second most likely genotype.
     * @param cc The called genotype, '0', '1', '2'.
     * @return An estimated triplet.
     */
    public static double[] glCcToGp(int gl, char cc) {
        final double hp = PowerLookupTable.pow(10.0, gl);
        if (cc == '0') {
            final double hp1 = 1.0 / (hp + 1);
            return new double[] {hp * hp1, hp1, 0.0};
        } else if (cc == '1') {
            final double hp2 = 1.0 / (hp + 2.0);
            return new double[] {hp2, hp * hp2, hp2};
        } else if (cc == '2') {
            final double hp1 = 1.0 / (hp + 1.0);
            return new double[] {0.0, hp1, hp * hp1};
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * The method does not do any validations so the the functionality is undefined if the input is bad.
     *
     * @param cs A triplet of doubles, separated by sep.
     * @param sep The separator.
     * @return The triplet as double array.
     */
    public static double[] parseDoubleTriplet(CharSequence cs, char sep) {
        int begin = 0;
        int end = 0;
        while (cs.charAt(end) != sep) end++;
        final double p0 = parseDouble(cs, begin, end);
        begin = ++end;
        while (cs.charAt(end) != sep) end++;
        final double p1 = parseDouble(cs, begin, end);
        final double p2 = parseDouble(cs, end + 1, cs.length());
        return new double[] {p0, p1, p2};
    }

    /**
     * Parses a double from a subsequence of a character sequence.
     *
     * The method does not do any validations so the the functionality is undefined if the input is bad.
     *
     * @param cs The character sequence to parse from.
     * @param begin The beginning of the subsequence.
     * @param end The end of the subsequence (exclusive).
     * @return The parsed double.
     */
    public static double parseDouble(CharSequence cs, int begin, int end) {
        double x = 0;
        final boolean is_negative;
        int idx;
        if (cs.charAt(begin) == '-') {
            is_negative = true;
            idx = begin + 1;
        } else {
            is_negative = false;
            idx = begin;
        }
        while (idx < end) {
            final char c = cs.charAt(idx++);
            if (c == '.') {
                break;
            } else if (c == 'e' || c == 'E') {
                final int exponent = parseInt(cs, idx, end);
                final double scale = PowerLookupTable.pow(10.0, exponent);
                x *= scale;
                return is_negative ? -x : x;
            }
            x = 10.0 * x + (c - '0');
        }
        double ten_pow = 0.1;
        while (idx < end) {
            final char c = cs.charAt(idx++);
            if (c == 'e' || c == 'E') {
                final int exponent = parseInt(cs, idx, end);
                final double scale = PowerLookupTable.pow(10.0, exponent);
                x *= scale;
                return is_negative ? -x : x;
            } else {
                x += (c - '0') * ten_pow;
                ten_pow *= 0.1;
            }
        }
        return is_negative ? -x : x;
    }

    /**
     * The method does not do any validations so the the functionality is undefined if the input is bad.
     *
     * @param cs A triplet of integers, separated by sep.
     * @param sep The separator.
     * @return The triplet as int array.
     */
    public static int[] parseIntTriplet(CharSequence cs, char sep) {
        int begin = 0;
        int end = 0;
        while (cs.charAt(end) != sep) end++;
        final int x0 = parseInt(cs, begin, end);
        begin = ++end;
        while (cs.charAt(end) != sep) end++;
        final int x1 = parseInt(cs, begin, end);
        final int x2 = parseInt(cs, end + 1, cs.length());
        return new int[] {x0, x1, x2};
    }

    /**
     * Parses an integer from a subsequence of a character sequence.
     *
     * The method does not do any validations so the the functionality is undefined if the input is bad.
     *
     * @param cs The sequence to parse from.
     * @param begin The beginning of the subsequence.
     * @param end The end of the subsequence (exclusive).
     * @return The parsed integer.
     */
    public static int parseInt(CharSequence cs, int begin, int end) {
        final boolean isNegative;
        int idx;
        if (cs.charAt(begin) == '-') {
            isNegative = true;
            idx = begin + 1;
        } else {
            isNegative = false;
            idx = begin;
        }
        int x = 0;
        while (idx < end) {
            x = 10 * x + (cs.charAt(idx) - '0');
            idx++;
        }
        return isNegative ? -x : x;
    }
}
