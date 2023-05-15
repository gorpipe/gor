package org.gorpipe.gor.table.files;

import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.table.dictionary.DictionaryEntry;
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

public class UTestNorTable {

    private static final Logger log = LoggerFactory.getLogger(UTestNorTable.class);

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
    public void testCreateNorTableFromEmpty() throws Exception {
        String name = "fromempty";
        Path norFile = workDirPath.resolve(name + ".nor");

        NorTable<Row> table = new NorTable<>(norFile.toString());
        table.setColumns("A", "B", "C");
        table.save();

        Assert.assertTrue(Files.exists(norFile));
        Assert.assertEquals("#A\tB\tC\n", Files.readString(norFile));

        Path metaFile = Path.of(norFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertTrue(meta.contains("## CREATED = "));
        Assert.assertEquals("## SERIAL = 1\n" +
                        "## FILE_FORMAT = 1.0\n" +
                        "## COLUMNS = A,B,C",
                Arrays.stream(meta.split("\n"))
                        .filter(l -> !l.startsWith("## CREATED"))
                        .collect(Collectors.joining("\n")));
    }

    @Test
    public void testCreateNorTableFromExisting() throws Exception {
        String name = "fromexisting";
        Path norFile = workDirPath.resolve(name + ".nor");

        String content = "#A\tB\tC\nd\te\tf\n";
        Files.write(norFile, content.getBytes(StandardCharsets.UTF_8));

        NorTable<Row> table = new NorTable<>(norFile.toString());
        table.save();

        Assert.assertArrayEquals(new String[]{"A", "B", "C"}, table.getColumns());

        Assert.assertTrue(Files.exists(norFile));
        Assert.assertEquals(content, Files.readString(norFile));

        Path metaFile = Path.of(norFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 1\n" +
                "## COLUMNS = A,B,C\n", meta);
    }

    @Test
    public void testCreateNorTableFromExistingWithMeta() throws Exception {
        String name = "fromexisting";
        Path norFile = workDirPath.resolve(name + ".nor");
        Path metaFile = Path.of(norFile + ".meta");

        String content = "#A\tB\tC\nd\te\tf\n";
        Files.write(norFile, content.getBytes(StandardCharsets.UTF_8));

        String meta = "## SERIAL = 1\n" +
                "## USE_HISTORY = true\n" +
                "## CREATED = 2021-12-08 16:43\n" +
                "## FILE_FORMAT = 2.0\n" +
                "## VALIDATE_FILES = false\n" +
                "## COLUMNS = A,B,C\n";
        Files.write(metaFile, meta.getBytes(StandardCharsets.UTF_8));

        NorTable<Row> table = new NorTable<>(norFile.toString());
        table.save();

        Assert.assertArrayEquals(new String[]{"A", "B", "C"}, table.getColumns());

        Assert.assertTrue(Files.exists(norFile));
        Assert.assertEquals(content, Files.readString(norFile));
        
        Assert.assertTrue(Files.exists(metaFile));
        Assert.assertEquals("## SERIAL = 2\n" +
                "## USE_HISTORY = true\n" +
                "## CREATED = 2021-12-08 16:43\n" +
                "## FILE_FORMAT = 2.0\n" +
                "## VALIDATE_FILES = false\n" +
                "## COLUMNS = A,B,C\n", Files.readString(metaFile));
    }

    @Test
    public void testCreateNorTableMultipleInserts() throws Exception {
        String name = "multipleInserts";
        Path norFile = workDirPath.resolve(name + ".nor");

        String content = "#A\tB\tC\nd\te\tf\n";
        Files.write(norFile, content.getBytes(StandardCharsets.UTF_8));

        NorTable<Row> table = new NorTable<>(norFile.toString());

        table.insert("d1\te1\tf1","d2\te2\tf2");;
        table.insert("d3\te3\tf3");
        table.save();

        Assert.assertArrayEquals(new String[]{"A", "B", "C"}, table.getColumns());

        Assert.assertTrue(Files.exists(norFile));
        Assert.assertEquals(content +
                "d1\te1\tf1\n" +
                "d2\te2\tf2\n" +
                "d3\te3\tf3\n", Files.readString(norFile));

        Path metaFile = Path.of(norFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));
        String meta = Files.readString(metaFile);
        Assert.assertEquals("## SERIAL = 1\n" +
                "## COLUMNS = A,B,C\n", meta);
    }

    @Test
    public void testCreateNorTableFromMultipleInsertsFromEntries() throws Exception {
        String name = "multipleInserts";
        Path norFile = workDirPath.resolve(name + ".nor");

        Files.write(workDirPath.resolve("input1.nor"), "#A\tB\tref\nchr2\t2\tT\n".getBytes(StandardCharsets.UTF_8));

        NorTable<Row> table = new NorTable<>(norFile.toString());

        table.insertEntries(List.of((DictionaryEntry) new DictionaryEntry.Builder("input1.nor",  workDirPath.toString()).alias("A").build()));
        table.save();

        Assert.assertArrayEquals(new String[]{"A", "B", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(norFile));
        Assert.assertEquals( "#A\tB\tref\n" +
                "chr2\t2\tT\n" , Files.readString(norFile));

        Path metaFile = Path.of(norFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));

        Assert.assertEquals("1", table.getProperty("SERIAL"));
        Assert.assertEquals("A,B,ref", table.getProperty("COLUMNS"));
    }

    @Test
    public void testTableInsertFromEntry() throws Exception {

        String name = "multipleInserts";
        Path norFile = workDirPath.resolve(name + ".nor");

        String content = "#A\tB\tref\nchr1\t1\tA\n";
        Files.write(norFile, content.getBytes(StandardCharsets.UTF_8));

        Files.write(workDirPath.resolve("input1.nor"), "#A\tB\tref\nchr2\t2\tT\n".getBytes(StandardCharsets.UTF_8));

        NorTable<Row> table = new NorTable<>(norFile.toString());

        table.insertEntries(List.of(
                (DictionaryEntry) new DictionaryEntry.Builder("input1.nor",  workDirPath.toString()).alias("A").build()));
        table.save();

        Assert.assertArrayEquals(new String[]{"A", "B", "ref"}, table.getColumns());

        Assert.assertTrue(Files.exists(norFile));
        Assert.assertEquals( "#A\tB\tref\n" +
                "chr1\t1\tA\n" +
                "chr2\t2\tT\n", Files.readString(norFile));

        Path metaFile = Path.of(norFile + ".meta");
        Assert.assertTrue(Files.exists(metaFile));

        Assert.assertEquals("1", table.getProperty("SERIAL"));
        Assert.assertEquals("A,B,ref", table.getProperty("COLUMNS"));
    }
}
