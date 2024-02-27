package gorsat;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class UTestDeflateInflateColumn {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private static final Random r = new Random();

    @Test
    public void testDeflateColumn() {
        // Create a very large row
        var numberOfRows = 100;

        try {
            var file = workDir.newFile("large_columns.gor");
            try(var writer = new FileWriter(file)) {
                writer.write("chrom\tpos\tvalue\n");
                for (int i = 1; i <= numberOfRows; i++) {
                    writer.write("chr1\t" + i + "\t" + getRandomString() + "\n");
                }
            }

            var result = TestUtils.runGorPipeLines("pgor " + file + " | deflatecolumn value -m 100");

            Assert.assertEquals(numberOfRows + 1, result.length);
            for (int i = 1; i < result.length; i++) {
                var row = result[i];
                var columns = row.split("\t");
                Assert.assertEquals(3, columns.length);
                Assert.assertTrue(columns[2].startsWith("zip::"));
                var cols = columns[2].split("::");
                Assert.assertEquals(3, cols.length);
                Assert.assertTrue(StringUtils.isNumeric(cols[1]));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Clean up
            workDir.delete();
        }
    }

    @Test
    public void testInflateColumn() {
        // Create a very large row
        var numberOfRows = 100;

        try {
            var file = workDir.newFile("large_columns.gor");
            try(var writer = new FileWriter(file)) {
                writer.write("chrom\tpos\tvalue\n");
                for (int i = 1; i <= numberOfRows; i++) {
                    writer.write("chr1\t" + i + "\t" + getRandomString() + "\n");
                }
            }

            var result = TestUtils.runGorPipeLines("gor " + file + " | deflatecolumn value -m 100");

            file = workDir.newFile("large_columns_deflated.gor");
            try(var writer = new FileWriter(file)) {
                for (String s : result) {
                    writer.write(s);
                }
            }

            result = TestUtils.runGorPipeLines("gor " + file + " | inflatecolumn value");

            Assert.assertEquals(numberOfRows + 1, result.length);
            for (int i = 1; i < result.length; i++) {
                var row = result[i];
                var columns = row.split("\t");
                Assert.assertEquals(3, columns.length);
                Assert.assertFalse(columns[2].startsWith("zip::"));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Clean up
            workDir.delete();
        }


    }


    private String getRandomString() {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        var size = (int)(500 * r.nextDouble() * 1024);
        var counter = 0;
        var builder = new StringBuilder();

        while (counter < size) {
            var segment_size = (int)(r.nextDouble() * 1024);
            counter += segment_size;
            var charindex = (int)(r.nextDouble() * (AlphaNumericString.length() - 1));
            var character =  AlphaNumericString.substring(charindex, charindex + 1);
            builder.append(character.repeat(segment_size));
        }

        return builder.toString();
    }
}
