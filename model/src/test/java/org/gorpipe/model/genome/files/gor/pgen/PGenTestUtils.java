package org.gorpipe.model.genome.files.gor.pgen;

import java.util.Random;

class PGenTestUtils {

    static byte[] getRandomHardCalls(int number) {
        final Random r = new Random();
        final byte[] hc = new byte[number];
        for (int i = 0; i < number; ++i) {
            hc[i] = (byte) r.nextInt(4);
        }
        return hc;
    }

    static double[][] getRandomImputedGenotypes(int numberOfSamples) {
        final Random r = new Random();
        final double[][] probs = new double[numberOfSamples][3];
        double p0, p1, p2, sum;
        double[] probs_i;
        for (int i = 0; i < numberOfSamples; ++i) {
            p0 = Math.random(); p1 = Math.random(); p2 = Math.random();
            sum = p0 + p1 + p2;
            probs_i = probs[i];
            probs_i[0] = p0 / sum; probs_i[1] = p1 / sum; probs_i[2] = p2 / sum;
        }
        return probs;
    }

    static byte[] getHC(double[][] imputed, float threshold) {
        final int len = imputed.length;
        final byte[] hc = new byte[len];
        double[] probs_i;
        for (int i = 0; i < len; ++i) {
            probs_i = imputed[i];
            if (probs_i[0] > threshold) hc[i] = 0;
            else if (probs_i[1] > threshold) hc[i] = 1;
            else if (probs_i[2] > threshold) hc[i] = 2;
            else hc[i] = 3;
        }
        return hc;
    }

    static double[] getDosages(double[][] probs) {
        final double[] dosages = new double[probs.length];
        double[] probs_i;
        for (int i = 0; i < probs.length; ++i) {
            probs_i = probs[i];
            dosages[i] = probs_i[1] + 2 * probs_i[2];
        }
        return dosages;
    }
}
