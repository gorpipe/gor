package org.gorpipe.gor.driver.linkfile;

import org.gorpipe.exceptions.GorResourceException;
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

import static org.gorpipe.gor.driver.linkfile.LinkFile.LINK_FILE_VALIDATE_LOAD;
import static org.gorpipe.gor.driver.linkfile.LinkFile.LINK_FILE_VALIDATE_SAVE;
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

    @Test(expected = GorResourceException.class)
    public void testInferDataFileNameFromLinkFile_Managed_TargetAlreadyExists_Throws() throws Exception {
        // When managed and the inferred target already exists, throw to prevent silent overwrite.
        // Mock the reader so exists() always returns true regardless of the (random) inferred path.
        String linkFileMeta = "## VERSION = 1\n## " + LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY + " = true";
        FileReader mockReader = mock(FileReader.class);
        when(mockReader.exists(anyString())).thenReturn(true);

        LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource("managed.gor.link"), linkFileMeta, mockReader);
    }

    @Test
    public void testInferDataFileNameFromLinkFile_Managed_TargetMissing_Succeeds() throws Exception {
        // When managed but the target does not yet exist, the call must succeed.
        String linkFileMeta = "## VERSION = 1\n## " + LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY + " = true";
        FileReader mockReader = mock(FileReader.class);
        when(mockReader.exists(anyString())).thenReturn(false);

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource("managed2.gor.link"), linkFileMeta, mockReader);
        assertNotNull(result);
    }

    @Test
    public void testInferDataFileNameFromLinkFile_Unmanaged_TargetAlreadyExists_NoThrow() throws Exception {
        // When NOT managed, an existing target must not trigger an error.
        String linkFileMeta = "## VERSION = 1\n## " + LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY + " = false";
        FileReader mockReader = mock(FileReader.class);
        when(mockReader.exists(anyString())).thenReturn(true);

        String result = LinkFileUtil.inferDataFileNameFromLinkFile(new FileSource("unmanaged.gor.link"), linkFileMeta, mockReader);
        assertNotNull(result);
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

    // --- MD5 fix: null/empty MD5 handling ---

    @Test
    public void testReuseStrategy_emptyMd5_doesNotMatchAnotherEmptyMd5() throws IOException {
        // Two entries with empty MD5 must NOT be treated as duplicates via MD5 match.
        // With REUSE strategy they should not collapse into one entry just because both have no MD5.
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_REUSE_STRATEGY_KEY, REUSE.name());
        linkFile.getMeta().setProperty(LinkFileMeta.HEADER_DATA_LIFECYCLE_MANAGED_KEY, "false");

        int initialCount = linkFile.getEntriesCount();

        // Append two entries with no MD5 and distinct URLs — each should create a separate entry.
        linkFile.appendEntry("no_md5_file1.gor", "", "first entry", fileReader);
        linkFile.appendEntry("no_md5_file2.gor", "", "second entry", fileReader);

        assertEquals(initialCount + 2, linkFile.getEntriesCount());
    }

    @Test
    public void testReuseStrategy_emptyMd5_usesTimestampFallback() throws IOException {
        // With empty MD5, canReuseEntryWithSameUrl should fall through to timestamp comparison
        // rather than incorrectly matching two entries just because both have no MD5.
        System.setProperty("gor.link.versioned.allow.overwrite", "false");
        try {
            LinkFile linkFile = LinkFile.createV1(source, "");
            Path file1 = workPath.resolve("shared_url.gor");
            Files.writeString(file1, "#Chrom\tPos\nchr1\t100\n");

            // First append with empty MD5 — establishes the URL.
            linkFile.appendEntry(file1.toString(), "", "first", fileReader);

            // Second append with the same URL and same (empty) MD5 — the fallback is to compare
            // timestamps. Since ver1 timestamp <= file's last-modified, it should succeed.
            linkFile.appendEntry(file1.toString(), "", "second", fileReader);

            // If we reach here without an exception the timestamp fallback was used (not a false MD5 match).
            assertEquals(2, linkFile.getEntriesCount());
        } finally {
            System.clearProperty("gor.link.versioned.allow.overwrite");
        }
    }

    // --- checkIntegrity() ---

    @Test
    public void testCheckIntegrity_cleanFile_noViolations() {
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        var violations = linkFile.checkIntegrity();
        assertTrue("Expected no violations for a well-formed link file", violations.isEmpty());
    }

    @Test
    public void testCheckIntegrity_serialNotMonotonicAfterTimestampSort() {
        // Entries are stored with ascending timestamps but descending serials.
        // After parsing (which sorts by timestamp) the serial order is violated.
        String corruptContent = """
                ## SERIAL = 2
                ## VERSION = 1
                #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                a.gorz\t2024-12-15T23:25:24.533Z\tAAA\t1\t
                b.gorz\t2024-12-15T11:21:30.790Z\tBBB\t2\t
                """;
        LinkFile linkFile = LinkFile.create(source, corruptContent);
        var violations = linkFile.checkIntegrity();
        assertFalse("Expected a serial-order violation", violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.contains("serial")));
    }

    @Test
    public void testCheckIntegrity_duplicateSerials() {
        String corruptContent = """
                ## SERIAL = 2
                ## VERSION = 1
                #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                a.gorz\t2024-12-15T11:21:30.790Z\tAAA\t2\t
                b.gorz\t2024-12-15T23:25:24.533Z\tBBB\t2\t
                """;
        LinkFile linkFile = LinkFile.create(source, corruptContent);
        var violations = linkFile.checkIntegrity();
        assertFalse("Expected a serial-order violation", violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.contains("serial")));
    }

    @Test
    public void testCheckIntegrity_urlReusedWithDifferentMd5() {
        // The same URL appearing with two different non-empty MD5s means the file was overwritten.
        String content = """
                ## SERIAL = 2
                ## VERSION = 1
                #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                shared.gorz\t2024-12-15T11:21:30.790Z\tAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\t1\t
                shared.gorz\t2024-12-15T23:25:24.533Z\tBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB\t2\t
                """;
        LinkFile linkFile = LinkFile.create(source, content);
        var violations = linkFile.checkIntegrity();
        assertFalse("Expected a URL-reuse-with-different-MD5 violation", violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.contains("shared.gorz")));
    }

    @Test
    public void testCheckIntegrity_urlReusedWithSameMd5_noViolation() {
        // The same URL with the same MD5 is fine (same content, URL just repeated in history).
        String content = """
                ## SERIAL = 2
                ## VERSION = 1
                #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
                shared.gorz\t2024-12-15T11:21:30.790Z\tAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\t1\t
                shared.gorz\t2024-12-15T23:25:24.533Z\tAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\t2\t
                """;
        LinkFile linkFile = LinkFile.create(source, content);
        var violations = linkFile.checkIntegrity();
        assertTrue("Same URL with same MD5 should not be a violation", violations.isEmpty());
    }

    @Test
    public void testCheckIntegrity_v0_alwaysClean() {
        LinkFile v0 = LinkFile.createV0(source, "some/path.gorz");
        assertTrue(v0.checkIntegrity().isEmpty());
    }

    // --- validate() ---

    // Corrupt V1 content: entries have ascending timestamps but descending serials,
    // so after parsing (which sorts by timestamp) the serial order is violated.
    private static final String CORRUPT_V1_CONTENT = """
            ## SERIAL = 2
            ## VERSION = 1
            #FILE\tTIMESTAMP\tMD5\tSERIAL\tINFO
            a.gorz\t2024-12-15T23:25:24.533Z\tAAA\t1\t
            b.gorz\t2024-12-15T11:21:30.790Z\tBBB\t2\t
            """;

    @Test
    public void testValidate_false_cleanFile_doesNotThrow() {
        System.setProperty(LINK_FILE_VALIDATE_LOAD, "false");
        System.setProperty(LINK_FILE_VALIDATE_SAVE, "false");
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        assertNotNull(linkFile);
        linkFile.save(fileReader);
    }

    @Test
    public void testValidate_false_corruptFile_doesNotThrow() {
        System.setProperty(LINK_FILE_VALIDATE_LOAD, "false");
        System.setProperty(LINK_FILE_VALIDATE_SAVE, "false");
        LinkFile linkFile = LinkFile.create(source, CORRUPT_V1_CONTENT);
        assertNotNull(linkFile);
        linkFile.save(fileReader);
    }

    @Test
    public void testValidate_true_calledOnConstruction_cleanFile() {
        System.setProperty(LINK_FILE_VALIDATE_LOAD, "true");
        LinkFile.create(source, v1LinkFileContent);
    }

    @Test(expected = GorResourceException.class)
    public void testValidate_true_calledOnConstruction_corruptFile() {
        System.setProperty(LINK_FILE_VALIDATE_LOAD, "true");
        LinkFile.create(source, CORRUPT_V1_CONTENT);
    }

    @Test
    public void testValidate_calledOnSave_cleanFile() {
        System.setProperty(LINK_FILE_VALIDATE_LOAD, "false");
        System.setProperty(LINK_FILE_VALIDATE_SAVE, "true");
        LinkFile linkFile = LinkFile.create(source, v1LinkFileContent);
        linkFile.save(fileReader);
    }

    @Test(expected = GorResourceException.class)
    public void testValidate_calledOnSave_corruptFile() {
        System.setProperty(LINK_FILE_VALIDATE_LOAD, "false");
        System.setProperty(LINK_FILE_VALIDATE_SAVE, "true");
        LinkFile linkFile = LinkFile.create(source, CORRUPT_V1_CONTENT);
        linkFile.save(fileReader);
    }

    @Test
    public void testValidate_v0_doesNotThrow() {
        // V0 has no invariants; validate() must always succeed silently.
        LinkFile v0 = LinkFile.createV0(source, "some/path.gorz");
        v0.validate(); // must not throw
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
