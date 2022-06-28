package org.gorpipe.gor.driver.pgen;

import org.apache.commons.io.FileUtils;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;

public class UTestSimplePGenWriter {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void test_write_hc() throws Exception {
        final File file = tf.newFile("hc.pgen");
        final String pGenPath = file.getAbsolutePath();
        final String pathMinusEnding = pGenPath.substring(0, pGenPath.lastIndexOf('.'));
        final String pVarPath = pathMinusEnding + ".pvar";
        final FWHardCallsPGenOutputStream os = new FWHardCallsPGenOutputStream(pGenPath);
        final PVarWriter pVarWriter = new PVarWriter(pVarPath);
        final VariantRecordFactory<BiAllelicHardCalls> vrFact = new HardCallRecordFactory();
        final SimplePGenWriter<BiAllelicHardCalls> writer = new SimplePGenWriter<>(os, pVarWriter, vrFact, 2, 3, 4, 5);
        writer.write(RowObj.apply("chr1\t1\tA\tC\trs1\t3210"));
        writer.close();

        final String pVarCont = FileUtils.readFileToString(new File(pVarPath), "UTF-8");
        final String wantedPVarCont = "#CHROM\tID\tPOS\tALT\tREF\n1\trs1\t1\tC\tA\n";
        Assert.assertEquals(wantedPVarCont, pVarCont);

        Assert.assertEquals(13, file.length());
        final FileInputStream fis = new FileInputStream(file);
        final byte[] buffer = new byte[13];
        Assert.assertEquals(buffer.length, fis.read(buffer));
        Assert.assertArrayEquals(new byte[]{0x6c, 0x1b, 0x02, 1, 0, 0, 0, 4, 0, 0, 0, 64, 0x1b}, buffer);
    }

    @Test
    public void test_write_imp() throws Exception {
        final File file = tf.newFile("imp.pgen");
        final String pGenPath = file.getAbsolutePath();
        final String pathMinusEnding = pGenPath.substring(0, pGenPath.lastIndexOf('.'));
        final String pVarPath = pathMinusEnding + ".pvar";
        final FWUnPhasedPGenOutputStream os = new FWUnPhasedPGenOutputStream(file.getAbsolutePath());
        final PVarWriter pVarWriter = new PVarWriter(pVarPath);
        final VariantRecordFactory<BiAllelicHardCallsAndDosages> vrFact = new ImputedRecordFactory(0.9f);
        final SimplePGenWriter<BiAllelicHardCallsAndDosages> writer = new SimplePGenWriter<>(os, pVarWriter, vrFact, 2, 3, 4, 5);
        writer.write(RowObj.apply("chr1\t1\tA\tC\trs1\t~~!~~!  "));
        writer.close();

        final String pVarCont = FileUtils.readFileToString(new File(pVarPath), "UTF-8");
        final String wantedPVarCont = "#CHROM\tID\tPOS\tALT\tREF\n1\trs1\t1\tC\tA\n";
        Assert.assertEquals(wantedPVarCont, pVarCont);

        Assert.assertEquals(21, file.length());
        final FileInputStream fis = new FileInputStream(file);
        final byte[] buffer = new byte[21];
        Assert.assertEquals(buffer.length, fis.read(buffer));
        Assert.assertArrayEquals(new byte[]{0x6c, 0x1b, 0x03, 1, 0, 0, 0, 4, 0, 0, 0, 64, 0x64, 0, 0, 0, 0x40, 0, (byte) 0x80, 0x11, (byte) 0xc2}, buffer);
    }
}