package org.gorpipe.model;

import org.gorpipe.model.genome.files.gor.GenomicIterator;

public class IteratorTestUtilities {
    public static int countRemainingLines(GenomicIterator it) {
        return countRemainingLines(it, Integer.MAX_VALUE);
    }

    public static int countRemainingLines(GenomicIterator it, int upperBound) {
        int count = 0;
        while (count < upperBound && it.hasNext()) {
            it.next();
            ++count;
        }
        return count;
    }
}
