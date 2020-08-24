package org.gorpipe.model.util;

/**
 * An iterator over all combinations of a given size from a set of a given size.
 */
public class CombinationIterator {

    private final int m, n;
    private final boolean[] incl;
    private boolean hasNext;

    public CombinationIterator(int m, int n) {
        this.m = m;
        this.n = n;
        this.incl = new boolean[m];
        for (int i = 0; i < n; ++i) {
            this.incl[i] = true;
        }
        this.hasNext = true;
    }

    public boolean hasNext() {
        return this.hasNext;
    }

    public void next(int[] indices) {
        int j = 0;
        for (int i = 0; j < this.n; ++i) {
            if (this.incl[i]) indices[j++] = i;
        }
        computeNext();
    }

    private void computeNext() {
        int i = this.m - 1;
        while (i != -1 && this.incl[i]) --i;

        final int numberOfTrue = this.m - 1 - i;

        if (numberOfTrue == this.n) {
            this.hasNext = false;
            return;
        }

        while (!this.incl[i]) --i;
        this.incl[i++] = false;
        this.incl[i++] = true;
        final int upTo = i + numberOfTrue;
        while (i < upTo) this.incl[i++] = true;
        while (i < this.m) this.incl[i++] = false;
    }
}
