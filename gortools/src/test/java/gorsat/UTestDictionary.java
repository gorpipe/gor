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

import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GorOptions;
import org.gorpipe.test.utils.FileTestUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UTestDictionary {
    private File gorFile;
    private File dictionaryFile;
    private File dictionaryFileWithHeader;
    private File pnFile;
    private File pnFile2;
    private File pnFileWithoutHeader;
    private File rangeDictionaryFile;
    private File gorFileBucketA;
    private File gorFileBucketB;
    private File dicionaryFileWihtBuckets;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        gorFile = FileTestUtils.createGenericSmallGorFile(workDir.getRoot());
        dictionaryFile = FileTestUtils.createGenericDictionaryFile(workDir.getRoot(), gorFile.getCanonicalPath(), "generic.gord");
        dictionaryFileWithHeader = FileTestUtils.createTempFile(workDir.getRoot(), "genericWithHeader.gord",
                "#gorfile\ttag\n" +
                        gorFile.getCanonicalPath() + "\ta\n" +
                        gorFile.getCanonicalPath() + "\tb\n"
        );
        pnFile = FileTestUtils.createPNTsvFile(workDir.getRoot());
        pnFileWithoutHeader = FileTestUtils.createPNTxtFile(workDir.getRoot());
        pnFile2 = FileTestUtils.createTempFile(workDir.getRoot(), "pns2.txt", "#PN\na\nb\nc\n");
        String genesPath = Paths.get("../tests/data/gor/genes.gorz").toFile().getCanonicalPath();
        rangeDictionaryFile = FileTestUtils.createTempFile(workDir.getRoot(), "range.gord",
                genesPath + "\tOTHER\tchr10\t1\tchrZ\t1\tbull\n");

        gorFileBucketA = FileTestUtils.createGenericSmallGorFileBucket(workDir.getRoot(), "a");
        gorFileBucketB = FileTestUtils.createGenericSmallGorFileBucket(workDir.getRoot(), "b");
        dicionaryFileWihtBuckets = FileTestUtils.createTempFile(workDir.getRoot(), "buckets.gord",
                String.format("%s|%s\ta\n%s|%s\tb\n",
                        gorFile.getCanonicalPath(), gorFileBucketA.getCanonicalPath(), gorFile.getCanonicalPath(), gorFileBucketB.getCanonicalPath()));

    }

    @Test
    public void testHeaderNameClash() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -s Gene_Symbol -f a | top 1 | select 1,2,Gene_Symbolx)");
    }

    @Test
    public void testPartgorDictionary() throws IOException {
        String[] args = new String[]{"create xxx = partgor -dict " + dictionaryFile.getCanonicalPath() + " -ff " + pnFile.getCanonicalPath() + " <(gor " + dictionaryFile.getCanonicalPath() + " -f #{tags}) | signature -timeres 1; gor [xxx]"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Partgor should return all data lines from dictionary", 18, count);
    }

    @Test
    public void testPartgorDictionaryVirtualPNList() throws IOException {
        String[] args = new String[]{"create pns = nor " + pnFile.getCanonicalPath() + ";" +
                "create xxx = partgor -dict " + dictionaryFile.getCanonicalPath() + " -ff [pns] <(gor " + dictionaryFile.getCanonicalPath() + " -f #{tags}) | signature -timeres 1; " +
                "gor [xxx]"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Nor should read dictionary file, not the files in the dictionary", 18, count);
    }

    @Test
    public void testGorDictionary() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -ff " + pnFile.getCanonicalPath() + ""};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Gor should return all data lines from dictionary", 18, count);
    }

    @Test
    public void testGorDictionaryWithoutHeader() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -ff " + pnFileWithoutHeader.getCanonicalPath() + ""};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Gor should return all data lines from dictionary", 18, count);
    }

    @Test(expected = GorDataException.class)
    public void testGorDictionaryFilterWithMissingPNsShouldReturnError() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -f a,b,c"};
        TestUtils.runGorPipeCount(args);
    }

    @Test
    public void testGorDictionaryFilterWithMissingPNsShouldNotReturnError() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -f a,b,c -fs "};
        TestUtils.runGorPipeCount(args);
    }

    @Test(expected = GorDataException.class)
    public void testGorDictionaryFilterSingleTagMissing() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -f c )"); // todo want exception
    }

    @Test(expected = GorDataException.class)
    public void testGorDictionaryWithHeaderFilterSingleTagMissing() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFileWithHeader.getCanonicalPath() + " -f c )"); // todo want exception
    }

    @Test(expected = GorDataException.class)
    public void testGorDictionaryFilterMultipeTagsMissing() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -f c,d )"); // todo want exception
    }


    @Test(expected = GorResourceException.class)
    public void testGorDictionaryFilterFSingleTagMissing() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -ff c )"); // todo want exception
    }

    @Test(expected = GorResourceException.class)
    public void testGorDictionaryFilterFMultipeTagsMissing() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -ff c,d )"); // todo want exception
    }

    @Test
    public void testGorDictionaryFilterTagsMissingSilent() throws IOException {
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -f c -fs )");
        TestUtils.runGorPipe("gor <(" + dictionaryFile.getCanonicalPath() + " -f c,d -fs )");
    }

    @Test
    public void testGorDictionaryTags() throws IOException {
        Assert.assertEquals(9, TestUtils.runGorPipeCount("gor <(" + dictionaryFileWithHeader.getCanonicalPath() + " -f a )"));
        Assert.assertEquals(9, TestUtils.runGorPipeCount("gor <(" + dictionaryFileWithHeader.getCanonicalPath() + " -f b )"));
        Assert.assertEquals(9, TestUtils.runGorPipeCount("gor <(" + dictionaryFile.getCanonicalPath() + " -f a )"));
        Assert.assertEquals(9, TestUtils.runGorPipeCount("gor <(" + dictionaryFile.getCanonicalPath() + " -f b )"));

    }

    @Test
    public void testGorDictionaryFilterWithMissingPNsIgnoringFilterError() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -f a,b,c -fs"};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Gor should return all data lines from dictionary", 18, count);
    }

    @Test(expected = GorDataException.class)
    public void testGorDictionaryFileFilterWithMissingPNsShouldReturnError() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -ff " + pnFile2.getAbsolutePath()};
        TestUtils.runGorPipeCount(args);
    }

    @Test
    public void testGorDictionaryFileFilterWithMissingPNsIgnoringFilter() throws IOException {
        String[] args = new String[]{"gor " + dictionaryFile.getCanonicalPath() + " -fs -ff " + pnFile2.getAbsolutePath()};
        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals("Gor should return all data lines from dictionary", 18, count);
    }

    @Test
    public void testRangeDictChr1() throws IOException {
        int count = TestUtils.runGorPipeCount("gor -p chr1 " + rangeDictionaryFile.getCanonicalPath());
        Assert.assertEquals("There should be no data lines for chromosome 1 in the dictionary", 0, count);
    }

    @Test
    public void testRangeDictChr2() throws IOException {
        int count = TestUtils.runGorPipeCount("gor -p chr2 " + rangeDictionaryFile.getCanonicalPath());
        Assert.assertEquals("There should be data lines for chromosome 2 in dictionary", 3507, count);
    }

    @Test
    public void testRangeDictChrY() throws IOException {
        int count = TestUtils.runGorPipeCount("gor -p chrY " + rangeDictionaryFile.getCanonicalPath());
        Assert.assertEquals("There should be data lines for chromosome Y in dictionary", 480, count);
    }

    @Test
    public void testRangeDictChrM() throws IOException {
        int count = TestUtils.runGorPipeCount("gor -p chrM " + rangeDictionaryFile.getCanonicalPath());
        Assert.assertEquals("There should be data lines for chromosome M in dictionary", 37, count);
    }

    @Test
    public void seekInDict() throws IOException {
        File dictFile = createSimpleDictWithBucket();

        GorOptions options = GorOptions.createGorOptions(dictFile.getAbsolutePath());
        GenomicIterator iterator = options.getIterator();
        iterator.seek("chr1", 1);

        int counter = 0;
        while (iterator.hasNext()) {
            iterator.next();
            counter++;
        }
        Assert.assertEquals(4, counter);
    }


    @Test
    public void testSourceColumnOnPlainDictionary() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dictionaryFile.getCanonicalPath())) {
            Assert.assertEquals("Plain dictionary should include source column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnBucketsDictionary() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dicionaryFileWihtBuckets.getCanonicalPath())) {
            Assert.assertEquals("Buckets dictionary should include source column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tSource", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnPlainDictionaryWithFFlag() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dictionaryFile.getCanonicalPath() + " -f a")) {
            Assert.assertEquals("Plain dictionary with -f should include Source column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tSource", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnBucketsDictionaryWithFFlag() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dicionaryFileWihtBuckets.getCanonicalPath() + " -f a")) {
            Assert.assertEquals("Buckets dictionary with -f should include Source column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tSource", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnPlainDictionaryWithSFlag() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dictionaryFile.getCanonicalPath() + " -s Source")) {
            Assert.assertEquals("Plain dictionary with -s should include Source column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tSource", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnBucketsDictionaryWithSFlag() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dicionaryFileWihtBuckets.getCanonicalPath() + " -s Source")) {
            Assert.assertEquals("Buckets dictionary with -s should include Source column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tSource", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnPlainDictionaryWithSFlagPN() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dictionaryFile.getCanonicalPath() + " -s PN")) {
            Assert.assertEquals("Plain dictionary with -s PN should include PN column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tPN", rs.getHeader());
        }
    }

    @Test
    public void testSourceColumnOnBucketsDictionaryWithSFlagPN() throws IOException {
        try(GenomicIterator rs = TestUtils.runGorPipeIterator("gor " + dicionaryFileWihtBuckets.getCanonicalPath() + " -s PN")) {
            Assert.assertEquals("Buckets dictionary with -s PN should include PN column",
                    "Chrom\tgene_start\tgene_end\tGene_Symbol\tPN", rs.getHeader());
        }
    }


    @Test
    public void testSourceColumnForBoundedDictsSimple() throws IOException {
        File dictFile = createSimpleDictWithBucket();
        String result = TestUtils.runGorPipe(String.format("pgor %s", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\tsource", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void testSourceColumnForBoundedDictsNoBuckets() throws IOException {
        File dictFile = createSimpleDictWithBucket();
        String result = TestUtils.runGorPipe(String.format("pgor %s -Y", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\tsource", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void testSourceColumnForBoundedDictsFilter() throws IOException {
        File dictFile = createSimpleDictWithBucket();
        String result = TestUtils.runGorPipe(String.format("gor %s -f 1", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\tsource", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void testSourceColumnForBoundedDictsSimpleNonStandardSource() throws IOException {
        File dictFile = createSimpleDictWithBucket("hickups");
        String result = TestUtils.runGorPipe(String.format("pgor %s", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\tsource", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void testSourceColumnForBoundedDictsFilterNonStandardSource() throws IOException {
        File dictFile = createSimpleDictWithBucket("hickups");
        String result = TestUtils.runGorPipe(String.format("gor %s -f 1", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\tsource", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void testSourceColumnForBoundedDictsSimpleMinusS() throws IOException {
        File dictFile = createSimpleDictWithBucket("hickups");
        String result = TestUtils.runGorPipe(String.format("pgor %s -s taboo", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\ttaboo", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void testSourceColumnForBoundedDictsFilterMinusS() throws IOException {
        File dictFile = createSimpleDictWithBucket("hickups");
        String result = TestUtils.runGorPipe(String.format("gor %s -f 1 -s taboo", dictFile.getAbsolutePath()));
        Assert.assertEquals("chrom\tpos\tdata\ttaboo", result.split("\n")[0].toLowerCase());
    }

    @Test
    public void test_noFilterHorizontalBuckets() throws IOException {
        final String gorCont = "CHROM\tPOS\tID\tREF\tALT\tBUCKET\tVALUES\nchr1\t1\trs1\tA\tC\t1\t~~~~\n";
        final File gorFile = workDir.newFile("gorfile.gor");
        final String dictCont = gorFile.getAbsolutePath() + "\t1\tchr1\t0\tchrZ\t1000000000\tPN1,PN2\n";
        final File dictFile = workDir.newFile("dictFile.gord");
        FileUtils.writeStringToFile(gorFile, gorCont, "UTF-8");
        FileUtils.writeStringToFile(dictFile, dictCont, "UTF-8");
        final String results = TestUtils.runGorPipe("gor " + dictFile.getAbsolutePath() + " -nf -f PN1");
        Assert.assertEquals(2, results.split("\n").length);
    }

    @Test
    public void testReadDictionaryWithServerFileReader() throws IOException {
        Path gordPath = workDir.getRoot().toPath().resolve("dbsnp.gord");
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gorz"),workDir.getRoot().toPath().resolve("dbsnp.gorz"));
        Files.writeString(gordPath,"dbsnp.gorz\tuu\n");
        String[] args = {"gor "+gordPath.getFileName().toString(),"-gorroot", workDir.getRoot().getPath()};
        int count = TestUtils.runGorPipeCount(args, true);
        Assert.assertEquals(count, 48);
    }

    @Test
    public void testReadOutOfScopeDictionaryWithServerFileReader() throws IOException {
        Path gordPath = workDir.getRoot().toPath().getParent().resolve("dbsnp.gord");
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gorz"),workDir.getRoot().toPath().resolve("dbsnp.gorz"));
        Files.writeString(gordPath,"dbsnp.gorz\tuu\n");
        String[] args = {"gor "+ gordPath.toAbsolutePath(),"-gorroot", workDir.getRoot().getPath()};
        boolean failed = false;
        try {
            TestUtils.runGorPipeCount(args, true);
        } catch(Exception e) {
            failed = true;
        }
        Assert.assertTrue("Out of project scope query should have failed", failed);
    }

    @Test
    public void testReadOutOfScopeGor() throws IOException {
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gorz"),workDir.getRoot().toPath().getParent().resolve("dbsnp.gorz"), StandardCopyOption.REPLACE_EXISTING);
        String[] args = {"gor ../dbsnp.gorz","-gorroot", workDir.getRoot().getPath()};
        boolean failed = false;
        try {
            TestUtils.runGorPipeCount(args, true);
        } catch(Exception e) {
            failed = true;
        }
        Assert.assertTrue("Out of project scope query should have failed", failed);
    }

    @Test
    public void testMissingGorFromGord() throws IOException {
        String dbsnpname = "dbsnp.gorz";
        Path dbsnp = Paths.get("../tests/data/gor/dbsnp_test.gorz");
        String query = "create xxx = pgor "+dbsnpname+"; gor [xxx] | top 1";
        Path root = workDir.getRoot().toPath();
        Files.copy(dbsnp,root.resolve(dbsnpname));
        Path cache = Paths.get("result_cache");
        Path cacheroot = root.resolve(cache);
        Files.createDirectory(cacheroot);
        String result = TestUtils.runGorPipe(query,"-gorroot",root.toString(),"-cachedir",cache.toString());
        Path p = Files.walk(cacheroot).filter(gorp -> gorp.toString().endsWith(".gorz")).findFirst().get();
        Files.delete(root.resolve(cache).resolve(p));
        String result2 = TestUtils.runGorPipe(query,"-gorroot",root.toString(),"-cachedir",cache.toString());
        Assert.assertEquals(result, result2);
    }

    @Test
    public void testDictCacheSameNameDifferentProjects() throws IOException {
        var one = workDir.getRoot().toPath().resolve("one");
        var two = workDir.getRoot().toPath().resolve("two");
        Files.createDirectory(one);
        Files.createDirectory(two);
        var onegene = one.resolve("genes.gord");
        var twogene = two.resolve("genes.gord");
        Files.writeString(onegene, "dbsnp.gorz\taa\n");
        Files.writeString(twogene, "dbsnp.gorz\tbb\n");
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gorz"),one.resolve("dbsnp.gorz"));
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gorz"),two.resolve("dbsnp.gorz"));

        var argsone = new String[] {"gor genes.gord -f aa", "-gorroot", one.toAbsolutePath().toString()};
        var argstwo = new String[] {"gor genes.gord -f aa", "-gorroot", one.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(argsone, true);
        TestUtils.runGorPipeCount(argstwo, true);
    }

    private File createSimpleDictWithBucket() throws IOException {
        return createSimpleDictWithBucket("source");
    }

    private File createSimpleDictWithBucket(String sourceColumn) throws IOException {
        String dict = "%s|%s\t1\tchr1\t0\tchr1\t10\n" +
                "%s|%s\t2\tchr1\t11\tchr1\t20\n";

        String first =  "chrom\tpos\tdata\n" +
                "chr1\t1\tchr1 1\n" +
                "chr1\t2\tchr1 2\n";

        String second = "chrom\tpos\tdata\n" +
                "chr1\t11\tchr1 11\n" +
                "chr1\t12\tchr1 12\n";

        String bucket = "chrom\tpos\tdata\t" + sourceColumn + "\n" +
                "chr1\t1\tchr1 1\t1\n" +
                "chr1\t2\tchr1 2\t1\n" +
                "chr1\t11\tchr1 11\t2\n" +
                "chr1\t12\tchr1 12\t2\n";

        File directory = FileTestUtils.createTempDirectory("UTestDictionary");
        File bucketFile = FileTestUtils.createTempFile(directory, "bucket.gor", bucket);
        File firstFile = FileTestUtils.createTempFile(directory, "first.gor", first);
        File secondFile = FileTestUtils.createTempFile(directory, "second.gor", second);

        String resolvedDict = String.format(dict, firstFile.getAbsolutePath(), bucketFile.getAbsolutePath(),
                secondFile.getAbsolutePath(), bucketFile.getAbsolutePath());
        return FileTestUtils.createTempFile(directory, "dict.gord", resolvedDict);
    }
}
