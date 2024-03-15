package org.gorpipe.gor.table.files;

import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        GorTable<Row> table = new GorTable<>(gorFile.toString());
        table.setColumns("chrom", "pos", "ref");
        table.save();

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals("#chrom\tpos\tref\n", Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertTrue(meta.contains("## CREATED = "));
        Assert.assertEquals("## SERIAL = 1\n" +
                        "## FILE_FORMAT = 1.0\n" +
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

        GorTable<Row> table = new GorTable<>(gorFile.toString());
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals(content, Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 1\n" +
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

        GorTable<Row> table = new GorTable<>(gorFile.toString());
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

    @Test
    public void testCreateGorTableMultipleInserts() throws Exception {
        String name = "multipleInserts";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        table.insert("chr1\t2\tC","chr1\t3\tG");;
        table.insert("chr1\t4\tT\n");   // With new line.
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals(content +
                "chr1\t2\tC\n" +
                "chr1\t3\tG\n" +
                "chr1\t4\tT\n", Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 1\n" +
                "## COLUMNS = chrom,pos,ref\n", meta);
    }

    @Test
    public void testGorTableMultipleIdenticalInserts() throws Exception {
        String name = "multipleInserts";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        table.insert("chr1\t2\tC","chr1\t3\tG");;
        table.insert("chr1\t2\tC");
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals(content +
                "chr1\t2\tC\n" +
                "chr1\t2\tC\n" +
                "chr1\t3\tG\n", Files.readString(gorFile));
    }

    @Test
    public void testGorTableInsertPostProcessing() throws Exception {
        String name = "multipleInserts";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));
        Files.write(Path.of(gorFile.toString() + ".meta"),
                "## SELECT_TRANSFORM = sort genome | distinct".getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        table.insert("chr1\t2\tC","chr1\t3\tG");;
        table.insert("chr1\t2\tC");
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals(content +
                "chr1\t2\tC\n" +
                "chr1\t3\tG\n", Files.readString(gorFile));
    }

    @Test
    public void testCreateGorTableFromMultipleInsertsFromEntries() throws Exception {
        String name = "multipleInserts";
        Path gorFile = workDirPath.resolve(name + ".gor");

        Files.write(workDirPath.resolve("input1.gor"), "#chrom\tpos\tref\nchr2\t2\tT\n".getBytes(StandardCharsets.UTF_8));
        Files.write(workDirPath.resolve("input2.gor"), "#chrom\tpos\tref\nchr3\t3\tG\n".getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        table.insertEntries(List.of(
                (GorDictionaryEntry) new GorDictionaryEntry.Builder("input1.gor",  workDirPath.toString()).alias("A").build(),
                (GorDictionaryEntry) new GorDictionaryEntry.Builder("input2.gor",  workDirPath.toString()).alias("B").build()));
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals( "#chrom\tpos\tref\n" +
                "chr2\t2\tT\n" +
                "chr3\t3\tG\n", Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));

        Assert.assertEquals("1", table.getProperty("SERIAL"));
        Assert.assertEquals("chrom,pos,ref", table.getProperty("COLUMNS"));
    }

    @Test
    public void testTableInsertFromEntries() throws Exception {
        String name = "multipleInserts";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        Files.write(workDirPath.resolve("input1.gor"), "#chrom\tpos\tref\nchr2\t2\tT\n".getBytes(StandardCharsets.UTF_8));
        Files.write(workDirPath.resolve("input2.gor"), "#chrom\tpos\tref\nchr3\t3\tG\n".getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        table.insertEntries(List.of(
                (GorDictionaryEntry) new GorDictionaryEntry.Builder("input1.gor",  workDirPath.toString()).alias("A").build(),
                (GorDictionaryEntry) new GorDictionaryEntry.Builder("input2.gor",  workDirPath.toString()).alias("B").build()));
        table.save();

        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(gorFile));
        Assert.assertEquals( "#chrom\tpos\tref\n" +
                "chr1\t1\tA\n" +
                "chr2\t2\tT\n" +
                "chr3\t3\tG\n", Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 1\n" +
                "## COLUMNS = chrom,pos,ref\n", meta);
    }

    @Test
    public void testDeleteGorTable() throws Exception {
        String name = "delete";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\nchr1\t2\tC\nchr1\t3\tG\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        table.delete("chr1\t2\tC");
        table.save();
        
        Assert.assertArrayEquals(new String[]{"chrom", "pos", "ref"}, table.getColumns());
        Assert.assertEquals("#chrom\tpos\tref\nchr1\t1\tA\nchr1\t3\tG\n", Files.readString(gorFile));

        table.delete("chr1\t3\tG\n");   // With new line
        table.save();
        Assert.assertEquals("#chrom\tpos\tref\nchr1\t1\tA\n", Files.readString(gorFile));

        Path metaFile = Path.of(gorFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 2\n" +
                "## COLUMNS = chrom,pos,ref\n", meta);
    }

    @Test
    public void testGetLines() throws Exception {
        String name = "getLines";
        Path gorFile = workDirPath.resolve(name + ".gor");

        String content = "#chrom\tpos\tref\nchr1\t1\tA\nchr1\t2\tC\nchr1\t3\tG\n";
        Files.write(gorFile, content.getBytes(StandardCharsets.UTF_8));

        GorTable<Row> table = new GorTable<>(gorFile.toString());

        String streamContent;
        try (Stream<String> stream = table.getLines()) {
            streamContent = stream.collect(Collectors.joining("\n", "", "\n"));
        }

        Assert.assertEquals(content, streamContent);
    }
}
