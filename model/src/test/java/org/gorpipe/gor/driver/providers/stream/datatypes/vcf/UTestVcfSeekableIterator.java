package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.binsearch.TestFileGenerator;
import org.gorpipe.gor.binsearch.UTestSeekableGenomicIterator;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Row;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class UTestVcfSeekableIterator extends UTestSeekableGenomicIterator {

    public static File workDir;

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.deleteDirectory(workDir);
    }

    @Parameterized.Parameters(name = "Test file: {0}")
    public static Collection<Object[]> data() throws IOException {
        workDir = Files.createTempDirectory("uTestGorSeekableIterator").toFile();
        final TestFileGenerator[] testFileGenerators = new TestFileGenerator[] {
                new TestFileGenerator("BASIC_GOR_FILE", workDir,10,1, NOT_SO_BIG_NUMBER, false),
                new TestFileGenerator("GOR_FILE_WITH_LONG_LINES", workDir, 10, 1, BIG_NUMBER, false),
                new TestFileGenerator("GOR_FILE_WITH_MANY_LINES", workDir, 5, 100, NOT_SO_BIG_NUMBER,false),
                new TestFileGenerator("PATHOLOGICAL_GOR_FILE", workDir,5, 10, BIG_NUMBER, false)
        };
        for (TestFileGenerator testFileGenerator : testFileGenerators) {
            testFileGenerator.writeVCF();
        }
        return Arrays.stream(testFileGenerators).map(testFile -> new Object[]{testFile}).collect(Collectors.toList());
    }

    @Test
    public void verifyGorOrder() {
        final GenomicIterator it = getIterator(testFileGenerator.path);
        String lastChr = "";
        int lastPos = 0;
        while (it.hasNext()) {
            final Row row = it.next();
            Assert.assertTrue((lastChr.equals(row.chr) && lastPos <= row.pos) || lastChr.compareTo(row.chr) < 0);
            lastChr = row.chr;
            lastPos = row.pos;
        }
    }

    @Override
    public GenomicIterator getIterator(String path) {
        try {
            return new VcfIteratorFactory().createIterator(new StreamSourceFile(new FileSource(new SourceReference(path))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
