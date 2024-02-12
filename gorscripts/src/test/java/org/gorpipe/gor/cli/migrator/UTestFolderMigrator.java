package org.gorpipe.gor.cli.migrator;

import org.gorpipe.gor.reference.FolderMigrator;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class UTestFolderMigrator {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;


    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
    }


    @Test
    public void testMigrate() {

        FolderMigrator.migrate(Path.of("../tests/data/ref_mini/chromSeq"), workDirPath.resolve("chromSeqNew"));

        Assert.assertTrue(Files.exists(workDirPath.resolve("chromSeqNew")));
        Assert.assertTrue(Files.exists(workDirPath.resolve("chromSeqNew/chr1.txt")));
        Assert.assertTrue(Files.exists(workDirPath.resolve("chromSeqNew/chrY.txt")));
    }

}
