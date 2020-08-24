package org.gorpipe.gor.driver.bgen;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.zip.DataFormatException;

import static org.gorpipe.gor.driver.bgen.BGenOutputStream.getAltAlleleString;

public class UTestBGenOutputStream {
    private static File tmpDir;

    @BeforeClass
    public static void setup() throws IOException {
        tmpDir = Files.createTempDirectory("bgenOutputStreamTest").toFile();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }

    @Test
    public void test_writeNothing() {
        final File file = new File(tmpDir, "aBadIdea.bgen");
        final BGenOutputStream os = new BGenOutputStream(file.getAbsolutePath());
        os.close();
        Assert.assertFalse(file.exists());
    }

    @Test
    public void test_basic() throws IOException, SQLException, DataFormatException {
        final HardCallDataBlock db = new HardCallDataBlock();

        final String file = new File(tmpDir, "test.bgen").getAbsolutePath();
        final BGenOutputStream os = new BGenOutputStream(file);
        db.setVariables("chr1", 1, null, null, new boolean[4], new int[4], new int[4], "A", "C");
        os.write(db);
        db.setVariables("chr1", 2, null, null, new boolean[4], new int[4], new int[4], "A", "C");
        os.write(db);
        os.close();

        final SimpleBGenDriver bGenDriver = new SimpleBGenDriver(file);
        Assert.assertEquals(20, bGenDriver.offset);
        Assert.assertEquals(20, bGenDriver.headerLength);
        Assert.assertEquals(2, bGenDriver.numberOfVariants);
        Assert.assertEquals(4, bGenDriver.numberOfSamples);
        Assert.assertEquals(2, bGenDriver.layOut);
        Assert.assertEquals(1, bGenDriver.compressionType);
        Assert.assertTrue(bGenDriver.noSampleIdentifiers);
        //Header block validated


        final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file + ".bgi");
        final ResultSet rs = connection.createStatement().executeQuery("SELECT chromosome,position,file_start_position,size_in_bytes,number_of_alleles,reference_allele,alternative_alleles FROM Variant ORDER BY chromosome, position");

        Assert.assertTrue(bGenDriver.hasMore());
        Assert.assertTrue(rs.next());

        final SimpleBGenDriver.BGenBlock bGenBlock1 = bGenDriver.nextBlock();
        Assert.assertEquals(bGenBlock1.chr, rs.getString(1));
        Assert.assertEquals(bGenBlock1.pos, rs.getInt(2));
        Assert.assertEquals(bGenBlock1.offsetInFile, rs.getLong(3));
        Assert.assertEquals(bGenBlock1.sizeInBytes, rs.getLong(4));
        Assert.assertEquals(bGenBlock1.allCount, rs.getInt(5));
        Assert.assertEquals(String.join(",", bGenBlock1.alleles), rs.getString(6) + "," + rs.getString(7));

        Assert.assertTrue(bGenDriver.hasMore());
        Assert.assertTrue(rs.next());

        final SimpleBGenDriver.BGenBlock bGenBlock2 = bGenDriver.nextBlock();
        Assert.assertEquals(bGenBlock2.chr, rs.getString(1));
        Assert.assertEquals(bGenBlock2.pos, rs.getInt(2));
        Assert.assertEquals(bGenBlock2.offsetInFile, rs.getLong(3));
        Assert.assertEquals(bGenBlock2.sizeInBytes, rs.getLong(4));
        Assert.assertEquals(bGenBlock2.allCount, rs.getInt(5));
        Assert.assertEquals(String.join(",", bGenBlock2.alleles), rs.getString(6) + "," + rs.getString(7));

        Assert.assertFalse(bGenDriver.hasMore());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void test_basic_threeAlleles() throws IOException, SQLException, DataFormatException {
        final HardCallDataBlock db = new HardCallDataBlock();

        final String file = new File(tmpDir, "test2.bgen").getAbsolutePath();
        final BGenOutputStream os = new BGenOutputStream(file);
        db.setVariables("chr1", 1, null, null, new boolean[4], new int[4], new int[4], "A", "C", "G");
        os.write(db);
        db.setVariables("chr1", 2, null, null, new boolean[4], new int[4], new int[4], "A", "C", "G");
        os.write(db);
        os.close();

        final SimpleBGenDriver bGenDriver = new SimpleBGenDriver(file);
        Assert.assertEquals(20, bGenDriver.offset);
        Assert.assertEquals(20, bGenDriver.headerLength);
        Assert.assertEquals(2, bGenDriver.numberOfVariants);
        Assert.assertEquals(4, bGenDriver.numberOfSamples);
        Assert.assertEquals(2, bGenDriver.layOut);
        Assert.assertEquals(1, bGenDriver.compressionType);
        Assert.assertTrue(bGenDriver.noSampleIdentifiers);
        //Header block validated


        final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file + ".bgi");
        final ResultSet rs = connection.createStatement().executeQuery("SELECT chromosome,position,file_start_position,size_in_bytes,number_of_alleles,reference_allele,alternative_alleles FROM Variant ORDER BY chromosome, position");

        Assert.assertTrue(bGenDriver.hasMore());
        Assert.assertTrue(rs.next());

        final SimpleBGenDriver.BGenBlock bGenBlock1 = bGenDriver.nextBlock();
        Assert.assertEquals(bGenBlock1.chr, rs.getString(1));
        Assert.assertEquals(bGenBlock1.pos, rs.getInt(2));
        Assert.assertEquals(bGenBlock1.offsetInFile, rs.getLong(3));
        Assert.assertEquals(bGenBlock1.sizeInBytes, rs.getLong(4));
        Assert.assertEquals(bGenBlock1.allCount, rs.getInt(5));
        Assert.assertEquals(String.join(",", bGenBlock1.alleles), rs.getString(6) + "," + rs.getString(7));

        Assert.assertTrue(bGenDriver.hasMore());
        Assert.assertTrue(rs.next());

        final SimpleBGenDriver.BGenBlock bGenBlock2 = bGenDriver.nextBlock();
        Assert.assertEquals(bGenBlock2.chr, rs.getString(1));
        Assert.assertEquals(bGenBlock2.pos, rs.getInt(2));
        Assert.assertEquals(bGenBlock2.offsetInFile, rs.getLong(3));
        Assert.assertEquals(bGenBlock2.sizeInBytes, rs.getLong(4));
        Assert.assertEquals(bGenBlock2.allCount, rs.getInt(5));
        Assert.assertEquals(String.join(",", bGenBlock2.alleles), rs.getString(6) + "," + rs.getString(7));

        Assert.assertFalse(bGenDriver.hasMore());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void test_getAltAlleleString() {
        final String res1 = getAltAlleleString(new String[] {"A", "C"});
        Assert.assertEquals("C", res1);

        final String res2 = getAltAlleleString(new String[] {"A", "C", "G"});
        Assert.assertEquals("C,G", res2);
    }
}

