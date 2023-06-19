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

import org.gorpipe.gor.model.GorOptions;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.gorpipe.gor.model.GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME;

public class UTestGorWriteFolder {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;
    private Path cachePath;

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        cachePath = workDirPath.resolve("result_cache");
        Files.createDirectory(cachePath);

        var genespath = Path.of("../tests/data/gor/genes.gor");
        Files.copy(genespath, workDirPath.resolve("genes.gor"));
    }

    @Test
    public void testWriteIntoGordFolder() throws IOException {
        final Path gordPath = workDirPath.toAbsolutePath().resolve("test.gord");
        Files.createDirectory(gordPath);

        String query = String.format("gorrow 1,2,3 | write %s", gordPath);
        String result = TestUtils.runGorPipe(query);

        Assert.assertFalse(Files.exists(gordPath.resolve(GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME)));
        List<Path> folderGorz = Files.list(gordPath).filter(p -> p.toString().endsWith(".gorz")).collect(Collectors.toList());
        TestUtils.assertGorpipeResults("chrom\tbpStart\tbpStop\nchr1\t2\t3\n", "gor " + folderGorz.get(0));
    }

    @Test
    public void testWriteIntoGordFolderDirect() throws IOException {
        final Path gordPath = workDirPath.toAbsolutePath().resolve("test.gord");
        final Path gorfilePath = gordPath.resolve("my.gor");
        Files.createDirectory(gordPath);

        String query = String.format("gorrow 1,2,3 | write %s", gorfilePath);
        String result = TestUtils.runGorPipe(query);

        Assert.assertFalse(Files.exists(gordPath.getParent().resolve(GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME)));
        Assert.assertEquals("#chrom\tbpStart\tbpStop\nchr1\t2\t3\n", Files.readString(gorfilePath));
    }

    @Test
    public void testPgorWriteEmptyOutput() {
        int count = TestUtils.runGorPipeCount("pgor genes.gor | top 1 | write test.gord", workDirPath.toAbsolutePath().toString());
        Assert.assertEquals("Pgor with write should return empty result", 0, count);
    }

    @Test
    public void testPgorWriteGordFolder() {
        var folderpath = workDirPath.resolve("folder.gord");

        TestUtils.runGorPipe("pgor genes.gor | top 1 | write " + folderpath,
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        String results = TestUtils.runGorPipe("gor " + folderpath,
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        Assert.assertEquals(UTestGorWriteExplicit.WRONG_RESULT, UTestGorWriteExplicit.GENE_PGOR_TOP1, results);

        Assert.assertEquals("Nor-ing the folder with -asdict should be the same as noring the dict",
                TestUtils.runGorPipe("nor -asdict " + folderpath.resolve(DEFAULT_FOLDER_DICTIONARY_NAME)),
                TestUtils.runGorPipe("nor -asdict " + folderpath));
    }

    @Test
    public void testPgorForkWrite() {
        var folderpath = workDirPath.resolve("folder.gord");

        TestUtils.runGorPipe("pgor genes.gor | rownum | calc mod mod(rownum,2) | hide rownum | write -f mod " + folderpath + "/file_#{CHROM}_#{BPSTART}_#{BPSTOP}_#{fork}.gorz;",
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir",cachePath.toString());

        String results = TestUtils.runGorPipe(
                String.format("gor %s | group chrom -count", folderpath),
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir",cachePath.toString());
        Assert.assertEquals(UTestGorWriteExplicit.WRONG_RESULT, UTestGorWriteExplicit.GENE_GROUP_CHROM, results);
    }

    @Test
    public void testPgorWriteOverGordFolder() throws IOException {
        var folderpath = workDirPath.resolve("folder.gord");

        // Fill the folder with different data.
        TestUtils.runGorPipe("pgor genes.gor | skip 1 | top 1 | write " + folderpath,
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        // Overwrite
        TestUtils.runGorPipe("pgor genes.gor | top 1 | write " + folderpath,
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        String results = TestUtils.runGorPipe("gor " + folderpath.toString(),
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());
        Assert.assertEquals(UTestGorWriteExplicit.WRONG_RESULT, UTestGorWriteExplicit.GENE_PGOR_TOP1, results);
    }

    @Test
    public void testParallelWriteOverGordFolder() throws IOException {
        var folderpath = workDirPath.resolve("folder.gord");

        // Fill the folder with different data.
        TestUtils.runGorPipe("parallel -parts <(norrows 2) <(gor genes.gor | rownum | where #{col:RowNum} + 3 = rownum | hide rownum) | write " + folderpath,
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        // Overwrite
        TestUtils.runGorPipe("parallel -parts <(norrows 2) <(gor genes.gor | rownum | where #{col:RowNum} + 1 = rownum | hide rownum) | write " + folderpath,
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        String results = TestUtils.runGorPipe("gor " + folderpath.toString(),
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());
        Assert.assertEquals(UTestGorWriteExplicit.WRONG_RESULT, "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                "chr1\t11868\t14412\tDDX11L1\n" +
                "chr1\t14362\t29806\tWASH7P\n", results);
    }

    @Test
    public void testPartgorWriteOverGordFolder() throws IOException {
        String dictContent =  "#col1\tcol2\tcol3\tcol4\tcol5\tcol6\tlis_PN\n" +
                "data/bucket_1.gorz\tbucket_1\tchr1\t0\tchrZ\t1000000000\tPN1\n" +
                "data/bucket_2.gorz\tbucket_2\tchr1\t0\tchrZ\t1000000000\tPN2\n";
        Path dictPath = workDirPath.resolve("dict.gord");
        Files.writeString(dictPath, dictContent);

        var folderpath = workDirPath.resolve("folder.gord");

        // Fill the folder with different data.
        TestUtils.runGorPipe(String.format("partgor -dict %s -f PN2 <(gorrow chr1,1,1| calc x \"#{tags:q}\") | write %s", dictPath, folderpath),
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        // Overwrite
        TestUtils.runGorPipe(String.format("partgor -dict %s -f PN1 <(gorrow chr1,1,1| calc x \"#{tags:q}\") | write %s", dictPath, folderpath),
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());

        String results = TestUtils.runGorPipe("gor " + folderpath.toString(),
                "-gorroot", workDirPath.toAbsolutePath().toString(),
                "-cachedir", cachePath.toString());
        Assert.assertEquals(UTestGorWriteExplicit.WRONG_RESULT, "chrom\tbpStart\tbpStop\tx\n" +
                "chr1\t1\t1\t'PN1'\n" , results);
    }
}
