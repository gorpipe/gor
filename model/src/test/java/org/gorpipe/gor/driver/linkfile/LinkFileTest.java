package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.FileReader;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.gorpipe.gor.driver.linkfile.LinkFileV1.LinkReuseStrategy.NO_REUSE;
import static org.gorpipe.gor.driver.linkfile.LinkFileV1.LinkReuseStrategy.REUSE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LinkFileTest {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    private FileReader fileReader;

    private StreamSource mockSource;
    private StreamSource source;
    private final String v1LinkFileContent = """
            ## SERIAL = 2
            ## VERSION = 1
            #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
            source/v1/ver1.gorz\t2024-12-15T11:21:30.790Z\tABCDEAF13422\t1\t
            source/v1/ver2.gorz\t2024-12-15T23:25:24.533Z\t334DEAF13422\t2\t
            """;
    // 2024-12-15T11:21:30.790Z  = 1734261690790
    // 2024-12-15T23:25:24.533Z  = 1734305124533L
    private final String v0LinkFileContent = "source/v0/verx.gorz\n";
    private final String simpleFile = "source/y.gorz";
    protected Path workPath;

    @Before
    public void setUp() throws IOException {
        workPath = workDir.getRoot().toPath().toAbsolutePath();

        source = new FileSource(workPath.resolve("test.link").toString());

        fileReader = new DriverBackedFileReader(null, workPath.toString());
        mockSource = mock(StreamSource.class);
    }

    @Test
    public void testCreateLinkFile() {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        assertNotNull(linkFile);
        assertTrue(linkFile instanceof LinkFileV1);
        assertEquals("1", linkFile.getMeta().getVersion());
        assertEquals(2, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Test
    public void testCreateLinkFileSimple() throws IOException {
        LinkFile linkFile = LinkFile.createV1(source, "test.gorz");
        assertNotNull(linkFile);
        assertTrue(linkFile instanceof LinkFileV1);
        assertEquals("1", linkFile.getMeta().getVersion());
        assertEquals(1, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Ignore("Fiddly test depending on system properties, ignore for now.  Can run in isolation to verify.")
    @Test
    public void testCreateLinkFileSimpleWithDefault0() {
        System.setProperty("gor.driver.link.default.version", "0");
        LinkFile linkFile = LinkFile.create(source, "test.gorz");
        assertNotNull(linkFile);
        assertTrue(linkFile instanceof LinkFileV0);
        assertEquals("0", linkFile.getMeta().getVersion());
        assertEquals(1, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Test
    public void testLoadLinkFile() throws IOException {
        Files.writeString(Path.of(source.getFullPath()), v1LinkFileContent);
        LinkFile linkFile = LinkFile.load(source);
        assertNotNull(linkFile);
        assertEquals(2, linkFile.getEntries().size());
        assertEquals(100, linkFile.getEntriesCountMax());
    }

    @Test
    public void testAppendEntry() {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.appendEntry(simpleFile, "NEWMD5SUM", "Test1");
        assertEquals(3, linkFile.getEntries().size());
    }

    @Test
    public void testGetLatestPath() {
        when(mockSource.getFullPath()).thenReturn("/mnt/csa/projects/test/x.link");
        LinkFile linkFile = LinkFile.create(mockSource, v1LinkFileContent);
        assertEquals("/mnt/csa/projects/test/source/v1/ver2.gorz", linkFile.getLatestEntryUrl());
        linkFile.appendEntry(simpleFile, "NEWMD5SUM", "");
        assertEquals("/mnt/csa/projects/test/" + simpleFile, linkFile.getLatestEntryUrl());
    }

    @Test
    public void testGetTimedPath() {
        when(mockSource.getFullPath()).thenReturn("/mnt/csa/projects/test/x.link");
        LinkFile linkFile = LinkFile.create(mockSource, v1LinkFileContent);
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");

        assertEquals(null, linkFile.getEntryUrl(1734261690790L - 1000));
        assertEquals("/mnt/csa/projects/test/source/v1/ver1.gorz", linkFile.getEntryUrl(1734261690790L + 1000));
        assertEquals("/mnt/csa/projects/test/source/v1/ver2.gorz", linkFile.getEntryUrl(1734305124533L));
        assertEquals("/mnt/csa/projects/test/source/v1/ver2.gorz", linkFile.getEntryUrl(1734305124533L + 1000));
        assertEquals("/mnt/csa/projects/test/" + simpleFile, linkFile.getEntryUrl(System.currentTimeMillis()));
    }

    @Test
    public void testSaveNewV1LinkFile() throws IOException {
        var linkPath = workPath.resolve("test.link");
        LinkFile linkFile = LinkFile.createV1(new FileSource(linkPath.toString()), "");
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save(fileReader);
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveNewV0LinkFile() throws IOException {
        var linkPath = workPath.resolve("test.link");
        LinkFile linkFile = LinkFile.createV0(new FileSource(linkPath.toString()), "");
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save(fileReader);
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test
    public void testSaveLinkFileV1ToV1() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, v1LinkFileContent);
        LinkFile linkFile = LinkFile.load(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save(fileReader);
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.startsWith("## SERIAL = 3"));
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveLinkFileV0ToV0() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, "a/b/c.gorz");
        LinkFile linkFile = LinkFile.load(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save(fileReader);
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test
    public void testSaveLinkFileV0ToV1() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, "a/b/c.gorz");
        LinkFile linkFile = LinkFile.loadV1(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save(fileReader);
        String savedContent = Files.readString(linkPath);
        assertTrue(savedContent.contains("## VERSION = 1"));
        assertEquals(2, linkFile.getEntries().size());
        assertTrue(savedContent.contains(simpleFile));
    }

    @Test
    public void testSaveLinkFileV1ToV0() throws IOException {
        var linkPath = workPath.resolve("test.link");
        Files.writeString(linkPath, v1LinkFileContent);
        LinkFile linkFile = LinkFile.loadV0(new FileSource(linkPath.toString()));
        linkFile.appendEntry(simpleFile, "NEWMD5SUM");
        linkFile.save(fileReader);
        String savedContent = Files.readString(linkPath);
        assertEquals(simpleFile, savedContent.trim());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInferDataFileNameFromLinkFile_NullOrEmptyPath() throws Exception {
        LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource(""), null);
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromEnvVariable_WithProject() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_ROOT_URL, root);

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource(new SourceReference("x.gor.link", null, "/projects/test", -1, null,  null, false, false)), null);
        assertNotNull(result);
        assertTrue(result.matches((root + "/test/x/x_.*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromEnvVariable_WithOutProject() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_ROOT_URL, root);

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource("x.gor.link"), null);
        assertTrue(result.matches((root + "/x/x_.*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromExiting_File() throws Exception {
        String root = "/managed/fromfile";
        String linkFilePath =  "x.gor.link";
        Files.createDirectory(workPath.resolve("test"));
        Files.writeString(workPath.resolve("test").resolve(linkFilePath), root + "/source/y/y.gorz\n");

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource(new SourceReference(linkFilePath, null, workPath.resolve("test").toString(), -1, null,  null, false, false)), null);
        assertNotNull(result);
        assertTrue(result.matches((root + "/source/x/x_.*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromMetaParam() throws Exception {
        String root = "/managed/fromparam";
        String linkFilePath = "x.gor.link";
        String linkFileMeta = "## " + LinkFileMeta.HEADER_DATA_LOCATION_KEY + " = " + root;

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource(new SourceReference(linkFilePath)), linkFileMeta);
        assertNotNull(result);
        assertTrue(result.matches((root + "/x/x_..*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_FromMetaParam_ExistingFile() throws Exception {
        String fileroot = "/managed/fromfile";
        String linkFilePath = "x.gor.link";
        Files.writeString(workPath.resolve(linkFilePath), "## " + LinkFileMeta.HEADER_DATA_LOCATION_KEY + " = " + fileroot + "\nsource/y.gorz\n");

        String paramroot = "/managed/fromparam";
        String linkFileMeta = "## " + LinkFileMeta.HEADER_DATA_LOCATION_KEY + " = " + paramroot;

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource(new SourceReference(linkFilePath)), linkFileMeta);
        assertNotNull(result);
        assertTrue(result.matches((paramroot + "/x/x_.*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testInferDataFileNameFromLinkFile_AbsolutePath() throws Exception {
        String root = "/managed/root";
        environmentVariables.set(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_ROOT_URL, root);

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource("/abs/path/x.gor.link"), null);

        assertNotNull(result);
        assertTrue(result.matches((root + "/x/x_.*\\.gor").replace("/", "\\/")));
    }

    @Test
    public void testLimitByNumberOfEntries() throws IOException {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.setEntriesCountMax(2);

        // Current entries: 2. Add one more.
        linkFile.appendEntry("new_entry.gor", "md5");
        linkFile.save(fileReader);

        LinkFile saved = LinkFile.loadV1(source);
        assertEquals(2, saved.getEntriesCount());
        // Verify the latest entry is the one we appended
        assertEquals(workPath.resolve("new_entry.gor").toString(), saved.getLatestEntryUrl());
        // Verify the oldest entry was dropped.
        assertEquals("source/v1/ver2.gorz", saved.getEntries().get(0).url());
    }

    @Test
    public void testLimitByTimestamp() throws IOException {
        // v1LinkFileContent contains timestamps:
        // ver1: 1734261690790
        // ver2: 1734305124533

        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.setEntriesAgeMax(10000);

        // Save with current time far in the future
        long futureTime = 1734305124533L + 20000;
        linkFile.save(futureTime, fileReader);

        LinkFile saved = LinkFile.loadV1(source);

        // Both original entries are older than 10s from futureTime.
        // Should be empty or 0 entries written (if header preserved)
        assertEquals(0, saved.getEntriesCount());
    }

    @Test
    public void testLimitBySize() throws IOException {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_REUSE_STRATEGY_KEY, NO_REUSE.name());

        // Generate a large info string to exceed default 10000 bytes.
        // 4000 chars * 3 entries should exceed it.
        String largeInfo = java.util.stream.IntStream.range(0, 4000).mapToObj(i -> "x").reduce("", String::concat);

        linkFile.appendEntry("large1.gor", "md5", largeInfo);
        linkFile.appendEntry("large2.gor", "md5", largeInfo);
        linkFile.appendEntry("large3.gor", "md5", largeInfo);
        linkFile.save(fileReader);

        LinkFile saved = LinkFile.loadV1(source);

        // Should have fewer than 3+2=5 entries.
        // Likely latest 2 entries fit (~8kb + some), 3rd pushes over.
        assertTrue(saved.getEntriesCount() < 5);
        // Ensure latest is present
        assertEquals(workPath.resolve("large3.gor").toString(), saved.getLatestEntryUrl());
    }

    @Test
    public void testGarbageCollectionManaged() throws IOException {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.setEntriesCountMax(1); // Force eviction of oldest
        linkFile.getMeta().setProperty("DATA_LIFECYCLE_MANAGED", "true");

        FileReader mockReader = mock(FileReader.class);
        StreamSource mockDeletedSource = mock(StreamSource.class);

        // Original oldest is source/v1/ver1.gorz
        when(mockReader.resolveUrl(anyString())).thenReturn(mockDeletedSource);
        when(mockDeletedSource.exists()).thenReturn(true);

        linkFile.save(mockReader);

        // Verify async delete called. Use timeout because it's in a separate thread.
        verify(mockDeletedSource, timeout(1000).atLeastOnce()).delete();
        // Verify it tried to delete ver1 (the one evicted)
        verify(mockReader, timeout(1000)).resolveUrl(contains("ver1.gorz"));
    }

    @Test
    public void testNoGarbageCollectionUnmanaged() throws IOException {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.setEntriesCountMax(1); // Force eviction
        linkFile.getMeta().setProperty("DATA_LIFECYCLE_MANAGED", "false");

        FileReader mockReader = mock(FileReader.class);
        StreamSource mockDeletedSource = mock(StreamSource.class);

        when(mockReader.resolveUrl(anyString())).thenReturn(mockDeletedSource);

        linkFile.save(mockReader);

        // Verify delete NOT called.
        verify(mockDeletedSource, after(500).never()).delete();
    }

    @Test
    public void testReuseStrategyReuseSkipsDuplicateLatestEntryUnManaged() throws IOException {
        var setupRes = setupReuseStrategyTest(REUSE, false);

        assertEquals(setupRes.initialCount, setupRes.linkFile.getEntriesCount());
        assertEquals(setupRes.latestBefore, setupRes.linkFile.getLatestEntry());
        assertTrue(Files.exists(setupRes.newFile)); // Verify the new file is not used and not deleted.
    }

    @Test
    public void testReuseStrategyReuseDataCreatesNewEntryWithExistingUrlUnManaged() throws IOException {
        var setupRes = setupReuseStrategyTest(LinkFileV1.LinkReuseStrategy.REUSE_DATA, false);

        assertEquals(setupRes.initialCount + 1, setupRes.linkFile.getEntriesCount());
        var latestAfter = setupRes.linkFile.getLatestEntry();
        assertEquals("source/v1/ver2.gorz", latestAfter.url());
        assertEquals("new entry info", latestAfter.info());
        assertEquals(setupRes.latestBefore.serial() + 1, latestAfter.serial());
        assertTrue(Files.exists(setupRes.newFile)); // Verify the new file is not used and not deleted.
    }

    @Test
    public void testReuseStrategyNoReuseKeepsNewUrlUnManaged() throws IOException {
        var setupRes = setupReuseStrategyTest(NO_REUSE, false);

        assertEquals(setupRes.initialCount + 1, setupRes.linkFile.getEntriesCount());
        var latestAfter = setupRes.linkFile.getLatestEntry();
        assertEquals(setupRes.newFile.toString(), latestAfter.url());
        assertEquals("new entry info", latestAfter.info());
        assertTrue(Files.exists(setupRes.newFile)); // Verify the new file is not used and not deleted.
    }

    @Test
    public void testReuseStrategyReuseSkipsDuplicateLatestEntryManaged() throws IOException {
        var setupRes = setupReuseStrategyTest(REUSE, true);

        assertEquals(setupRes.initialCount, setupRes.linkFile.getEntriesCount());
        assertEquals(setupRes.latestBefore, setupRes.linkFile.getLatestEntry());
        assertFalse(Files.exists(setupRes.newFile)); // Verify the new file is not used and deleted.
    }

    @Test
    public void testReuseStrategyReuseDataCreatesNewEntryWithExistingUrlManaged() throws IOException {
        var setupRes = setupReuseStrategyTest(LinkFileV1.LinkReuseStrategy.REUSE_DATA, true);

        assertEquals(setupRes.initialCount + 1, setupRes.linkFile.getEntriesCount());
        var latestAfter = setupRes.linkFile.getLatestEntry();
        assertEquals("source/v1/ver2.gorz", latestAfter.url());
        assertEquals("new entry info", latestAfter.info());
        assertEquals(setupRes.latestBefore.serial() + 1, latestAfter.serial());
        assertFalse(Files.exists(setupRes.newFile)); // Verify the new file is not used and deleted.
    }

    @Test
    public void testReuseStrategyNoReuseKeepsNewUrlManaged() throws IOException {
        var setupRes = setupReuseStrategyTest(NO_REUSE, true);

        assertEquals(setupRes.initialCount + 1, setupRes.linkFile.getEntriesCount());
        var latestAfter = setupRes.linkFile.getLatestEntry();
        assertEquals(setupRes.newFile.toString(), latestAfter.url());
        assertEquals("new entry info", latestAfter.info());
        assertTrue(Files.exists(setupRes.newFile)); // Verify the new file is not used and not deleted.
    }

    @Test
    public void testReuseStrategyReuseKeepsNewUrlUnManagedDontDeleteExisting() throws IOException {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_REUSE_STRATEGY_KEY, REUSE.name());
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY, String.valueOf(true));

        int initialCount = linkFile.getEntriesCount();
        var latestBefore = linkFile.getLatestEntry();

        Path newFile = workPath.resolve("ver3.gor");
        Files.writeString(newFile, "#Chrom\tPos\nchr1\t100\n");
        // Add same entry twice, make sure we dont delete the file.
        linkFile.appendEntry(newFile.toString(), "123", "new entry info", fileReader);
        linkFile.appendEntry(newFile.toString(), "123", "new entry info", fileReader);

        assertEquals(initialCount + 1, linkFile.getEntriesCount());
        assertNotEquals(latestBefore, linkFile.getLatestEntry());
        assertEquals("new entry info", linkFile.getLatestEntry().info());
        assertTrue(Files.exists(newFile)); // Verify the new file is not used and deleted.
    }

    record ReuseStrategySetupResult(LinkFile linkFile, int initialCount, LinkFileEntry latestBefore, Path newFile) {}
    private ReuseStrategySetupResult setupReuseStrategyTest(LinkFileV1.LinkReuseStrategy reuseStrategy, boolean lifecycleManaged) throws IOException {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_REUSE_STRATEGY_KEY, reuseStrategy.name());
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY, String.valueOf(lifecycleManaged));

        int initialCount = linkFile.getEntriesCount();
        var latestBefore = linkFile.getLatestEntry();

        Path newFile = workPath.resolve("ver3.gor");
        Files.writeString(newFile, "#Chrom\tPos\nchr1\t100\n");

        linkFile.appendEntry(newFile.toString(), "334DEAF13422", "new entry info", fileReader);

        return new ReuseStrategySetupResult(linkFile, initialCount, latestBefore, newFile);
    }
}
