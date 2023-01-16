/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package gorsat;

import gorsat.Analysis.SortAnalysis;
import gorsat.Analysis.TopN;
import gorsat.Commands.Analysis;
import gorsat.Iterators.PipeStepIteratorAdaptor;
import gorsat.process.GenericSessionFactory;
import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.session.GorSession;
import gorsat.process.GorSessionFactory;
import org.gorpipe.gor.model.Row;
import gorsat.Iterators.FastGorSource;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Created by sigmar on 21/12/15.
 */
public class UTestSortGenome {
    private File orderGor;
    private Path dir;
    private GorSession session;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUpTest() throws IOException {
        dir = Files.createTempDirectory("sorttest");
        GorSessionFactory factory = new GenericSessionFactory("", dir.toAbsolutePath().toString());
        session = factory.create();
        orderGor = FileTestUtils.createTempFile(workDir.getRoot(), "order.gor",
                "Chrom\tPos\tOrder\n" +
                        "chr1\t1\tb\n" +
                        "chr1\t1\ta\n" +
                        "chr2\t2\tc\n" +
                        "chr2\t1\td\n"
        );
    }


    @Test
    public void sortChromosomesOutOfOrder() {
        String gorcmd = "gorrows -p chr1:1-5 \n" +
                "| calc tempchrom '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,X,Y,M' \n" +
                "| split tempchrom\n" +
                "| calc newchrom 'chr'+tempchrom\n" +
                "| select newchrom,pos\n" +
                "| rename newchrom chrom\n" +
                "| sort genome\n" +
                "| verifyorder";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(gorcmd)) {
            Row prev = null;
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Assert.assertTrue("File should be returned in gor order", prev == null || row.compareTo(prev) >= 0);
                prev = row;
            }
        }
    }

    @Test
    public void testSortGenomeVcf() {
        String gorcmd = "gor ../tests/data/external/samtools/test.vcf | sort genome";

        try (GenomicIterator iterator = TestUtils.runGorPipeIterator(gorcmd)) {
            Row prev = null;
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Assert.assertTrue("File should be returned in gor order", prev == null || row.compareTo(prev) >= 0);
                prev = row;
            }
        }
    }

    @Test
    public void testSortGenomeInNorContextWithError() {
        String query = "nor -h ../tests/config/build37split.txt | sort 1000";

        try {
            TestUtils.runGorPipeLines(query);
        } catch (GorParsingException ex) {
            Assert.assertTrue("Should get parsing exception", ex.getMessage().contains("Cannot have binSize"));
        }
    }

    @Test
    public void testVcfGorOrder() {
        String curdir = new File(".").getAbsolutePath();
        GenomicIterator inputSource = new FastGorSource("../tests/data/external/samtools/test.vcf", curdir, session.getGorContext(), false, null, 0);
        Analysis analyser = new TopN(10);
        PipeStepIteratorAdaptor pit = new PipeStepIteratorAdaptor(inputSource, analyser, null);

        Row prev = null;
        int count = 0;
        while (pit.hasNext()) {
            count++;
            Row row = pit.next();
            Assert.assertTrue("File should be returned in gor order", prev == null || row.compareTo(prev) >= 0);
            prev = row;
        }
        pit.close();

        Assert.assertEquals(5, count);
    }

    @Test
    public void testGorOrder() throws IOException {
        String curdir = new File(".").getAbsolutePath();
        GenomicIterator inputSource = new FastGorSource(orderGor.getCanonicalPath(), curdir, session.getGorContext(), false, null, 0);
        String header = inputSource.getHeader();
        Analysis analyser = new SortAnalysis(header, session, 500000000, null);
        PipeStepIteratorAdaptor pit = new PipeStepIteratorAdaptor(inputSource, analyser, header);

        Row prev = null;
        int count = 0;
        while (pit.hasNext()) {
            count++;
            Row row = pit.next();
            int comp = prev != null ? row.compareTo(prev) : 0;
            Assert.assertTrue("File should be returned in gor order", comp >= 0);
            prev = row;
        }
        pit.close();
        Files.walkFileTree(dir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        Files.deleteIfExists(dir);

        Assert.assertEquals("Wrong number of lines in sorted file", 4, count);
    }

    @Test
    @Ignore("Changing system property is not thread safe.")
    public void testDeleteOnExit() throws IOException {
        String origTmpdir = System.getProperty("java.io.tmpdir");
        String ourTmpDir = origTmpdir + "/testDeleteOnExit";
        System.setProperty("java.io.tmpdir", ourTmpDir);
        File directory = new File(ourTmpDir);
        directory.mkdirs();
        System.setProperty("gor.sort.batchSize", "2");
        TestUtils.runGorPipe("gor " + orderGor.getCanonicalPath() + "| sort genome");
        Assert.assertEquals(0, Objects.requireNonNull(directory.listFiles()).length);
        FileUtils.deleteDirectory(directory);
        System.setProperty("java.io.tmpdir", origTmpdir);
        System.setProperty("gor.sort.batchSize", "2000000");
    }

    @Test
    public void testGorSortWithSelfMerge() {
        String query = "gor -p chr1 <(../tests/data/gor/dbsnp_test.gorz | sort 100000 | merge <(gor ../tests/data/gor/dbsnp_test.gorz) )| group 1 -gc 3- -count | throwif allcount != 2";
        TestUtils.runGorPipe(query);

        query = "gor -p chr1 <(../tests/data/gor/dbsnp_test.gorz | sort 1 | merge <(gor ../tests/data/gor/dbsnp_test.gorz) )| group 1 -gc 3- -count | throwif allcount != 2";
        TestUtils.runGorPipe(query);
    }
}
