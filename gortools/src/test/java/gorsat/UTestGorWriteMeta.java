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

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.driver.linkfile.LinkFileUtil;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.GorMeta;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ENGKNOW-4236: reference build in WRITE output meta, the WRITE -meta option, and -linkmeta values with spaces.
 */
public class UTestGorWriteMeta {

    private static final int HG19_CHR1 = 249250621;
    private static final int HG38_CHR1 = 248956422;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
    }

    private String writeConfig(String name, int chr1Length, String buildVersion) throws IOException {
        Path buildSize = workDirPath.resolve(name + "_buildsize.gor");
        Files.writeString(buildSize, "chr1\t" + chr1Length + "\nchr2\t243199373\n");
        StringBuilder config = new StringBuilder("buildSizeFile\t" + buildSize + "\n");
        if (buildVersion != null) {
            config.append("buildVersion\t").append(buildVersion).append("\n");
        }
        Path configFile = workDirPath.resolve(name + "_config.txt");
        Files.writeString(configFile, config.toString());
        return configFile.toString();
    }

    private String run(String query, String config) {
        return config == null
                ? TestUtils.runGorPipe(query, "-gorroot", workDirPath.toString())
                : TestUtils.runGorPipe(query, "-gorroot", workDirPath.toString(), "-config", config);
    }

    private GorMeta meta(String file) {
        Path metaPath = workDirPath.resolve(file + ".meta");
        Assert.assertTrue("Missing meta file " + metaPath, Files.exists(metaPath));
        return GorMeta.createAndLoad(new DriverBackedFileReader(""), metaPath.toString());
    }

    // --- Reference build ---

    @Test
    public void testGorzMetaHasBuildHg19() throws IOException {
        String config = writeConfig("hg19", HG19_CHR1, null);
        run("gorrow chr1,1,100 | write test.gorz", config);
        Assert.assertEquals("hg19", meta("test.gorz").getProperty(GorMeta.HEADER_BUILD_KEY));
    }

    @Test
    public void testGorzMetaHasBuildHg38() throws IOException {
        String config = writeConfig("hg38", HG38_CHR1, null);
        run("gorrow chr1,1,100 | write test.gorz", config);
        Assert.assertEquals("hg38", meta("test.gorz").getProperty(GorMeta.HEADER_BUILD_KEY));
    }

    @Test
    public void testConfigBuildVersionOverridesInferredBuild() throws IOException {
        String config = writeConfig("override", HG19_CHR1, "GRCh37");
        run("gorrow chr1,1,100 | write test.gorz", config);
        Assert.assertEquals("GRCh37", meta("test.gorz").getProperty(GorMeta.HEADER_BUILD_KEY));
    }

    @Test
    public void testUnknownBuildWritesNoBuildKey() throws IOException {
        String config = writeConfig("unknown", 250000000, null);
        run("gorrow chr1,1,100 | write test.gorz", config);
        Assert.assertFalse(meta("test.gorz").containsProperty(GorMeta.HEADER_BUILD_KEY));
    }

    @Test
    public void testNoConfigWritesNoBuildKey() {
        run("gorrow chr1,1,100 | write test.gorz", null);
        Assert.assertFalse(meta("test.gorz").containsProperty(GorMeta.HEADER_BUILD_KEY));
    }

    @Test
    public void testGorWritesMetaSidecarWithBuild() throws IOException {
        String config = writeConfig("hg38", HG38_CHR1, null);
        run("gorrow chr1,1,100 | write test.gor", config);
        var meta = meta("test.gor");
        Assert.assertEquals("hg38", meta.getProperty(GorMeta.HEADER_BUILD_KEY));
        Assert.assertEquals(1, meta.getLineCount());
        Assert.assertEquals("#chrom\tbpStart\tbpStop\nchr1\t1\t100\n", Files.readString(workDirPath.resolve("test.gor")));
    }

    @Test
    public void testVcfWritesMetaSidecarAndReferenceHeader() throws IOException {
        String config = writeConfig("hg38", HG38_CHR1, null);
        run("gorrow chr1,1,100 | calc ID '.' | calc REF 'A' | calc ALT 'C' | select 1,2,ID,REF,ALT | write test.vcf", config);
        Assert.assertEquals("hg38", meta("test.vcf").getProperty(GorMeta.HEADER_BUILD_KEY));
        List<String> lines = Files.readAllLines(workDirPath.resolve("test.vcf"));
        Assert.assertEquals("##fileformat=VCFv4.2", lines.get(0));
        Assert.assertEquals("##reference=hg38", lines.get(1));
        Assert.assertTrue(lines.get(2).startsWith("#chrom"));
    }

    @Test
    public void testVcfWithoutKnownBuildHasNoReferenceHeader() throws IOException {
        run("gorrow chr1,1,100 | calc ID '.' | calc REF 'A' | calc ALT 'C' | select 1,2,ID,REF,ALT | write test.vcf", null);
        Assert.assertTrue(Files.readAllLines(workDirPath.resolve("test.vcf")).stream().noneMatch(l -> l.startsWith("##reference")));
    }

    @Test
    public void testNorzHasNoBuildKey() throws IOException {
        String config = writeConfig("hg38", HG38_CHR1, null);
        run("norrows 2 | write test.norz", config);
        Assert.assertFalse(meta("test.norz").containsProperty(GorMeta.HEADER_BUILD_KEY));
    }

    // --- -meta option ---

    @Test
    public void testMetaOptionOnGorz() {
        run("gorrow chr1,1,100 | write test.gorz -meta 'PROJECT=abc,OWNER=Jane Doe'", null);
        var meta = meta("test.gorz");
        Assert.assertEquals("abc", meta.getProperty("PROJECT"));
        Assert.assertEquals("Jane Doe", meta.getProperty("OWNER"));
    }

    @Test
    public void testMetaOptionOnGorNorzAndVcf() {
        run("gorrow chr1,1,100 | write test.gor -meta 'A=1'", null);
        run("norrows 2 | write test.norz -meta 'A=2'", null);
        run("gorrow chr1,1,100 | calc ID '.' | calc REF 'A' | calc ALT 'C' | select 1,2,ID,REF,ALT | write test.vcf -meta 'A=3'", null);
        Assert.assertEquals("1", meta("test.gor").getProperty("A"));
        Assert.assertEquals("2", meta("test.norz").getProperty("A"));
        Assert.assertEquals("3", meta("test.vcf").getProperty("A"));
    }

    @Test
    public void testMetaKeysAreUppercased() {
        run("gorrow chr1,1,100 | write test.gorz -meta 'project=abc'", null);
        Assert.assertEquals("abc", meta("test.gorz").getProperty("PROJECT"));
    }

    @Test
    public void testMetaBuildOverridesSessionBuild() throws IOException {
        String config = writeConfig("hg38", HG38_CHR1, null);
        run("gorrow chr1,1,100 | write test.gorz -meta 'BUILD=hg19'", config);
        Assert.assertEquals("hg19", meta("test.gorz").getProperty(GorMeta.HEADER_BUILD_KEY));
    }

    @Test
    public void testMetaCannotOverrideSystemKey() {
        Assert.assertThrows(GorParsingException.class,
                () -> run("gorrow chr1,1,100 | write test.gorz -meta 'MD5=abc'", null));
        Assert.assertThrows(GorParsingException.class,
                () -> run("gorrow chr1,1,100 | write test.gorz -meta 'line_count=5'", null));
    }

    @Test
    public void testMetaWithoutValueIsRejected() {
        Assert.assertThrows(GorParsingException.class,
                () -> run("gorrow chr1,1,100 | write test.gorz -meta 'PROJECT'", null));
    }

    @Test
    public void testMetaNotSupportedForTsv() {
        Assert.assertThrows(GorParsingException.class,
                () -> run("norrows 2 | write test.tsv -meta 'A=1'", null));
    }

    @Test
    public void testMetaNotSupportedForOtherOutputs() {
        Assert.assertThrows(GorParsingException.class,
                () -> run("gorrow chr1,1,100 | write test.bed -meta 'A=1'", null));
        Assert.assertThrows(GorParsingException.class,
                () -> run("nor -h <(gorrow chr1,1,100) | write test.gor -meta 'A=1'", null));
    }

    @Test
    public void testMetaOnForkWrite() {
        run("gorrows -p chr1:1-3 | calc f if(pos=1,'a','b') | write test_#{fork}.gorz -f f -meta 'A=x y'", null);
        Assert.assertEquals("x y", meta("test_a.gorz").getProperty("A"));
        Assert.assertEquals("x y", meta("test_b.gorz").getProperty("A"));
    }

    @Test
    public void testMetaOnPgorFolderWrite() throws IOException {
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gor"), workDirPath.resolve("dbsnp.gor"));
        run("pgor dbsnp.gor | write -meta 'A=x y' dbsnp2.gord", null);
        List<Path> metas;
        try (Stream<Path> s = Files.list(workDirPath.resolve("dbsnp2.gord"))) {
            metas = s.filter(p -> p.toString().endsWith(".gorz.meta")).collect(Collectors.toList());
        }
        Assert.assertFalse(metas.isEmpty());
        for (Path p : metas) {
            Assert.assertEquals(p.toString(), "x y",
                    GorMeta.createAndLoad(new DriverBackedFileReader(""), p.toString()).getProperty("A"));
        }
    }

    // --- -linkmeta values with spaces ---

    @Test
    public void testExtractLinkMetaOptionDataKeepsSpaces() {
        Assert.assertEquals("A=b c,D=e",
                LinkFileUtil.extractLinkMetaOptionData("gor x.gor | write y.gord -link z.gord -linkmeta 'A=b c,D=e' -d f"));
        Assert.assertEquals("A=b c",
                LinkFileUtil.extractLinkMetaOptionData("gor x.gor | write -linkMeta \"A=b c\" y.gord"));
        Assert.assertEquals("A=b",
                LinkFileUtil.extractLinkMetaOptionData("gor x.gor | write -linkmeta 'A=b' y.gord"));
        Assert.assertEquals("", LinkFileUtil.extractLinkMetaOptionData("gor x.gor | write y.gord"));
    }

    @Test
    public void testLinkMetaWithSpacesOnWrite() throws IOException {
        run("gorrow chr1,1,100 | write test.gor -link ltest.gor -linkmeta 'TEST1=a b,TEST2=T2'", null);
        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("ltest.gor.link").toString()));
        Assert.assertEquals("a b", linkFile.getMeta().getProperty("TEST1"));
        Assert.assertEquals("T2", linkFile.getMeta().getProperty("TEST2"));
    }

    @Test
    public void testLinkMetaWithSpacesOnPgor() throws IOException {
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gor"), workDirPath.resolve("dbsnp.gor"));
        run("pgor dbsnp.gor | write -link dbsnp3.gord -linkmeta 'TEST1=a b,TEST2=T2' dbsnp2.gord", null);
        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("dbsnp3.gord.link").toString()));
        Assert.assertEquals("a b", linkFile.getMeta().getProperty("TEST1"));
        Assert.assertEquals("T2", linkFile.getMeta().getProperty("TEST2"));
    }

    @Test
    public void testUnquotedLinkMetaOnPgor() throws IOException {
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gor"), workDirPath.resolve("dbsnp.gor"));
        run("pgor dbsnp.gor | write -link dbsnp3.gord -linkmeta TEST1=T1 dbsnp2.gord", null);
        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("dbsnp3.gord.link").toString()));
        Assert.assertEquals("T1", linkFile.getMeta().getProperty("TEST1"));
    }

    @Test
    public void testLinkMetaWithSpacesOnParallel() throws IOException {
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gor"), workDirPath.resolve("dbsnp.gor"));
        run("parallel -parts <(nor dbsnp.gor | select chrom | distinct) <(gor -p #{col:Chrom} dbsnp.gor) | write -link dbsnp3.gord -linkmeta 'TEST1=a b' dbsnp2.gord", null);
        var linkFile = LinkFile.load(new FileSource(workDirPath.resolve("dbsnp3.gord.link").toString()));
        Assert.assertEquals("a b", linkFile.getMeta().getProperty("TEST1"));
    }
}
