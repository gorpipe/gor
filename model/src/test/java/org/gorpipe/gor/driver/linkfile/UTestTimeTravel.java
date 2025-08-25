package org.gorpipe.gor.driver.linkfile;

import gorsat.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class UTestTimeTravel {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    protected Path workPath;
    private String aold = """
                chrom\tpos\tval
                1\t100\taold
                """;
    private String anew = """
                chrom\tpos\tval
                1\t100\tanew
                """;
    private String bold = """
                chrom\tpos\tval
                1\t100\tbold
                """;
    private String bnew = """
                chrom\tpos\tval
                1\t100\tbnew
                """;
    private LinkFile linkFileB;

    @Before
    public void setup() throws IOException {
        workPath = workDir.getRoot().toPath().toAbsolutePath();
        Files.createDirectories(workPath.resolve("result_cache"));

        setupData();
    }

    @Test
    public void testNoTimeTravel() {
        assertEquals(anew, TestUtils.runGorPipe("gor A.gor.link", "-gorroot", workPath.toString()));
    }

    @Test
    public void testTimeTravelSimple() {
        assertEquals(aold, TestUtils.runGorPipe("gor -time 1500000000000 A.gor.link", "-gorroot", workPath.toString()));
        assertEquals(anew, TestUtils.runGorPipe("gor -time 2500000000000 A.gor.link", "-gorroot", workPath.toString()));
    }

    @Test
    public void testTimeTravelWithNestedQuery() {
        assertEquals(anew, TestUtils.runGorPipe("gor -time 1500000000000 <(gor A.gor.link)", "-gorroot", workPath.toString()));
    }

    @Test
    public void testTimeTravelOnlyInNestedQuery() {
        assertEquals(aold, TestUtils.runGorPipe("gor <(gor -time 1500000000000 A.gor.link)", "-gorroot", workPath.toString()));
    }

    @Test
    public void testTimeTravelISO() {
        assertEquals(aold, TestUtils.runGorPipe("gor -time 2017-07-14T02:40:00Z A.gor.link", "-gorroot", workPath.toString()));
    }

    @Test
    public void testTimeTravelISOShort() {
        assertEquals(aold, TestUtils.runGorPipe("gor -time 2017-07-14 A.gor.link", "-gorroot", workPath.toString()));
    }

    @Test
    public void testQueryIntegrityChangeWhileRunning() {
        // Use the timestamp when query started.
        assertEquals(anew, TestUtils.runGorPipe("""
        create before = gor A.gor.link;
        create update = gor B.gor.link | join -snpsnp [before] | select 1-3 | write Alatest.gor -vlink A.gor.link;
        create after = gor A.gor.link | join -snpsnp [update] | select 1-3;
        gor [after]
        """, "-gorroot", workPath.toString()));

        // Force use latest.
        assertEquals(bnew, TestUtils.runGorPipe(String.format("""
        create before = gor A.gor.link;
        create update = gor B.gor.link | join -snpsnp [before] | select 1-3 | write Alatest.gor -vlink A.gor.link;
        create after = gor -time %d A.gor.link | join -snpsnp [update] | select 1-3;
        gor [after]
        """, Long.MAX_VALUE), "-gorroot", workPath.toString()));
    }

    private void setupData() throws IOException {
        Files.writeString(workPath.resolve("Aold.gor"), aold);
        Files.writeString(workPath.resolve("Anew.gor"), anew);
        Files.writeString(workPath.resolve("Bold.gor"), bold);
        Files.writeString(workPath.resolve("Bnew.gor"), bnew);

        Files.writeString(workPath.resolve("A.gor.link"), """
                ## VERSION = 1
                #File\tTimestamp\tMD5\tSerial
                Aold.gor\t1000000000000\t\t1
                Anew.gor\t1700000000000\t\t2
                """);

        Files.writeString(workPath.resolve("B.gor.link"), """
                ## VERSION = 1
                #File\tTimestamp\tMD5\tSerial
                Bold.gor\t1000000000000\t\t1
                Bnew.gor\t1700000000000\t\t2
                """);
    }

}
