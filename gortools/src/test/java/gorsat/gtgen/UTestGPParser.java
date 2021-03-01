package gorsat.gtgen;

import org.junit.Assert;
import org.junit.Test;

import static gorsat.gtgen.GPParser.*;

public class UTestGPParser {

    @Test
    public void test_parseInt() {
        final String num1 = "12345";
        Assert.assertEquals(12345, parseInt(num1, 0, num1.length()));
        final String num2 = "-12345";
        Assert.assertEquals(-12345, parseInt(num2, 0, num2.length()));
    }

    @Test
    public void test_parseDouble() {
        final double d = 1e-15;
        final String num1 = "0.0";
        Assert.assertEquals(0.0, parseDouble(num1, 0, num1.length()), d);
        final String num2 = "1.0";
        Assert.assertEquals(1.0, parseDouble(num2, 0, num2.length()), d);
        final String num3 = "-1.0";
        Assert.assertEquals(-1.0, parseDouble(num3, 0, num3.length()), d);
        final String num4 = "1.0e2";
        Assert.assertEquals(100.0, parseDouble(num4, 0, num4.length()), d);
        final String num5 = "1.0e-2";
        Assert.assertEquals(0.01, parseDouble(num5, 0, num5.length()), d);
        final String num6 = "1e0";
        Assert.assertEquals(1.0, parseDouble(num6, 0, num6.length()), d);
        final String num7 = "1e-2";
        Assert.assertEquals(0.01, parseDouble(num7, 0, num7.length()), d);
        final String num8 = "-1e-2";
        Assert.assertEquals(-0.01, parseDouble(num8, 0, num8.length()), d);
    }

    @Test
    public void test_parseDoubleTriplet() {
        final double d = 1e-15;

        final String sTriplet1 = "0.0;0.1;0.9";
        final double[] dTriplet1 = {0.0, 0.1, 0.9};
        Assert.assertArrayEquals(dTriplet1, parseDoubleTriplet(sTriplet1, ';'), d);

        final String sTriplet2 = "0.99;0.009;1e-3";
        final double[] dTriplet2 = {0.99, 0.009, 1e-3};
        Assert.assertArrayEquals(dTriplet2, parseDoubleTriplet(sTriplet2, ';'), d);
    }

    @Test
    public void test_glToGp() {
        final double[] gps = {0.020687333214812486, 0.7945196697676049, 0.18479299701758242};
        final String gls = "4.134474378537163;5.718874522187176;5.085455377782171";
        Assert.assertArrayEquals(gps, glToGp(gls, ';'), 1e-12);
        final String gls2 = "14.134474378537163;15.718874522187176;15.085455377782171";
        Assert.assertArrayEquals(gps, glToGp(gls2, ';'), 1e-12);
    }

    @Test
    public void test_glCcToPp() {
        final double[] gps1 = {10.0 / 11.0, 1.0 / 11.0, 0.0};
        Assert.assertArrayEquals(gps1, glCcToGp(1, '0'), 1e-12);

        final double[] gps2 = {1.0 / 12.0, 10.0 / 12.0, 1.0 / 12.0};
        Assert.assertArrayEquals(gps2, glCcToGp(1, '1'), 1e-12);

        final double[] gps3 = {0.0, 1.0 / 11.0, 10.0 / 11.0};
        Assert.assertArrayEquals(gps3, glCcToGp(1, '2'), 1e-12);
    }

    @Test
    public void test_glCcToPp_exception() {
        boolean success = false;
        try {
            glCcToGp(0, '3');
        } catch (IllegalArgumentException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test_plToGp() {
        final double d = 1e-12;

        final String pls1 = "0;10;20";
        final double[] gps1 = {1.0, 0.1, 0.01};
        Assert.assertArrayEquals(gps1, plToGp(pls1, ';'), d);

        final String pls2 = "0;20;40";
        final double[] gps2 = {1.0, 0.01, 0.0001};
        Assert.assertArrayEquals(gps2, plToGp(pls2, ';'), d);

        final String pls3 = "0;0;0";
        final double[] gps3 = {1.0, 1.0, 1.0};
        Assert.assertArrayEquals(gps3, plToGp(pls3, ';'), d);

        final String pls4 = "100;0;0";
        final double[] gps4 = {0, 1.0, 1.0};
        Assert.assertArrayEquals(gps4, plToGp(pls4, ';'), d);

        final String pls5 = "0;100;0";
        final double[] gps5 = {1.0, 0.0, 1.0};
        Assert.assertArrayEquals(gps5, plToGp(pls5, ';'), d);

        final String pls6 = "0;0;100";
        final double[] gps6 = {1.0, 1.0, 0.0};
        Assert.assertArrayEquals(gps6, plToGp(pls6, ';'), d);
    }

    @Test
    public void test_parseIntTriplet() {
        final String triplet1 = "1,1,1";
        final int[] ints1 = {1, 1, 1};
        Assert.assertArrayEquals(ints1, parseIntTriplet(triplet1, ','));


        final String triplet2 = "-1,-1,-1";
        final int[] ints2 = {-1, -1, -1};
        Assert.assertArrayEquals(ints2, parseIntTriplet(triplet2, ','));
    }
}
