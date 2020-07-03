package gorsat.gtgen;

import org.junit.*;

public class UTestGTGen {

    @Test
    public void test_impute_01_01() {
        final int n = 20;
        final double tol = 1e-5;
        final int[] depths = {15, 2, 10, 17, 19, 1, 19, 0, 8, 19, 16, 5, 15, 2, 10, 18, 15, 9, 17, 9};
        final int[] calls = {0, 0, 0, 3, 1, 1, 1, 0, 4, 2, 1, 0, 1, 0, 0, 0, 1, 0, 9, 3};
        final double[] wantedGTS = {0.99997, 2.6850e-05, 1.0587e-20, 0.94705, 0.052950, 2.5488e-08, 0.99949, 0.00050710, 6.2488e-16, 0.99399, 0.0060050, 6.9049e-17, 0.99998, 2.3019e-05, 1.3071e-22, 0.52472, 0.47527, 1.0295e-05, 0.99998, 2.3019e-05, 1.3071e-22, 0.84663, 0.15337, 1.8456e-06, 0.084852, 0.91515, 1.8497e-07, 0.99979, 0.00020714, 1.0586e-20, 0.99987, 0.00013423, 9.5277e-20, 0.99050, 0.0094958, 3.6567e-11, 0.99976, 0.00024159, 8.5740e-19, 0.94705, 0.052950, 2.5488e-08, 0.99949, 0.00050710, 6.2488e-16, 1.0000, 4.6039e-06, 1.4524e-23, 0.99976, 0.00024159, 8.5740e-19, 0.99909, 0.00091241, 5.6216e-15, 0.00031137, 0.99969, 6.1089e-09, 0.60033, 0.39967, 1.7952e-09};
        final double wantedAF = 0.07668472935670928;
        final GTGen gtGen = new GTGen(0.1, n);
        gtGen.setAF(0.1);
        for (int i = 0; i < n; ++i) {
            gtGen.addData(i, calls[i], depths[i]);
        }
        final double[] gts = new double[3 * n];
        final boolean converged = gtGen.impute(gts, 1e-5, 10);
        final double af = gtGen.getAF();

        Assert.assertTrue(converged);
        Assert.assertEquals(wantedAF, af, tol);
        Assert.assertArrayEquals(wantedGTS, gts, tol);
    }

    @Test
    public void test_impute_1_0() {
        final int n = 20;
        final double tol = 1e-5;
        final int[] depths = {6, 9, 0, 19, 12, 2, 1, 3, 1, 2, 17, 7, 9, 17, 11, 1, 7, 12, 9, 9};
        final int[] calls = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] wantedGTS = {0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0};
        final double wantedAF = 1.0;
        final GTGen gtGen = new GTGen(1.0, n);
        gtGen.setAF(0.1);
        for (int i = 0; i < n; ++i) {
            gtGen.addData(i, calls[i], depths[i]);
        }
        final double[] gts = new double[3 * n];
        final boolean converged = gtGen.impute(gts, 0.5 * 1e-5, 10);
        final double af = gtGen.getAF();

        Assert.assertTrue(converged);
        Assert.assertEquals(wantedAF, af, tol);
        Assert.assertArrayEquals(wantedGTS, gts, tol);
    }

    @Test
    public void test_impute_0_1() {
        final int n = 20;
        final double tol = 1e-5;
        final int[] depths = {6, 13, 9, 7, 3, 7, 11, 7, 1, 0, 8, 19, 15, 9, 0, 13, 19, 11, 13, 6};
        final int[] calls = {6, 13, 9, 7, 3, 7, 11, 7, 1, 0, 8, 19, 15, 9, 0, 13, 19, 11, 13, 6};
        final double[] wantedGTS = {0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0};
        final double wantedAF = 1.0;
        final GTGen gtGen = new GTGen(0.0, n);
        gtGen.setAF(0.1);
        for (int i = 0; i < n; ++i) {
            gtGen.addData(i, calls[i], depths[i]);
        }
        final double[] gts = new double[3 * n];
        final boolean converged = gtGen.impute(gts, 0.5 * 1e-5, 10);
        final double af = gtGen.getAF();

        Assert.assertTrue(converged);
        Assert.assertEquals(wantedAF, af, tol);
        Assert.assertArrayEquals(wantedGTS, gts, tol);
    }

    @Test
    public void test_impute_0_0() {
        final int n = 20;
        final double tol = 1e-5;
        final int[] depths = {19, 7, 12, 5, 18, 3, 17, 5, 7, 1, 19, 12, 13, 4, 4, 2, 0, 13, 12, 5};
        final int[] calls =  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] wantedGTS = {1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
        final double wantedAF = 0.0;
        final GTGen gtGen = new GTGen(0.0, n);
        gtGen.setAF(0.1);
        for (int i = 0; i < n; ++i) {
            gtGen.addData(i, calls[i], depths[i]);
        }
        final double[] gts = new double[3 * n];
        final boolean converged = gtGen.impute(gts, 0.5 * 1e-5, 10);
        final double af = gtGen.getAF();

        Assert.assertTrue(converged);
        Assert.assertEquals(wantedAF, af, tol);
        Assert.assertArrayEquals(wantedGTS, gts, tol);
    }

    @Test
    public void test_noData() {
        final GTGen gtGen = new GTGen(0.0, 1);
        gtGen.setAF(0.05);
        final double[] gts = {1.0, 1.0, 1.0}; //Want to test that the array is actually cleaned.

        Assert.assertTrue(gtGen.impute(gts, 0.0, 1));
        Assert.assertEquals(0.0, gts[0], 0.0);
        Assert.assertEquals(0.0, gts[1], 0.0);
        Assert.assertEquals(0.0, gts[2], 0.0);
    }

    @Test
    public void test_notAllData() {
        final GTGen gtGen = new GTGen(0.0, 2);
        gtGen.setAF(0.05);
        gtGen.addData(0, 1.0, 0.0, 0.0);
        final double[] gts = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0}; //Want to test that the array is actually cleaned.

        Assert.assertTrue(gtGen.impute(gts, 0.0, 10));
        Assert.assertEquals(1.0, gts[0], 0.0);
        Assert.assertEquals(0.0, gts[1], 0.0);
        Assert.assertEquals(0.0, gts[2], 0.0);
        Assert.assertEquals(0.0, gts[3], 0.0);
        Assert.assertEquals(0.0, gts[4], 0.0);
        Assert.assertEquals(0.0, gts[5], 0.0);
    }

    @Test
    public void test_prior() {
        final int n = 20;
        final double tol = 1e-5;
        final double e = 0.1;
        final GTGen gtGen = new GTGen(e, n);
        final double[] gts = new double[3 * n];
        final int[] depths = {15, 2, 10, 17, 19, 1, 19, 0, 8, 19, 16, 5, 15, 2, 10, 18, 15, 9, 17, 9};
        final int[] calls = {0, 0, 0, 3, 1, 1, 1, 0, 4, 2, 1, 0, 1, 0, 0, 0, 1, 0, 9, 3};
        gtGen.setAF(0.05);
        gtGen.setPrior(0.99, 1_000_000);
        Assert.assertEquals(0.99, gtGen.getPriorAf(), 0.0);
        Assert.assertEquals(1_000_000, gtGen.getPriorAn());
        for (int i = 0; i < n; ++i) {
            gtGen.addData(i, calls[i], depths[i]);
        }
        final boolean converged = gtGen.impute(gts, tol, 10);
        final double af = gtGen.getAF();

        Assert.assertTrue(converged);
        Assert.assertEquals(0.98999, af, tol);
    }

    @Test
    public void test_prior_2() {
        final int n = 20;
        final double tol = 1e-5;
        final double e = 0.1;
        final GTGen gtGen = new GTGen(e, n);
        final double[] gts = new double[3 * n];
        final int[] depths = {15, 2, 10, 17, 19, 1, 19, 0, 8, 19, 16, 5, 15, 2, 10, 18, 15, 9, 17, 9};
        final int[] calls = {0, 0, 0, 3, 1, 1, 1, 0, 4, 2, 1, 0, 1, 0, 0, 0, 1, 0, 9, 3};
        gtGen.setAF(0.05);
        gtGen.setPrior(0.99, 10);
        Assert.assertEquals(0.99, gtGen.getPriorAf(), 0.0);
        Assert.assertEquals(10, gtGen.getPriorAn());
        for (int i = 0; i < n; ++i) {
            gtGen.addData(i, calls[i], depths[i]);
        }
        final boolean converged = gtGen.impute(gts, tol, 10);
        final double af = gtGen.getAF();

        Assert.assertTrue(converged);
        Assert.assertEquals(0.41290, af, tol);
    }

    @Test
    public void test_hasCoverage() {
        final GTGen gtGen = new GTGen(0.05, 2);
        gtGen.setAF(0.05);
        gtGen.addData(0, 0.1, 0.8, 0.1);
        Assert.assertTrue(gtGen.hasCoverage(0));
        Assert.assertFalse(gtGen.hasCoverage(1));
        gtGen.reset();
        Assert.assertFalse(gtGen.hasCoverage(0));
        Assert.assertFalse(gtGen.hasCoverage(1));
    }

    @Test
    public void test_setTwiceException() {
        final GTGen gtGen = new GTGen(0.0, 1);
        gtGen.setAF(0.05);
        gtGen.addData(0, 0.25, 0.5, 0.25);
        boolean success = false;
        try {
            gtGen.addData(0, 1, 2);
        } catch (IllegalStateException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test_setTwiceException_2() {
        final GTGen gtGen = new GTGen(0.0, 1);
        gtGen.setAF(0.05);
        gtGen.addData(0, 1, 2);
        boolean success = false;
        try {
            gtGen.addData(0, 0.25, 0.5, 0.25);
        } catch (IllegalStateException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test_getNumberOfSamples() {
        final GTGen gtGen = new GTGen(0.0, 1);
        Assert.assertEquals(1, gtGen.getNumberOfSamples());
    }

    @Test
    public void test_getAn() {
        final GTGen gtGen = new GTGen(0.0, 10);
        Assert.assertEquals(0, gtGen.getAn());

        gtGen.addData(0, 0, 1);
        Assert.assertEquals(1, gtGen.getAn());


        gtGen.addData(1, 0, 1);
        Assert.assertEquals(2, gtGen.getAn());

        gtGen.reset();
        Assert.assertEquals(0, gtGen.getAn());

        gtGen.addData(0, 1, 1);
        gtGen.setPrior(0.01, 1000);
        Assert.assertEquals(1001, gtGen.getAn());
    }
}
