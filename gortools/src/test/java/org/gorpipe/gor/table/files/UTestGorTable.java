package org.gorpipe.gor.table.files;

import org.gorpipe.gor.model.Row;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class UTestGorTable {

    private static final Logger log = LoggerFactory.getLogger(UTestGorTable.class);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @BeforeClass
    public static void setUp() {

    }

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testCreateGorTableFromEmpty() throws Exception {
        String name = "fromempty";
        Path gorFile = workDirPath.resolve(name + ".gor");

        GorTable<Row> table = new GorTable<>(gorFile.toUri());
        table.setColumns("chrom", "pos", "ref");
        table.save();

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals("#chrom\tpos\tref\n", Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertTrue(meta.contains("## CREATED = "));
        Assert.assertEquals("## SERIAL = 1\n" +
                        "## USE_HISTORY = true\n" +
                        "## FILE_FORMAT = 1.0\n" +
                        "## VALIDATE_FILES = true\n" +
                        "## COLUMNS = chrom,pos,ref",
                Arrays.stream(meta.split("\n"))
                        .filter(l -> !l.startsWith("## CREATED"))
                        .collect(Collectors.joining("\n")));
    }

    @Test
    public void testCreateGorTableFromExisting() throws Exception {
        String name = "fromexisting";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\n";

        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toUri());
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals(content, Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 1\n" +
                "## USE_HISTORY = true\n" +
                "## VALIDATE_FILES = true\n" +
                "## COLUMNS = chrom,pos,ref\n", meta);
    }

    @Test
    public void testCreateGorTableFromExistingWithMeta() throws Exception {
        String name = "fromexisting";
        Path gorFile = workDirPath.resolve(name + ".gor");
        Path metaFile = Path.of(gorFile + ".meta");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        String meta = "## SERIAL = 1\n" +
                "## USE_HISTORY = true\n" +
                "## CREATED = 2021-12-08 16:43\n" +
                "## FILE_FORMAT = 2.0\n" +
                "## VALIDATE_FILES = false\n" +
                "## COLUMNS = chrom,pos,ref\n";
        Files.write(metaFile, meta.getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toUri());
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals(content, Files.readString(gorFile));
        
        Assert.assertTrue(Files.exists(metaFile));
        Assert.assertEquals("## SERIAL = 2\n" +
                "## USE_HISTORY = true\n" +
                "## CREATED = 2021-12-08 16:43\n" +
                "## FILE_FORMAT = 2.0\n" +
                "## VALIDATE_FILES = false\n" +
                "## COLUMNS = chrom,pos,ref\n", Files.readString(metaFile));
    }
}
