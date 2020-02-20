package org.gorpipe.model.genome.files.gor.pgen;

import org.gorpipe.model.gor.RowObj;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

public class UTestGroupingPGenWriter {

    private File workDir;

    @Before
    public void init() throws IOException {
        this.workDir = Files.createTempDirectory("utestGroupingPGenWriter").toFile();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(this.workDir);
    }

    @Test
    public void test_writeNothing() throws IOException {
        final File file = new File(workDir,"ghostFile.pgen");
        final String pGenPath = file.getAbsolutePath();
        final String pathMinusEnding = pGenPath.substring(0, pGenPath.lastIndexOf('.'));
        final String pVarPath = pathMinusEnding + ".pvar";
        final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(pGenPath);
        final PVarWriter pVarWriter = new PVarWriter(pVarPath);
        final VariantRecordFactory<BiAllelicHardCalls> vrFact = new HardCallRecordFactory();
        final GroupingPGenWriter writer = new GroupingPGenWriter(os, pVarWriter, vrFact, 2, 3, 4, 5);
        boolean success = true;
        try {
            writer.close();
        } catch (Exception e) {
            success = false;
        }
        Assert.assertTrue(success);
        Assert.assertFalse(file.exists());
        Assert.assertFalse(new File(pVarPath).exists());
    }

    @Test
    public void test_write_basic() throws Exception {
        final File file = new File(workDir, "multiAllelic.pgen");
        final String pGenPath = file.getAbsolutePath();
        final String pathMinusEnding = pGenPath.substring(0, pGenPath.lastIndexOf('.'));
        final String pVarPath = pathMinusEnding + ".pvar";
        final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(pGenPath);
        final PVarWriter pVarWriter = new PVarWriter(pVarPath);
        final VariantRecordFactory<BiAllelicHardCalls> vrFact = new HardCallRecordFactory();
        final GroupingPGenWriter writer = new GroupingPGenWriter(os, pVarWriter, vrFact, 2, 3, 4, 5);
        writer.write(RowObj.apply("chr1\t1\tA\tC\trs1\t01010103"));
        writer.write(RowObj.apply("chr1\t1\tA\tG\trs1\t00110013"));
        writer.write(RowObj.apply("chr1\t1\tA\tT\trs1\t00001113"));
        writer.write(RowObj.apply("chr1\t2\tA\tC\trs2\t03030303"));
        writer.write(RowObj.apply("chr1\t3\tA\tC\trs3\t01013333"));
        writer.write(RowObj.apply("chr1\t3\tA\tG\trs3\t00113333"));
        writer.close();

        Assert.assertEquals(51, file.length());
        final byte[] buffer = new byte[51];
        final FileInputStream fis = new FileInputStream(file);
        Assert.assertEquals(buffer.length, fis.read(buffer));
        final byte[] wanted = {0x6c, 0x1b, 0x10, 0x03, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x07, 0x23, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x08, 0x08, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0x94, (byte) 0xe9 , 0x00, 0x06, 0x02, 0x07, (byte) 0x84, 0x09, (byte) 0xcc, (byte) 0xcc, (byte) 0x94, (byte) 0xff, 0x00, 0x02, 0x01, 0x00};
        Assert.assertArrayEquals(wanted, buffer);
    }

    @Test
    public void test_write4Alts() throws Exception {
        final File file = new File(workDir, "fourAlts.pgen");
        final String pGenPath = file.getAbsolutePath();
        final String pathMinusEnding = pGenPath.substring(0, pGenPath.lastIndexOf('.'));
        final String pVarPath = pathMinusEnding + ".pvar";
        final VariableWidthPGenOutputStream os = new VariableWidthPGenOutputStream(pGenPath);
        final PVarWriter pVarWriter = new PVarWriter(pVarPath);
        final VariantRecordFactory<BiAllelicHardCalls> vrFact = new HardCallRecordFactory();
        final GroupingPGenWriter writer = new GroupingPGenWriter(os, pVarWriter, vrFact, 2, 3, 4, 5);
        writer.write(RowObj.apply("chr1\t1\tA\tAA\trs1\t010101001003"));
        writer.write(RowObj.apply("chr1\t1\tA\tAC\trs1\t001100100103"));
        writer.write(RowObj.apply("chr1\t1\tA\tAG\trs1\t000011100013"));
        writer.write(RowObj.apply("chr1\t1\tA\tAT\trs1\t000000011113"));
        writer.close();

        Assert.assertEquals(35, file.length());
        final byte[] buffer = new byte[35];
        final FileInputStream fis = new FileInputStream(file);
        Assert.assertEquals(buffer.length, fis.read(buffer));
        final byte[] wanted = {0x6c, 0x1b, 0x10, 0x01, 0x00, 0x00, 0x00, 0x0c, 0x00, 0x00, 0x00, 0x07, 0x19, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x0a, 0x00, 0x00, 0x00, (byte) 0x94, 0x69, (byte) 0xea, 0x00, 0x0e, 0x24, 0x3f, (byte) 0x84, (byte) 0xc9, (byte) 0xed};
        Assert.assertArrayEquals(wanted, buffer);
    }
}
