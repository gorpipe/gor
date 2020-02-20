package org.gorpipe.model.genome.files.gor.pgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestImputedRecordFactory {

    @Test
    public void test_merge() {
        boolean success = false;
        try {
            final ImputedRecordFactory irf = new ImputedRecordFactory(0.9f);
            irf.merge();
        } catch (IllegalStateException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test_add() {
        boolean success = false;
        try {
            final ImputedRecordFactory irf = new ImputedRecordFactory(0.9f);
            irf.add("");
        } catch (IllegalStateException e) {
            success = true;
        }
        Assert.assertTrue(success);
    }
}
