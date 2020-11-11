package org.gorpipe.gor.driver.pgen;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

public class UTestPVarWriter {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void testGetChrNum() {
        PVarWriter pVarWriter = new PVarWriter("");
        IntStream.rangeClosed(1, 27).forEach(chrNum -> {
            String chrString;
            if (chrNum < 23) chrString = "chr" + chrNum;
            else if (chrNum == 23) chrString = "chrX";
            else if (chrNum == 24) chrString = "chrY";
            else if (chrNum == 25) chrString = "chrXY";
            else if (chrNum == 26) chrString = "chrMT";
            else chrString = "Unknown";
            Assert.assertEquals(chrNum, Integer.parseInt(pVarWriter.getChrNum(chrString)));
        });
    }

    @Test
    public void test_writeEmpty() throws IOException {
        final Path path = tf.getRoot().toPath().resolve("empty.pvar");
        final PVarWriter writer = new PVarWriter(path.toString());
        writer.close();

        Assert.assertFalse(Files.exists(path));
    }
}
