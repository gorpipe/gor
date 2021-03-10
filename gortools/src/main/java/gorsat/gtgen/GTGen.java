package gorsat.gtgen;

import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * A class for running simple joint variant calling.
 *
 * @author Hjalti Thor Isleifsson
 */
public class GTGen {
    private final int n;
    private int m;
    private double priorAF;
    private boolean hasPrior;
    private final double[] qs;
    private final boolean[] hasData;
    private double pAA = 0.25;
    private double pAB = 0.5;
    private double pBB = 0.25;
    private int covCount;
    private final PowerLookupTable ePlt;
    private final PowerLookupTable oneMinusEPlt;
    private final PowerLookupTable halfPlt;

    /**
     * @param e The estimated error frequency.
     * @param n The number of samples.
     */
    public GTGen(double e, int n) {
        this.n = n;
        this.qs = new double[3 * n];
        this.hasData = new boolean[n];
        this.ePlt = PowerLookupTable.getLookupTable(e);
        this.oneMinusEPlt = PowerLookupTable.getLookupTable(1 - e);
        this.halfPlt = PowerLookupTable.getLookupTable(0.5);
    }

    /**
     * Resets the object. Can be used as new.
     */
    public void reset() {
        Arrays.fill(this.hasData, false);
        this.hasPrior = false;
        this.covCount = 0;
    }

    public int getNumberOfSamples() {
        return this.n;
    }

    public double getPriorAf() {
        return this.priorAF;
    }

    public int getPriorAn() {
        return this.m;
    }

    /**
     * Sets the allele frequency which we are assuming to have been used when computing the given triplets.
     */
    public void setAF(double af) {
        this.pAA = (1 - af) * (1 - af);
        this.pAB = 2 * af * (1 - af);
        this.pBB = af * af;
    }

    public double getAF() {
        return 0.5 * this.pAB + this.pBB;
    }

    public double get_pAB()  {
        return this.pAB;
    }

    public double get_pBB()  {
        return this.pBB;
    }
    /**
     * @return Returns whether there is some data for sample with index {@code idx}.
     */
    public boolean hasCoverage(int idx) {
        return this.hasData[idx];
    }

    /**
     * @return Returns the number of samples which have coverage.
     */
    public int getAn() {
        return this.hasPrior ? this.m + this.covCount : this.covCount;
    }

    /**
     * Indicates that our {@code n} samples are a part of a bigger cohort of {@code m + n} samples, and for the
     * {@code m} ones, excluding ours, the estimated allele frequency is {@code af}. The resulting allele frequency
     * will be the same as if we had called them all together at first.
     */
    public void setPrior(double pab, double pbb, int m) {
        this.pAB = pab;
        this.pBB = pbb;
        this.pAA = 1.0-pab-pbb;
        this.m = m;
        this.hasPrior = true;
        this.priorAF = this.getAF();
    }

    public void setPrior(double af, int m) {
        this.pBB = af*af;
        this.pAA = (1-af)*(1-af);
        this.pAB = 1-this.pAA-this.pBB;
        this.m = m;
        this.hasPrior = true;
        this.priorAF = this.getAF();
    }

    public void addData(int sampleIdx, double pAA, double pAB, double pBB) {
        if (this.hasData[sampleIdx]) {
            throw new IllegalStateException("The data for sample " + sampleIdx + " has already been set.");
        }
        this.hasData[sampleIdx] = true;
        this.covCount++;
        this.qs[3 * sampleIdx] = pAA / this.pAA;
        this.qs[3 * sampleIdx + 1] = pAB / this.pAB;
        this.qs[3 * sampleIdx + 2] = pBB / this.pBB;
    }

    public void addData(int sampleIdx, int call, int depth) {
        if (this.hasData[sampleIdx]) {
            throw new IllegalStateException("The data for sample " + sampleIdx + " has already been set.");
        }
        this.hasData[sampleIdx] = true;
        this.covCount++;
        this.qs[3 * sampleIdx] = this.ePlt.pow(call) * this.oneMinusEPlt.pow(depth - call);
        this.qs[3 * sampleIdx + 1] = this.halfPlt.pow(depth);
        this.qs[3 * sampleIdx + 2] = this.ePlt.pow(depth - call) * this.oneMinusEPlt.pow(call);
    }

    /**
     * Estimates the allele frequency of the cohort and then computes the genotype probabilities.
     *
     * @param gts An array of size {@code 3*n}.
     * @param tol The tolerance in the allele frequency estimate.
     * @param maxIt The maximum number of iterations.
     * @return Whether we managed to estimate the allele frequency up to the desired accuracy.
     */
    public boolean impute(double[] gts, double tol, int maxIt) {
        if (this.covCount > 0) {
            boolean converged = computeAf(tol, maxIt);
            fillGenotypes(gts);
            return converged;
        } else {
            Arrays.fill(gts, 0.0);
            return true;
        }
    }

    private void fillGenotypes(double[] gts) {
        for (int i = 0; i < this.n; ++i) {
            if (this.hasData[i]) {
                final double x0 = this.qs[3 * i] * this.pAA;
                final double x1 = this.qs[3 * i + 1] * this.pAB;
                final double x2 = this.qs[3 * i + 2] * this.pBB;
                final double sum_rec = 1.0 / (x0 + x1 + x2);
                gts[3 * i] = x0 * sum_rec;
                gts[3 * i + 1] = x1 * sum_rec;
                gts[3 * i + 2] = x2 * sum_rec;
            } else {
                gts[3 * i] = 0.0;
                gts[3 * i + 1] = 0.0;
                gts[3 * i + 2] = 0.0;
            }
        }
    }

    private boolean computeAf(double tol, int maxIt) {
        if (this.hasPrior) {
            return computeAfWithPrior(tol, maxIt);
        } else {
            return computeAfWithoutPrior(tol, maxIt);
        }
    }

    private boolean computeAfWithPrior(double tol, int maxIt) {
        // this.setAF(this.priorAF);
        final double p0AA = this.pAA;
        final double p0AB = this.pAB;
        final double p0BB = this.pBB;
        double error = Double.POSITIVE_INFINITY;
        double corr_factor = 0.01/(1+this.m + this.covCount);  /* we cannot estimate a smaller prob with full power */
        int it = 0;
        while (error > tol && error > corr_factor && it++ < maxIt) {
            double sum_pAA = this.m * p0AA;
            double sum_pAB = this.m * p0AB;
            double sum_pBB = this.m * p0BB;
            for (int i = 0; i < this.n; ++i) {
                if (this.hasData[i]) {
                    final double x0 = this.qs[3 * i] * this.pAA;
                    final double x1 = this.qs[3 * i + 1] * this.pAB;
                    final double x2 = this.qs[3 * i + 2] * this.pBB;
                    final double sum_rec = 1.0 / (x0 + x1 + x2);
                    sum_pAA += x0 * sum_rec;
                    sum_pAB += x1 * sum_rec;
                    sum_pBB += x2 * sum_rec;
                }
            }

            final double new_pAA_temp = corr_factor + sum_pAA / (this.m + this.covCount);
            final double new_pAB_temp = corr_factor + sum_pAB / (this.m + this.covCount);
            final double new_pBB_temp = corr_factor + sum_pBB / (this.m + this.covCount);

            final double tot_prob = new_pAA_temp+new_pAB_temp+new_pBB_temp;
            final double new_pAA = new_pAA_temp/tot_prob;
            final double new_pAB = new_pAB_temp/tot_prob;
            final double new_pBB = new_pBB_temp/tot_prob;

            error = max(abs(new_pAA - this.pAA), max(abs(new_pAB - this.pAB), abs(new_pBB - this.pBB)));
            this.pAA = new_pAA;
            this.pAB = new_pAB;
            this.pBB = new_pBB;
        }
        return error <= tol || error < corr_factor;
    }

    private boolean computeAfWithoutPrior(double tol, int maxIt) {
        double error = Double.POSITIVE_INFINITY;
        double corr_factor = 0.01/(1+this.covCount); /* we cannot estimate a smaller prob with full power */
        int it = 0;
        while (error > tol && error > corr_factor && it++ < maxIt) {
            double sum_pAA = 0;
            double sum_pAB = 0;
            double sum_pBB = 0;
            for (int i = 0; i < this.n; ++i) {
                if (this.hasData[i]) {
                    final double x0 = this.qs[3 * i] * this.pAA;
                    final double x1 = this.qs[3 * i + 1] * this.pAB;
                    final double x2 = this.qs[3 * i + 2] * this.pBB;
                    final double sum_rec = 1.0 / (x0 + x1 + x2);
                    sum_pAA += x0 * sum_rec;
                    sum_pAB += x1 * sum_rec;
                    sum_pBB += x2 * sum_rec;
                }
            }
            final double new_pAA_temp = corr_factor + sum_pAA / this.covCount;
            final double new_pAB_temp = corr_factor + sum_pAB / this.covCount;
            final double new_pBB_temp = corr_factor + sum_pBB / this.covCount;

            final double tot_prob = new_pAA_temp+new_pAB_temp+new_pBB_temp;
            final double new_pAA = new_pAA_temp/tot_prob;
            final double new_pAB = new_pAB_temp/tot_prob;
            final double new_pBB = new_pBB_temp/tot_prob;

            error = max(abs(new_pAA - this.pAA), max(abs(new_pAB - this.pAB), abs(new_pBB - this.pBB)));
            this.pAA = new_pAA;
            this.pAB = new_pAB;
            this.pBB = new_pBB;
        }
        return error <= tol || error < corr_factor;
    }
}
