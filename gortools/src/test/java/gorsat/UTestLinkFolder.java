package gorsat;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class UTestLinkFolder {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void testUrlLinkFolder() throws IOException {
        Path s = Paths.get("../tests/data/");
        String testDataUrl = s.toAbsolutePath().normalize().toUri().toString();
        Path workDirPath = workDir.getRoot().toPath();
        Path ba = workDirPath.resolve("ba");
        Path pdir = ba.resolve("sa");
        Files.createDirectories(pdir);
        Path hey = pdir.resolve("hey.link");
        Files.writeString(hey, testDataUrl);
        String remoteResult = TestUtils.runGorPipe("gor "+testDataUrl+"gor/genes.gorz | top 10");
        String linkResult = TestUtils.runGorPipe("gor ba/sa/hey/gor/genes.gorz | top 10", "-gorroot", workDirPath.toString());
        Assert.assertEquals("Results don't compare", remoteResult, linkResult);
    }

    @Test
    public void testUrlLinkUrlFolder() throws IOException {
        Path s = Paths.get("../tests/data/");
        String testDataUrl = s.toAbsolutePath().normalize().toUri().toString();
        Path workDirPath = workDir.getRoot().toPath();
        Path ba = workDirPath.resolve("ba");
        String testUrl = ba.toAbsolutePath().normalize().toUri().toString();
        Path pdir = ba.resolve("sa");
        Files.createDirectories(pdir);
        Path hey = pdir.resolve("hey.link");
        Files.writeString(hey, testDataUrl);
        String remoteResult = TestUtils.runGorPipe("gor "+testDataUrl+"gor/genes.gorz | top 10");
        String linkResult = TestUtils.runGorPipe("gor "+testUrl+"/sa/hey/gor/genes.gorz | top 10");
        Assert.assertEquals("Results don't compare", remoteResult, linkResult);
    }

    @Test
    public void testLinkFolder() throws IOException {
        Path s = Paths.get("../tests/data/");
        String testUrl = s.toAbsolutePath().normalize().toString()+"/";
        Path workDirPath = workDir.getRoot().toPath();
        Path ba = workDirPath.resolve("ba");
        Path pdir = ba.resolve("sa");
        Files.createDirectories(pdir);
        Path hey = pdir.resolve("hey.link");
        Files.writeString(hey, testUrl);
        String remoteResult = TestUtils.runGorPipe("gor "+testUrl+"gor/genes.gor | top 10", "-gorroot", workDirPath.toString());
        String linkResult = TestUtils.runGorPipe("gor ba/sa/hey/gor/genes.gor | top 10", "-gorroot", workDirPath.toString());
        Assert.assertEquals("Results don't compare", remoteResult, linkResult);
    }
}
