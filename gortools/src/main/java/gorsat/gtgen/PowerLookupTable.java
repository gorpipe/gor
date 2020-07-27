package gorsat.gtgen;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class PowerLookupTable {
    private final double[] powers;
    private final double x;

    private PowerLookupTable(double x, int n) {
        this.powers = new double[n];
        double y = 1D;
        for (int i = 0; i < n; ++i) {
            this.powers[i] = y;
            y *= x;
        }
        this.x = x;
    }

    double pow(int e) {
        if (e < this.powers.length) {
            return this.powers[e];
        } else {
            return pow(x, e);
        }
    }

    /**
     * Computes x^n, n >= 0.
     */
    public static double pow(double x, int e) {
        if (e == 0) return 1D;
        double res = 1D;
        double mult;
        if (e > 0) {
            mult = x;
        } else {
            mult = 1.0 / x;
            e = -e;
        }
        do {
            if ((e & 1) == 1) {
                res *= mult;
            }
            e >>>= 1;
            if (e == 0) break;
            mult *= mult;
        } while (true);
        return res;
    }

    private static final Map<Double, WeakReference<PowerLookupTable>> cache = new HashMap<>();

    static synchronized PowerLookupTable getLookupTable(double x) {
        final WeakReference<PowerLookupTable> ref = cache.get(x);
        final PowerLookupTable toReturn;
        final PowerLookupTable candidate;
        if (ref == null || (candidate = ref.get()) == null) {
            toReturn = new PowerLookupTable(x, 100);
            cache.put(x, new WeakReference<>(toReturn));
        } else {
            toReturn = candidate;
        }
        return toReturn;
    }
}
