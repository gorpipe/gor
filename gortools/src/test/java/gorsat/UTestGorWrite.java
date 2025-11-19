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
import org.gorpipe.exceptions.GorSecurityException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.driver.linkfile.LinkFileMeta;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.model.BaseMeta;
import org.gorpipe.gor.util.DataUtil;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static org.gorpipe.gor.driver.meta.DataType.GORI;

/**
 * Created by hjalti on 13/06/17.
 */
public class UTestGorWrite {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Rule
    public TemporaryFolder tempRoot = new TemporaryFolder();
    private Path tempRootPath;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    private String defaultV1LinkFileHeader;
    private String testdbsnpTestLine1 = """
            Chrom\tPOS\treference\tallele\tdifferentrsIDs
            chr1\t10179\tC\tCC\trs367896724
            """;

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        Files.createDirectories(workDirPath.resolve("result_cache"));
        tempRootPath = tempRoot.getRoot().toPath();

        var meta = new LinkFileMeta();
        meta.setProperty(BaseMeta.HEADER_VERSION_KEY, "1");
        defaultV1LinkFileHeader = meta.formatHeader();
    }

    @Test
    public void testWritePathWithVersionedLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());

        Assert.assertTrue( Files.readString(workDirPath.resolve("dbsnp3.gor.link")).startsWith(
                defaultV1LinkFileHeader +  workDirPath.resolve("dbsnp2.gor") + "\t"));

        String linkresult = TestUtils.runGorPipe("gor dbsnp3.gor | top 1", "-gorroot", workDirPath.toString());
        Assert.assertEquals(testdbsnpTestLine1, linkresult);
    }

    @Test
    public void testWritePathWithServerVersionedLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));
        TestUtils.runGorPipe(new String[] {"gor dbsnp.gor | write user_data/dbsnp2.gor -link user_data/dbsnp3.gor", "-gorroot", workDirPath.toString()}, true);

        Assert.assertTrue( Files.readString(workDirPath.resolve("user_data").resolve(DataUtil.toLinkFile("dbsnp3", DataType.GOR))).startsWith(
                defaultV1LinkFileHeader +  workDirPath.resolve("user_data").resolve(DataUtil.toFile("dbsnp2", DataType.GOR)).toString() + "\t"));

        String linkresult = TestUtils.runGorPipe("gor user_data/dbsnp3.gor | top 1", "-gorroot", workDirPath.toString());
        Assert.assertEquals(testdbsnpTestLine1, linkresult);
    }

    @Test
    public void testWritePathWithUnAuthorizedServerLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));

        Assert.assertThrows( "Writing link to un-writable project location, throws exception",
                GorSecurityException.class,
                () -> TestUtils.runGorPipe(new String[]{"gor dbsnp.gor | write user_data/dbsnp2.gor -link /tmp/dbsnp3.gor",
                        "-gorroot", workDirPath.toString()}, true));

    }

    @Test(expected = Exception.class)
    public void testWritePathWithInaccessiableLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link s3://bacbucket/dbsnp3.gor", "-gorroot", workDirPath.toString());
    }

    @Test
    public void testWritePathWithExistingLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));
        Files.writeString(workDirPath.resolve("dbsnp3.gor.link"), workDirPath.resolve("dbsnp.gor").toString() + "\n");
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());
        var linkUrl = LinkFile.load(new FileSource(workDirPath.resolve("dbsnp3.gor.link"))).getLatestEntryUrl();
        Assert.assertEquals(workDirPath.resolve("dbsnp2.gor").toString(), linkUrl);
    }

    @Test
    public void testWritePathWithExistingVersionedLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));
        Files.writeString(workDirPath.resolve("dbsnp3.gor.link"), defaultV1LinkFileHeader + workDirPath.resolve("dbsnp.gor").toString() + "\n");
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());

        Assert.assertTrue(Files.readString(workDirPath.resolve("dbsnp3.gor.link")).startsWith(
                defaultV1LinkFileHeader
                        + workDirPath.resolve("dbsnp.gor") + "\t1970-01-01T00:00:00Z\t\t0\t\n"
                        + workDirPath.resolve("dbsnp2.gor") + "\t"));
    }

    @Test
    public void testWritePathWithExistingBadLinkFile() throws IOException {
        Path link = workDirPath.resolve("dbsnp3.gor.link");
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gor"), workDirPath.resolve("dbsnp.gor"));
        Files.writeString(link, "");
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());
        var linkUrl = LinkFile.load(new FileSource(link)).getLatestEntryUrl();
        Assert.assertEquals(workDirPath.resolve("dbsnp2.gor").toString(), linkUrl);
    }

    @Test
    public void testWritePathWithExistingBadVersionedLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));
        Files.writeString(workDirPath.resolve("dbsnp3.gor.link"), "");
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());

        Assert.assertTrue(Files.readString(workDirPath.resolve("dbsnp3.gor.link")).startsWith(
                defaultV1LinkFileHeader + workDirPath.resolve("dbsnp2.gor") + "\t"));
    }

    @Test
    public void testOverwritePathWithExistingVersionedLinkFile() throws IOException {
        Path p = Paths.get("../tests/data/gor/dbsnp_test.gor");
        Files.copy(p, workDirPath.resolve("dbsnp.gor"));

        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());
        LinkFile linkFile = LinkFile.load(new FileSource(workDirPath.resolve("dbsnp3.gor.link").toString()));
        Assert.assertEquals(1, linkFile.getEntriesCount());

        // Test with same file.
        TestUtils.runGorPipe("gor dbsnp.gor | write dbsnp2.gor -link dbsnp3.gor", "-gorroot", workDirPath.toString());
        linkFile = LinkFile.load(new FileSource(workDirPath.resolve("dbsnp3.gor.link").toString()));
        Assert.assertEquals(2, linkFile.getEntriesCount());

        // Test with different file
        Assert.assertThrows( "Overwriting link with same path, throws exception",
                GorSystemException.class,
                () -> TestUtils.runGorPipe("gor dbsnp.gor | top 1 | write dbsnp2.gor -link dbsnp3.gor",
                        "-gorroot", workDirPath.toString()));
    }

    @Test
    public void testWriteLinkFileAndMeta() throws IOException {
        TestUtils.runGorPipe("gorrow chr1,1,100 | write test.gor -link ltest.gor -linkmeta 'TEST1=T1,TEST2=T2, TEST3=T3'", "-gorroot", workDirPath.toString());

        String linkresult = TestUtils.runGorPipe("gor ltest.gor | top 1", "-gorroot", workDirPath.toString());
        Assert.assertEquals("chrom\tbpStart\tbpStop\nchr1\t1\t100\n", linkresult);

        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("ltest.gor.link").toString()));
        Assert.assertEquals(1, linkFile.getEntriesCount());
        Assert.assertEquals("T1", linkFile.getMeta().getProperty("TEST1"));
        Assert.assertEquals("T2", linkFile.getMeta().getProperty("TEST2"));
        Assert.assertEquals("T3", linkFile.getMeta().getProperty("TEST3"));
    }

    @Test
    public void testWriteLinkFileAndUnqotedMeta() throws IOException {
        TestUtils.runGorPipe("gorrow chr1,1,100 | write test.gor -link ltest.gor -linkmeta TEST1=T1,TEST2=T2", "-gorroot", workDirPath.toString());

        String linkresult = TestUtils.runGorPipe("gor ltest.gor | top 1", "-gorroot", workDirPath.toString());
        Assert.assertEquals("chrom\tbpStart\tbpStop\nchr1\t1\t100\n", linkresult);

        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("ltest.gor.link").toString()));
        Assert.assertEquals(1, linkFile.getEntriesCount());
        Assert.assertEquals("T1", linkFile.getMeta().getProperty("TEST1"));
        Assert.assertEquals("T2", linkFile.getMeta().getProperty("TEST2"));
    }

    @Test
    public void testWriteLinkFileAndMetaWithInfo() throws IOException {
        TestUtils.runGorPipe("gorrow chr1,1,100 | write test.gor -link ltest.gor -linkmeta TEST1=T1,ENTRY_INFO='Some file info'", "-gorroot", workDirPath.toString());

        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("ltest.gor.link").toString()));
        Assert.assertEquals(1, linkFile.getEntriesCount());
        Assert.assertEquals("T1", linkFile.getMeta().getProperty("TEST1"));
        Assert.assertEquals("NOTFOUND", linkFile.getMeta().getProperty("ENTRY_INFO", "NOTFOUND"));
        Assert.assertEquals("Some file info", linkFile.getLatestEntry().info());
    }

    @Test
    public void testWriteLinkFileWithInferFileName() throws IOException {

        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_FILES_URL, workDirPath.resolve("managed_data").toString());
        TestUtils.runGorPipe("gorrow chr1,1,100 | write -link ltest.gor", "-gorroot", workDirPath.toString());

        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("ltest.gor.link").toString()));

        Assert.assertEquals(1, linkFile.getEntriesCount());
        Assert.assertTrue(linkFile.getLatestEntry().url().startsWith(workDirPath.resolve("managed_data/ltest").toString()));
        Assert.assertTrue(linkFile.getLatestEntry().url().endsWith(".gor"));
        Assert.assertTrue(Files.exists(Path.of(linkFile.getLatestEntry().url())));
        Assert.assertEquals("#chrom\tbpStart\tbpStop\nchr1\t1\t100\n",
                Files.readString(Path.of(linkFile.getLatestEntry().url())));

    }

    @Test
    public void testTxtWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, workDirPath.resolve("simple1.nor"));
        String[] args = {"nor simple1.nor | select Chrom | calc p 1 | write test/new.txt", "-gorroot", workDirPath.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Ignore
    @Test
    public void testTxtFolderWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, workDirPath.resolve("simple1.nor"));
        String[] args = {"nor simple1.nor | select Chrom | calc p 1 | write -d test new.txt", "-gorroot", workDirPath.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Test
    public void testFolderWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, workDirPath.resolve("simple1.nor"));
        String[] args = {"nor simple1.nor | select Chrom | calc p 1 | write -d test", "-gorroot", workDirPath.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Test
    public void testTsvWriteServer() throws IOException {
        Path p = Paths.get("../tests/data/nor/simple.nor");
        Files.copy(p, workDirPath.resolve("simple2.nor"));
        String[] args = {"nor simple2.nor | select Chrom | calc p 1 | write test/new.tsv", "-gorroot", workDirPath.toAbsolutePath().toString()};
        TestUtils.runGorPipeCount(args, true);
    }

    @Test
    public void testGorWriteWithMd5() throws IOException {
        Path tmpfile = workDirPath.resolve("genes_md5.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor ../tests/data/gor/genes.gorz | write -m " + tmpfile.toAbsolutePath().normalize();

        TestUtils.runGorPipeCount(query);

        Path path = workDirPath.resolve("genes_md5.gorz.md5");
        Assert.assertTrue("Md5 file does not exist", Files.exists(path));
        path.toFile().deleteOnExit();
        String md5str = new String(Files.readAllBytes(path));
        Assert.assertEquals("Not a valid md5 string", 32, md5str.length());
    }

    @Test
    public void testWriteGorWithMd5AndRelPaths() throws IOException {
        // first gor command uses standard context to write a nor file into temp root
        Path tmpfile = tempRootPath.resolve("genes_md5.gorz");
        String query = "gor ../tests/data/gor/genes.gorz | write -m " + tmpfile.toAbsolutePath().normalize();
        TestUtils.runGorPipe(query, false);
        List<String> md5Content = Files.readAllLines(Path.of(tmpfile.toAbsolutePath() + ".md5"));
        Assert.assertEquals(1, md5Content.size());
        Assert.assertEquals(32, md5Content.get(0).length());

        // second gor command rewrites it to a new file, setting the tempRoot as root of the context
        // use relative path for files to exercise correct file path resolution in engine
        Path subdir = Files.createTempDirectory(tempRootPath, "dat");
        Path tmpfile2 = subdir.resolve("genes_md5.gorz").toAbsolutePath();
        Path tmpfile2rel = Path.of(subdir.getFileName().toString(), tmpfile2.getFileName().toString());
        String query1 = "gor " + tmpfile.getFileName() + " | write -m " + tmpfile2rel;
        TestUtils.runGorPipe(query1, tempRootPath.toString(), false);
        List<String> md5Content2 = Files.readAllLines(Path.of(tmpfile2.toAbsolutePath() + ".md5"));
        Assert.assertEquals(1, md5Content2.size());
        Assert.assertEquals(32, md5Content2.get(0).length());
    }


    @Test
    public void testGorWriteColumnNumber() {
        Path tmpfile = workDirPath.resolve("genes_md5.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor ../tests/data/gor/genes.gorz | write -m " + tmpfile.toAbsolutePath().normalize();
        String headerRes = TestUtils.runGorPipe(query);
        Assert.assertEquals("", "Chrom\tgene_start\tgene_end\tGene_Symbol\n", headerRes);
    }

    @Test
    public void testGorzWriteCompressionLevel() throws IOException {
        Path tmpfile = workDirPath.resolve("genes.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor ../tests/data/gor/genes.gor | write -l 9 " + tmpfile.toAbsolutePath().normalize();

        TestUtils.runGorPipeCount(query);

        Path originalGenesGorz = Paths.get("../tests/data/gor/genes.gorz");
        Assert.assertNotEquals("Files with different compression level are of same size", Files.size(tmpfile), Files.size(originalGenesGorz));
    }

    @Test
    @Ignore("This test is useful for load testing and performance analysis. Does not work without code change unless hekla or another proper csa volume is mounted at /mnt/csa")
    public void testGorWriteLargeFileWithMd5() throws IOException {
        Path tmpfile = workDirPath.resolve("large_md5.gorz");
        tmpfile.toFile().deleteOnExit();
        String query = "gor /mnt/csa/env/dev/projects/installation_test_project/ref/dbsnp.gorz | write -m " + tmpfile.toAbsolutePath().normalize().toString();

        TestUtils.runGorPipeCount(query);

        Path path = workDirPath.resolve("large_md5.gorz.md5");
        Assert.assertTrue("Md5 file does not exist", Files.exists(path));
        path.toFile().deleteOnExit();
        String md5str = new String(Files.readAllBytes(path));
        Assert.assertEquals("Not a valid md5 string", 32, md5str.length());
    }

    @Test
    public void testGorWriteReadWithIdx() throws IOException {
        Path tmpfile = workDirPath.resolve("genes.gorz");
        tmpfile.toFile().deleteOnExit();
        final String tmpFilePath = tmpfile.toAbsolutePath().normalize().toString();
        var genesPath = Path.of("../tests/data/gor/genes.gorz").toAbsolutePath();
        TestUtils.runGorPipeCount("gor "+ genesPath +" | write -i CHROM " + tmpFilePath,"-gorroot", workDirPath.toString());

        Path path = workDirPath.resolve("genes.gorz.gori");
        Assert.assertTrue( "Seek index file does not exist", Files.exists(path) );
        path.toFile().deleteOnExit();

        Assert.assertTrue("Index file for " + tmpFilePath + " is incorrect.", assertIndexFileIsCorrect(tmpFilePath));

        final int count = TestUtils.runGorPipeCount("gor -p chr22 " + tmpfile.toAbsolutePath().normalize());

        Assert.assertEquals("Wrong number of lines in seekindexed gorz file", 1127, count);
        var query1 = new String[] {"pgor "+ tmpfile.toAbsolutePath().normalize() +"|group chrom -count | signature -timeres 1","-gorroot", workDirPath.toString(),"-cachedir","result_cache"};
        var query2 = new String[] {"pgor "+genesPath+"|group chrom -count","-gorroot", workDirPath.toString(),"-cachedir","result_cache"};
        TestUtils.assertTwoGorpipeResults("Pgor on indexed gorz file returns different results than on unindexed one", query1, query2);
    }

    @Test
    public void testGorWriteReadWithFullIdx() throws IOException {
        Path tmpfile = workDirPath.resolve("genes.gorz");
        tmpfile.toFile().deleteOnExit();
        final String tmpFilePath = tmpfile.toAbsolutePath().normalize().toString();
        var genesPath = Path.of("../tests/data/gor/genes.gorz").toAbsolutePath();
        TestUtils.runGorPipeCount("gor "+genesPath+" | write -i FULL " + tmpFilePath,"-gorroot", workDirPath.toString()); //Create index file.

        Assert.assertTrue("Index file for " + tmpFilePath + " is incorrect.", assertIndexFileIsCorrect(tmpFilePath));

        Path path = workDirPath.resolve("genes.gorz.gori");
        Assert.assertTrue( "Seek index file does not exist", Files.exists(path) );
        path.toFile().deleteOnExit();

        final int count = TestUtils.runGorPipeCount("gor -p chr22 "+ tmpfile.toAbsolutePath().normalize());
        Assert.assertEquals("Wrong number of lines in seekindexed gorz file", 1127, count);
        var query1 = new String[] {"pgor "+ tmpfile.toAbsolutePath().normalize() +"|group chrom -count","-gorroot", workDirPath.toString(),"-cachedir","result_cache"};
        var query2 = new String[] {"pgor "+genesPath+"|group chrom -count","-gorroot", workDirPath.toString(),"-cachedir","result_cache"};
        TestUtils.assertTwoGorpipeResults("Pgor on indexed gorz file returns different results than on unindexed one", query1, query2);
    }

    @Test
    public void testWriteGor() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".gor").toAbsolutePath();
        TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("gor " + tmpfile, "gor ../tests/data/gor/genes.gorz");
        Assert.assertEquals("#Chrom	gene_start	gene_end	Gene_Symbol", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteNor() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".nor").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteNorWithMd5() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".nor").toAbsolutePath();
        TestUtils.runGorPipe("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write -m " + tmpfile, false);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        List<String> md5Content = Files.readAllLines(Path.of(tmpfile.toAbsolutePath() + ".md5"));
        Assert.assertEquals(1, md5Content.size());
        Assert.assertEquals(32, md5Content.get(0).length());
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteNorWithMd5AndRelPaths() throws IOException {
        // first gor command uses standard context to write a nor file into temp root
        Path tmpfile = Files.createTempFile(tempRootPath, "data", ".nor").toAbsolutePath();
        TestUtils.runGorPipe("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write -m " + tmpfile, false);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        List<String> md5Content = Files.readAllLines(Path.of(tmpfile.toAbsolutePath() + ".md5"));
        Assert.assertEquals(1, md5Content.size());
        Assert.assertEquals(32, md5Content.get(0).length());
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));

        // second gor command rewrites it to a new file, setting the tempRoot as root of the context
        // use relative path for files to exercise correct file path resolution in engine
        Path subdir = Files.createTempDirectory(tempRootPath, "dat");
        Path tmpfile2 = Files.createTempFile(subdir, "dataNew", ".nor").toAbsolutePath();
        Path tmpfile2rel = Path.of(subdir.getFileName().toString(), tmpfile2.getFileName().toString());
        TestUtils.runGorPipe("nor " + tmpfile.getFileName() + " | write -m " + tmpfile2rel,
                tempRootPath.toString(), false);
        List<String> md5Content2 = Files.readAllLines(Path.of(tmpfile2.toAbsolutePath() + ".md5"));
        Assert.assertEquals(1, md5Content2.size());
        Assert.assertEquals(32, md5Content2.get(0).length());
    }

    @Test
    @Ignore("Write uses chr")
    public void testWriteVcf() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".vcf").toAbsolutePath();
        TestUtils.runGorPipeCount("gor ../tests/data/external/samtools/test.vcf | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("gor " + tmpfile, "gor ../tests/data/external/samtools/test.vcf ");
        Assert.assertEquals("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	NA00001	NA00002	NA00003", Files.readAllLines(tmpfile).get(1));
    }

    @Test
    public void testWriteTsv() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".tsv").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteUnknownExt() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".non").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write " + tmpfile);
        TestUtils.assertTwoGorpipeResults("nor " + tmpfile, "nor ../tests/data/gor/dbsnp_test.gorz | select 3-");
        Assert.assertEquals("#reference\tallele\trsIDs", Files.readAllLines(tmpfile).get(0));
    }

    @Test
    public void testWriteNoHeaderUnknownExt() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".non").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/genes.gorz | top 1 | write -noheader " + tmpfile);
        Assert.assertEquals("chr1\t11868\t14412\tDDX11L1\n", FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
    }

    @Test
    public void testWriteNoHeaderVcf() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".vcf").toAbsolutePath();
        TestUtils.runGorPipeCount("gor ../tests/data/external/samtools/test.vcf  | top 1 | write -noheader " + tmpfile);
        Assert.assertEquals("chr20\t14370\trs6054257\tG\tA\t29\tPASS\tNS=3;DP=14;AF=0.5;DB;H2\tGT:GQ:DP:HQ\t0|0:48:1:51,51\t1|0:48:8:51,51\t1/1:43:5:.,.\n",
                FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
    }

    @Test
    public void testWriteNoHeaderTsv() throws IOException {
        Path tmpfile = Files.createTempFile(workDirPath, "data", ".tsv").toAbsolutePath();
        TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | top 1 | write -noheader " + tmpfile);
        Assert.assertEquals("C\tCC\trs367896724\n", FileUtils.readFileToString(tmpfile.toFile(), "utf8"));
    }

    @Test
    public void testWriteNoHeaderInternalExt() throws IOException {
        try {
            Path tmpfile = Files.createTempFile(workDirPath, "data", ".gor");
            TestUtils.runGorPipeCount("gor ../tests/data/gor/genes.gorz | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for gor");
        } catch (GorParsingException pe) {
        }

        try {
            Path tmpfile = Files.createTempFile(workDirPath, "data", ".gorz");
            TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for gorz");
        } catch (GorParsingException pe) {
        }

        try {
            Path tmpfile = Files.createTempFile(workDirPath, "data", ".nor");
            TestUtils.runGorPipeCount("nor ../tests/data/gor/dbsnp_test.gorz | select 3- | write -noheader "
                    + tmpfile.toAbsolutePath().normalize().toString());
            Assert.fail("Can not skip header for nor");
        } catch (GorParsingException pe) {
        }

        try {
            Path tmpfile = Files.createTempFile(workDirPath, "data", ".norz");
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

            Path tmpfile = Files.createTempFile(workDirPath, "data", ".gor").toAbsolutePath();;
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
        final String outputPath = workDirPath.toAbsolutePath().toString();
        String query = String.format("gorrow chr1,1,1 | calc xfile 0 | top 0 | write -t '0' -f xfile -r %s/data_#{fork}.gorz", outputPath);
        TestUtils.runGorPipe(query);
        Assert.assertTrue(Files.exists(workDirPath.resolve("data_0.gorz")));
    }

    @Test
    public void testForkWriteWithCreate() {
        String query = "create xxx = gorrows -p chr1:1-10 | signature -timeres 1 | rownum | replace rownum mod(rownum,2) | write -t '0' -f rownum -r data_#{fork}.gorz; gor data_0.gorz | top 1";
        String result = TestUtils.runGorPipe(query, workDirPath.toString(), false, null);
        Assert.assertEquals("Wrong result in fork file","chrom\tpos\nchr1\t2\n",result);
    }

    @Test
    public void testForkWriteFolderWithCreate() {
        final Path outputPath = workDirPath.toAbsolutePath().resolve("mystuff");
        String query = String.format("create xxx = gorrows -p chr1:1-10 | rownum | replace rownum mod(rownum,2) | write -t '0' -f rownum -r %s/data_#{fork}.gorz; gor %s/data_0.gorz | top 1", outputPath, outputPath);
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result in fork file","chrom\tpos\nchr1\t2\n",result);
    }

    @Test
    public void testForkWriteSubfolderWithCreate() {
        final Path outputPath = workDirPath.toAbsolutePath().resolve("mystuff");
        String query = String.format("create xxx = gorrows -p chr1:1-10 | rownum | replace rownum mod(rownum,2) | write -t '0' -f rownum -r %s/rownum=#{fork}/data.gorz; gor %s/rownum=0/data.gorz | top 1", outputPath, outputPath);
        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong result in fork file","chrom\tpos\nchr1\t2\n",result);
    }

    @Test
    public void testForkWriteWithParallel() {
        final Path outputPath = workDirPath.toAbsolutePath().resolve("mystuff");
        String query = String.format("create splits = norrows 3; parallel -parts [splits] <(gorrows -p chr1:1-100 | calc sp str(#{col:Rownum}) + '.gorz')  | write %s/#{fork} -f sp -r", outputPath);
        String result = TestUtils.runGorPipe(query);
        TestUtils.assertGorpipeResults("testForkWriteWithParallel", "chrom\tpos\nchr1\t1\n", String.format("gor %s | top 1", outputPath.resolve("0.gorz")));
    }

    @Test
    public void testForkWriteWithParallelCreate() {
        final Path outputPath = workDirPath.toAbsolutePath().resolve("mystuff");
        String query = String.format("create splits = norrows 3; create par = parallel -parts [splits] <(gorrows -p chr1:1-100 | calc sp str(#{col:Rownum}) + '.gorz')  | write %s/#{fork} -f sp -r; gor %s/0.gorz", outputPath, outputPath);
        String result = TestUtils.runGorPipe(query);
        TestUtils.assertGorpipeResults("testForkWriteWithParallel", "chrom\tpos\nchr1\t1\n", String.format("gor %s | top 1", outputPath.resolve("0.gorz")));
    }

    @Test
    public void testForkWriteDictWithParallel() {
        final Path outputPath = workDirPath.toAbsolutePath().resolve("my.gord");
        String query = String.format("create splits = norrows 3; parallel -parts [splits] <(gorrows -p chr1:1-100 | calc sp str(#{col:Rownum}) + '.gorz')  | write %s/#{fork} -f sp -r", outputPath);
        String result = TestUtils.runGorPipe(query);
        TestUtils.assertGorpipeResults("testForkWriteDictWithParallel file", "chrom\tpos\nchr1\t1\n", String.format("gor %s | top 1", outputPath.resolve("0.gorz")));
        TestUtils.assertGorpipeResults("testForkWriteDictWithParallel dict", "chrom\tpos\nchr1\t1\n", String.format("gor %s | top 1", outputPath));
    }

    @Test
    public void testSeekToFreshGorzFile() {
        final Path tmpFile = workDirPath.resolve("dbsnp_test.gorz");
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
        var user = workDirPath.resolve(userpath);
        Files.createDirectory(user);
        var pgen = userpath.resolve("test.pgen");
        var dest = workDirPath.resolve(filename);
        Files.copy(path, dest);
        var destrel = workDirPath.relativize(dest);
        var query = "gor "+destrel+" | rename reference ref | calc alt ref | calc values '0101' | binarywrite "+pgen;
        TestUtils.runGorPipe(query,"-gorroot", workDirPath.toString());
    }

    @Test
    public void testWriteInfer() throws IOException {
        var query = "gorrows -p chr1:1-3 | calc u 'hello' | calc m 1 | write test.gorz";
        TestUtils.runGorPipe(query,"-gorroot", workDirPath.toString());
        var oschema = Files.readAllLines(workDirPath.resolve("test.gorz.meta")).stream().filter(p -> p.startsWith("## SCHEMA")).map(p -> p.substring(12).trim()).findFirst();
        Assert.assertTrue(oschema.isPresent());
        var schema = oschema.get();
        Assert.assertEquals("S,I,S,I",schema);
    }

    @Test
    public void testWriteIntoSymlinkedFolder() throws IOException {
        Path outdir = workDirPath.resolve("out");
        Path link2outdir = workDirPath.resolve("outlink");
        Files.createDirectory(outdir);
        Files.createSymbolicLink(link2outdir, outdir);

        TestUtils.runGorPipe("gorrow chr1,1 | write outlink/b.gor", "-gorroot", workDirPath.toString());
        Assert.assertEquals("#chrom\tpos\n" +
                "chr1\t1\n", Files.readString(outdir.resolve("b.gor")));
    }

    static boolean assertIndexFileIsCorrect(final String filePath) throws IOException {
        final String idxFilePath = filePath + GORI.suffix;

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
