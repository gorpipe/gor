package org.gorpipe.model.genome.files.gor.pgen;

import org.junit.Assert;
import org.junit.Test;

public class UTestBiAllelicHardCalls {

    @Test
    public void test_getType() {
        final BiAllelicHardCalls bahc = new BiAllelicHardCalls(new byte[0]);
        Assert.assertEquals(0, bahc.getType());
    }
}
