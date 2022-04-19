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
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Created by hjalti on 13/06/17.
 */
public class UTestGorWrite {
    private Path tmpdir;

    @Before
    public void setupTest() throws IOException {
        tmpdir = Files.createTempDirectory("test_gor_write");
        tmpdir.toFile().deleteOnExit();
    }

    @Test
    public void testWritePathWithLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, tmpdir.resolve("dbsnp.gor"));
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", tmpdir.toString());

        Assert.assertEquals(tmpdir.resolve("dbsnp2.gor").toString() + "\n", Files.readString(tmpdir.resolve("dbsnp3.gor.link")));

        String linkresult = TestUtils.runGorPipe("gor dbsnp3.gor | top 1", "-gorroot", tmpdir.toString());
        Assert.assertEquals("Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                "chr1\t10179\tC\tCC\trs367896724\n", linkresult);
    }

    @Test
    public void testWritePathWithServerLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, tmpdir.resolve("dbsnp.gor"));
        TestUtils.runGorPipe(new String[] {"gor dbsnp.gor | write user_data/dbsnp2.gor -link user_data/dbsnp3.gor", "-gorroot", tmpdir.toString()}, true);

        Assert.assertEquals(tmpdir.resolve("user_data").resolve("dbsnp2.gor").toString() + "\n", Files.readString(tmpdir.resolve("user_data").resolve("dbsnp3.gor.link")));

        String linkresult = TestUtils.runGorPipe("gor user_data/dbsnp3.gor | top 1", "-gorroot", tmpdir.toString());
        Assert.assertEquals("Chrom\tPOS\treference\tallele\tdifferentrsIDs\n" +
                "chr1\t10179\tC\tCC\trs367896724\n", linkresult);
    }

    @Test
    public void testWritePathWithUnAuthorizedServerLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, tmpdir.resolve("dbsnp.gor"));
        try {
            TestUtils.runGorPipe(new String[]{"gor dbsnp.gor | write user_data/dbsnp2.gor -link /tmp/dbsnp3.gor", "-gorroot", tmpdir.toString()}, true);
            Assert.fail("Should not allow generating link file");
        } catch (GorResourceException e) {
            Assert.assertTrue(e.getMessage().contains("/tmp/dbsnp3.gor"));
        }
    }

    @Test(expected = Exception.class)
    public void testWritePathWithInaccessiableLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, tmpdir.resolve("dbsnp.gor"));
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link s3://bacbucket/dbsnp3.gor", "-gorroot", tmpdir.toString());
    }

    @Test
    public void testWritePathWithExistingLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, tmpdir.resolve("dbsnp.gor"));
        Files.writeString(tmpdir.resolve("dbsnp3.gor.link"), tmpdir.resolve("dbsnp.gor").toString() + "\n");
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", tmpdir.toString());

        Assert.assertEquals(tmpdir.resolve("dbsnp2.gor").toString() + "\n", Files.readString(tmpdir.resolve("dbsnp3.gor.link")));
    }

    @Test
    public void testWritePathWithExistingBadLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, tmpdir.resolve("dbsnp.gor"));
        Files.writeString(tmpdir.resolve("dbsnp3.gor.link"), "");
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", tmpdir.toString());

        Assert.assertEquals(tmpdir.resolve("dbsnp2.gor").toString() + "\n", Files.readString(tmpdir.resolve("dbsnp3.gor.link")));
    }

    @Test
    public void testTxtWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, tmpdir.resolve("simple1.nor"));
        String[] args = {"nor simple1.nor | select Chrom | calc p 1 | write test/new.txt", "-gorroot", tmpdir.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Ignore
    @Test
    public void testTxtFolderWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, tmpdir.resolve("simple1.nor"));
        String[] args = {"nor simple1.nor | select Chrom | calc p 1 | write -d test new.txt", "-gorroot", tmpdir.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Ignore
    @Test
    public void testFolderWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, tmpdir.resolve("simple1.nor"));
        String[] args = {"nor simple1.nor | select Chrom | calc p 1 | write -d test", "-gorroot", tmpdir.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Test
    public void testTsvWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, tmpdir.resolve("simple2.nor"));
        String[] args = {"nor simple2.nor | select Chrom | calc p 1 | write test/new.tsv", "-gorroot", tmpdir.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Test
    public void testGorWriteWithMd5() throws IOException {
        Path tmpfile = tmpdir.resolve("genes_md5.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor ../tests/data/gor/genes.gorz | write -m " + tmpfile.toAbsolutePath().normalize();

        TestUtils.runGorPipeCount(query);

        Path path = tmpdir.resolve("genes_md5.gorz.md5");
        Assert.assertTrue("Md5 file does not exist", Files.exists(path));
        path.toFile().deleteOnExit();
        String md5str = new String(Files.readAllBytes(path));
        Assert.assertEquals("Not a valid md5 string", 32, md5str.length());
    }

    @Test
    public void testGorWriteColumnNumber() {
        Path tmpfile = tmpdir.resolve("genes_md5.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor ../tests/data/gor/genes.gorz | write -m " + tmpfile.toAbsolutePath().normalize();
        String headerRes = TestUtils.runGorPipe(query);
        Assert.assertEquals("", "Chrom\tgene_start\n", headerRes);
    }

    @Test
    public void testGorzWriteCompressionLevel() throws IOException {
        Path tmpfile = tmpdir.resolve("genes.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor ../tests/data/gor/genes.gor | write -l 9 " + tmpfile.toAbsolutePath().normalize();

        TestUtils.runGorPipeCount(query);

        Path originalGenesGorz = Paths.get("../tests/data/gor/genes.gorz");
        Assert.assertNotEquals("Files with different compression level are of same size", Files.size(tmpfile), Files.size(originalGenesGorz));
    }

    @Test
    @Ignore("This test is useful for load testing and performance analysis. Does not work without code change unless hekla or another proper csa volume is mounted at /mnt/csa")
    public void testGorWriteLargeFileWithMd5() throws IOException {
        Path tmpfile = tmpdir.resolve("large_md5.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor /mnt/csa/env/dev/projects/installation_test_project/ref/dbsnp.gorz | write -m " + tmpfile.toAbsolutePath().normalize().toString();

        TestUtils.runGorPipeCount(query);

        Path path = tmpdir.resolve("large_md5.gorz.md5");
        Assert.assertTrue("Md5 file does not exist", Files.exists(path));
        path.toFile().deleteOnExit();
        String md5str = new String(Files.readAllBytes(path));
        Assert.assertEquals("Not a valid md5 string", 32, md5str.length());
    }

    @Test
    public void testGorWriteReadWithIdx() throws IOException {
        Path tmpfile = tmpdir.resolve("genes.gorz");
        tmpfile.toFile().deleteOnExit();
        final String tmpFilePath = tmpfile.toAbsolutePath().normalize().toString();
        TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write -i CHROM " + tmpFilePath);

        Path path = tmpdir.resolve("genes.gorz.gori");
        Assert.assertTrue( "Seek index file does not exist", Files.exists(path) );
        path.toFile().deleteOnExit();

        Assert.assertTrue("Index file for " + tmpFilePath + " is incorrect.", assertIndexFileIsCorrect(tmpFilePath));

        final int count = TestUtils.runGorPipeCount("gor -p chr22 " + tmpfile.toAbsolutePath().normalize());

        Assert.assertEquals("Wrong number of lines in seekindexed gorz file", 1127, count);
        TestUtils.assertTwoGorpipeResults("Pgor on indexed gorz file returns different results than on unindexed one", "pgor "+tmpfile.toAbsolutePath().normalize().toString()+"|group chrom -count | signature -timeres 1", "pgor ../tests/data/gor/genes.gorz|group chrom -count");
    }

    @Test
    public void testGorWriteReadWithFullIdx() throws IOException {
        Path tmpfile = tmpdir.resolve("genes.gorz");
        tmpfile.toFile().deleteOnExit();
        final String tmpFilePath = tmpfile.toAbsolutePath().normalize().toString();
        TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write -i FULL " + tmpFilePath); //Create index file.

        Assert.assertTrue("Index file for " + tmpFilePath + " is incorrect.", assertIndexFileIsCorrect(tmpFilePath));

        Path path = tmpdir.resolve("genes.gorz.gori");
        Assert.assertTrue( "Seek index file does not exist", Files.exists(path) );
        path.toFile().deleteOnExit();


        final int count = TestUtils.runGorPipeCount("gor -p chr22 "+ tmpfile.toAbsolutePath().normalize());
        Assert.assertEquals("Wrong number of lines in seekindexed gorz file", 1127, count);
        TestUtils.assertTwoGorpipeResults("Pgor on indexed gorz file returns different results than on unindexed one", "pgor "+tmpfile.toAbsolutePath().normalize().toString()+"|group chrom -count", "pgor ../tests/data/gor/genes.gorz|group chrom -count");
    }

    @Test
    public void testWriteGor() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".gor").toAbsolutePath();
        TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("gor " + tmpfile, "gor ../tests/data/gor/genes.gorz");
        Assert.assertEquals("#Chrom	gene_start	gene_end	Gene_Symbol", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteNor() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".nor").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    @Ignore("Write uses chr")
    public void testWriteVcf() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".vcf").toAbsolutePath();
        TestUtils.runGorPipeCount("gor ../tests/data/external/samtools/test.vcf | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("gor " + tmpfile, "gor ../tests/data/external/samtools/test.vcf ");
        Assert.assertEquals("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	NA00001	NA00002	NA00003", Files.readAllLines(tmpfile).get(1));
    }

    @Test
    public void testWriteTsv() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".tsv").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteUnknownExt() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".non").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteNoHeaderUnknownExt() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".non").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/genes.gorz | top 1 | write -noheader " + tmpfile);
        Assert.assertEquals("chr1\t11868\t14412\tDDX11L1\n", FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
    }

    @Test
    public void testWriteNoHeaderVcf() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".vcf").toAbsolutePath();
        TestUtils.runGorPipeCount("gor ../tests/data/external/samtools/test.vcf  | top 1 | write -noheader " + tmpfile);
        Assert.assertEquals("chr20\t14370\trs6054257\tG\tA\t29\tPASS\tNS=3;DP=14;AF=0.5;DB;H2\tGT:GQ:DP:HQ\t0|0:48:1:51,51\t1|0:48:8:51,51\t1/1:43:5:.,.\n",
                FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
    }

    @Test
    public void testWriteNoHeaderTsv() throws IOException {
        Path tmpfile = Files.createTempFile(tmpdir, "data", ".tsv").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | top 1 | write -noheader " + tmpfile);
        Assert.assertEquals("C\tCC\trs367896724\n", FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
    }

    @Test
    public void testWriteNoHeaderInternalExt() throws IOException {
        try {
            Path tmpfile = Files.createTempFile(tmpdir, "data", ".gor");
            TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for gor");
        } catch (GorParsingException pe) {
        }

        try {
            Path tmpfile = Files.createTempFile(tmpdir, "data", ".gorz");
            TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for gorz");
        } catch (GorParsingException pe) {
        }

        try {
            Path tmpfile = Files.createTempFile(tmpdir, "data", ".nor");
            TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for nor");
        } catch (GorParsingException pe) {
        }

        try {
            Path tmpfile = Files.createTempFile(tmpdir, "data", ".norz");
            TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for norz");
        } catch (GorParsingException pe) {
        }
    }

    @Test
    public void testGorWriteStandardOut() throws IOException {
        // Not sure what this test is doing with stdout!
        final PrintStream stdout = System.out;
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(1024 * 1024);
        try {
            System.setOut(new PrintStream(bout));

            Path tmpfile = Files.createTempFile(tmpdir, "data", ".gor").toAbsolutePath();;
            String query = "gor ../tests/data/gor/genes.gorz | top 3 | write " + tmpfile;
            String queryResult = TestUtils.runGorPipe("gor ../tests/data/gor/genes.gorz | top 3");

            TestUtils.runGorPipeIteratorOnMain(query);
            Assert.assertEquals("Unexpected results written to file!",
                    "#" + queryResult, FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
        } finally {
            System.setOut(stdout); // Replace with actual system out
        }
    }

    @Test
    public void testForkWrite() throws IOException {
        final Path tmpDir = Files.createTempDirectory("forkWrite");
        try {
            final String path = tmpDir.toAbsolutePath() + "/dbsnpWithForkCol_test.gor";
            createForkWriteTestFiles(path);
            TestUtils.runGorPipe("gor " + path + " | write -r -f forkcol " + tmpDir.toAbsolutePath() + "/dbsnp#{fork}_test.gor");
            TestUtils.assertTwoGorpipeResults("gor ../tests/data/gor/dbsnp_test.gor", "gor " + tmpDir.toAbsolutePath() + "/dbsnp117_test.gor");
        } finally {
            FileUtils.deleteDirectory(tmpDir.toFile());
        }
    }

    private void createForkWriteTestFiles(String path) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        final BufferedReader br = new BufferedReader(new FileReader("../tests/data/gor/dbsnp_test.gor"));
        String line;
        boolean isHeader = true;
        int colNum = -1;
        String[] newCols = null;
        while ((line = br.readLine()) != null) {
            final String[] cols = line.split("\t");
            if (isHeader) {
                newCols = new String[cols.length + 1];
                colNum = (int) (2 + (Math.random() * (cols.length - 2)));
                System.arraycopy(cols, 0, newCols, 0, colNum);
                newCols[colNum] = "forkcol";
                System.arraycopy(cols, colNum, newCols, colNum + 1, newCols.length - colNum - 1);
                bw.write(String.join("\t", newCols) + "\n");
                isHeader = false;
            } else {
                System.arraycopy(cols, 0, newCols, 0, colNum);
                newCols[colNum] = "117";
                System.arraycopy(cols, colNum, newCols, colNum + 1, newCols.length - colNum - 1);
                bw.write(String.join("\t", newCols) + "\n");
            }
        }
        bw.close();
        br.close();
    }

    @Test
    public void testForkWriteWithTAgs() throws IOException {
        final Path tmpDir = Files.createTempDirectory("forkWriteTags");
        try {
            final String path = tmpDir.toAbsolutePath() + "/dbsnpWithForkCol_test.gor";
            createForkWriteTestFiles(path);
            Path pathFoo = tmpDir.resolve("dbsnpfoo_test.gor");
            Path pathBar = tmpDir.resolve("dbsnpbar_test.gor");
            Path path117 = tmpDir.resolve("dbsnp117_test.gor");
            TestUtils.runGorPipe("gor " + path + " | write -r -f forkcol -t 'foo,bar,117' " + tmpDir.toAbsolutePath() + "/dbsnp#{fork}_test.gor");
            Assert.assertTrue(Files.exists(pathFoo));
            Assert.assertTrue(Files.size(pathFoo) < 100);
            Assert.assertTrue(Files.exists(pathBar));
            Assert.assertTrue(Files.size(pathBar) < 100);
            Assert.assertTrue(Files.exists(path117));
            Assert.assertTrue(Files.size(path117) > 100);
        } finally {
            FileUtils.deleteDirectory(tmpDir.toFile());
        }
    }

    @Test
    public void testForkWriteWithTagsWithZeroRowInputCreatesFile() {
        final String outputPath = tmpdir.toAbsolutePath().toString();
        String query = String.format("gorrow chr1,1,1 | calc xfile 0 | top 0 | write -t '0' -f xfile -r %s/data_#{fork}.gorz", outputPath);
        TestUtils.runGorPipe(query);
        Assert.assertTrue(Files.exists(tmpdir.resolve("data_0.gorz")));
    }

    @Test
    @Ignore("Stop using write as result file indicator")
    public void testForkWriteWithCreate() {
        String query = "create xxx = gorrows -p chr1:1-10 | rownum | replace rownum mod(rownum,2) | write -t '0' -f rownum -r data_#{fork}.gorz; gor [xxx]/data_0.gorz | top 1";
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result in fork file","chrom\tpos\nchr1\t2\n",result);
    }

    @Test
    @Ignore("Stop using write as result file indicator")
    public void testForkWriteFolderWithCreate() {
        final Path outputPath = tmpdir.toAbsolutePath().resolve("my.gorz");
        String query = String.format("create xxx = gorrows -p chr1:1-10 | rownum | replace rownum mod(rownum,2) | write -t '0' -f rownum -r %s/data_#{fork}.gorz; gor [xxx]/data_0.gorz | top 1", outputPath);
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result in fork file","chrom\tpos\nchr1\t2\n",result);
    }

    @Test
    @Ignore("Stop using write as result file indicator")
    public void testForkWriteSubfolderWithCreate() {
        final Path outputPath = tmpdir.toAbsolutePath().resolve("my.gorz");
        String query = String.format("create xxx = gorrows -p chr1:1-10 | rownum | replace rownum mod(rownum,2) | write -t '0' -f rownum -r %s/rownum=#{fork}/data.gorz; gor [xxx]/rownum=0/data.gorz | top 1", outputPath);
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result in fork file","chrom\tpos\nchr1\t2\n",result);
    }

    @Test
    public void testSeekToFreshGorzFile() {
        final Path tmpFile = tmpdir.resolve("dbsnp_test.gorz");
        tmpFile.toFile().deleteOnExit();
        final String path = tmpFile.toAbsolutePath().normalize().toString();
        TestUtils.runGorPipe("gor ../tests/data/gor/dbsnp_test.gor | write " + path);
        boolean success = true;
        try {
            success &= Files.lines(new File("../tests/data/gor/dbsnp_test.gor").toPath()).skip(1).allMatch(line -> {
                final String[] lineSplit = line.split("\t");
                final String pos = lineSplit[0] + ":" + lineSplit[1];
                final String tmp = TestUtils.runGorPipe("gor -p " + pos + " " + path);
                final String line2 = tmp.substring(tmp.indexOf('\n') + 1, tmp.length() - 1);
                return line.equals(line2);
            });
        } catch (Exception e) {
            success = false;
        }
        Assert.assertTrue("Could not seek to every position.", success);
    }

    @Test
    public void testBinaryWrite() throws IOException {
        var pathstr = "../tests/data/gor/dbsnp_test.gorz";
        var path = Paths.get(pathstr);
        var filename = path.getFileName();
        var userpath = Paths.get("user");
        var user = tmpdir.resolve(userpath);
        Files.createDirectory(user);
        var pgen = userpath.resolve("test.pgen");
        var dest = tmpdir.resolve(filename);
        Files.copy(path, dest);
        var destrel = tmpdir.relativize(dest);
        var query = "gor "+destrel+" | rename reference ref | calc alt ref | calc values '0101' | binarywrite "+pgen;
        TestUtils.runGorPipe(query,"-gorroot",tmpdir.toString());
    }

    @Test
    public void testWriteInfer() throws IOException {
        var query = "gorrows -p chr1:1-3 | calc u 'hello' | calc m 1 | write test.gorz";
        TestUtils.runGorPipe(query,"-gorroot",tmpdir.toString());
        var oschema = Files.readAllLines(tmpdir.resolve("test.gorz.meta")).stream().filter(p -> p.startsWith("## SCHEMA")).map(p -> p.substring(12).trim()).findFirst();
        Assert.assertTrue(oschema.isPresent());
        var schema = oschema.get();
        Assert.assertEquals("S,I,S,I",schema);
    }

    static boolean assertIndexFileIsCorrect(final String filePath) throws IOException {
        final String idxFilePath = filePath + ".gori";

        final Iterator<String> indexLineStream = new BufferedReader(new FileReader(idxFilePath)).lines().iterator();
        final Iterator<String> fileLineStream = new BufferedReader(new FileReader(filePath)).lines().iterator();

        long offsetInFile = fileLineStream.next().length() + 1;

        while (indexLineStream.hasNext()) {
            final String idxLine = indexLineStream.next();
            if (idxLine.startsWith("##")) {
                continue;
            }

            int idx = idxLine.indexOf('\t');
            final String wantedChr = idxLine.substring(0, idx);
            final int wantedPos = Integer.parseInt(idxLine.substring(idx + 1, idx = idxLine.indexOf('\t', idx + 1)));
            final long wantedOffset = Long.parseLong(idxLine.substring(idx + 1));

            boolean matched = false;
            while (fileLineStream.hasNext() && !matched) {
                final String line = fileLineStream.next();
                offsetInFile += line.length() + 1;

                final String chr = line.substring(0, idx = line.indexOf('\t'));
                final int pos = Integer.parseInt(line.substring(idx + 1, line.indexOf('\t', idx + 1)));

                if (chr.compareTo(wantedChr) > 0 || (chr.equals(wantedChr) && pos > wantedPos)) {
                    break;
                } else if (chr.equals(wantedChr) && pos == wantedPos && offsetInFile == wantedOffset) {
                    matched = true;
                }
            }
            if (!matched) return false;
        }
        return true;
    }
}
