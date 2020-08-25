package org.gorpipe.gor.driver.pgen;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class UTestPGenWriterFactory {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void test_getPGenWriter_groupAndImp() throws IOException {
        final String fileName = tf.newFile("groupAndImp.pgen").getAbsolutePath();
        boolean success = false;
        try {
            PGenWriterFactory.getPGenWriter(fileName, 2, 3, 4, 5, true, true, 0.95f);
        } catch (IllegalArgumentException e) {
            success = true;
        } catch (Exception e) {
            //Failed :(
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test_getPGenWriter_groupAndHC() throws IOException {
        final String fileName = tf.newFile("groupAndHc.pgen").getAbsolutePath();
        final PGenWriter pGenWriter = PGenWriterFactory.getPGenWriter(fileName, 2, 3, 4, 5, true, false, -1f);
        Assert.assertTrue(pGenWriter instanceof GroupingPGenWriter);
        Assert.assertTrue(pGenWriter.os instanceof VariableWidthPGenOutputStream);
        Assert.assertTrue(pGenWriter.vrFact instanceof HardCallRecordFactory);
    }

    @Test
    public void test_getPGenWriter_imp() throws IOException {
        final String fileName = tf.newFile("imp.pgen").getAbsolutePath();
        final PGenWriter pGenWriter = PGenWriterFactory.getPGenWriter(fileName, 2, 3, 4, 5, false, true, 0.95f);
        Assert.assertTrue(pGenWriter instanceof SimplePGenWriter);
        Assert.assertTrue(pGenWriter.os instanceof FWUnPhasedPGenOutputStream);
        Assert.assertTrue(pGenWriter.vrFact instanceof ImputedRecordFactory);
    }

    @Test
    public void test_getPGenWrite_hc() throws IOException {
        final String fileName = tf.newFile("hc.pgen").getAbsolutePath();
        final PGenWriter pGenWriter = PGenWriterFactory.getPGenWriter(fileName, 2, 3, 4, 5, false, false, 0.95f);
        Assert.assertTrue(pGenWriter instanceof SimplePGenWriter);
        Assert.assertTrue(pGenWriter.os instanceof FWHardCallsPGenOutputStream);
        Assert.assertTrue(pGenWriter.vrFact instanceof HardCallRecordFactory);
    }
}
