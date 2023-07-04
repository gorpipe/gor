package org.gorpipe.s3.shared;

import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.model.DriverBackedSecureFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.test.IntegrationTests;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import static gorsat.TestUtils.*;
import static org.gorpipe.gor.model.GorOptions.DEFAULT_FOLDER_DICTIONARY_NAME;
import static org.gorpipe.utils.DriverUtils.createSecurityContext;

@Category(IntegrationTests.class)
public class ITestS3Shared {

    private static String S3_KEY;
    private static String S3_SECRET;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue
            = new ProvideSystemProperty("aws.accessKeyId", S3_KEY);

    @Rule
    public final ProvideSystemProperty otherPropertyIsMissing
            = new ProvideSystemProperty("aws.secretKey", S3_SECRET);

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @BeforeClass
    static public void setUpClass() {
        Properties props = DriverUtils.getDriverProperties();
        S3_KEY = props.getProperty("S3_KEY");
        S3_SECRET = props.getProperty("S3_SECRET");
    }

    private Path workDirPath;

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testFallBackThrough() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile("user_data/a", DataType.GOR);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");
        Assert.assertTrue(source != null);
        Assert.assertTrue(!source.exists());
        Assert.assertEquals("s3data://project/" + dataPath, source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/user_data/a/a.gor", source.getSourceReference().getUrl());
    }

    @Test
    @Ignore
    public void testFallBackThroughWithException() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile("user_data/a", DataType.GOR);
        try {
            getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");
            Assert.fail("No fallback should throw error");
        } catch (GorResourceException gre) {
            Assert.assertTrue(systemErrRule.getLog().contains("File s3data://project/" + dataPath + " not found at " +
                    "s3data://project/, trying fallback s3data://shared/" + dataPath));
            Assert.assertTrue(systemErrRule.getLog().contains("File s3data://shared/" + dataPath + " not found at " +
                    "s3data://shared/, trying fallback s3region://shared/" + dataPath));
            Assert.assertTrue(systemErrRule.getLog().contains("File s3region://shared/" + dataPath + " not found at " +
                    "s3region://shared/, trying fallback s3global://shared/" + dataPath));
            Assert.assertTrue(systemErrRule.getLog().contains("File s3global://shared/" + dataPath + " not found at " +
                    "s3global://shared/, trying fallback None"));
        }
    }

    @Test
    public void testFallBackProjectToSharedProject() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_MOTHER_SLC52A2", DataType.VCFGZ), DataType.GORZ); // Exists in shared but not in project.

        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_MOTHER_SLC52A2/BVL_MOTHER_SLC52A2.vcf.gz.gorz", source.getSourceReference().getUrl());
    }

    @Test
    public void testProjectRead() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_FATHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/BVL_FATHER_SLC52A2/BVL_FATHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tHUUUART", iterator.getHeader());
        }
    }

    @Test
    public void testProjectReadUserData() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("user_data/BVL_INDEX_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/user_data/BVL_INDEX_SLC52A2/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNVKUKNN", iterator.getHeader());
        }
    }


    @Test
    public void testProjectSharedRead() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectSharedSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_MOTHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_MOTHER_SLC52A2/BVL_MOTHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNKHARLQ", iterator.getHeader());
        }
    }

    @Test
    public void testRegionSharedRead() throws IOException {
        S3SharedSourceProvider provider = new S3RegionSharedSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_MOTHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_MOTHER_SLC52A2/BVL_MOTHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNKHARLQ", iterator.getHeader());
        }
    }

    @Test
    public void testGlobalSharedRead() throws IOException {
        S3SharedSourceProvider provider = new S3GlobalSharedSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_MOTHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_MOTHER_SLC52A2/BVL_MOTHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNKHARLQ", iterator.getHeader());
        }
    }

    @Test
    public void testProjectFileRead() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataFileSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_MOTHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/BVL_MOTHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tNKHARLQ", iterator.getHeader());
        }
    }

    @Test
    public void testProjectSharedFileRead() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectSharedFileSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_FATHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_FATHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tHUUUART", iterator.getHeader());
        }
    }

    @Test
    public void testRegionSharedFileRead() throws IOException {
        S3SharedSourceProvider provider = new S3RegionSharedFileSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_FATHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_FATHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tHUUUART", iterator.getHeader());
        }
    }

    @Test
    public void testGlobalSharedFileRead() throws IOException {
        S3SharedSourceProvider provider = new S3GlobalSharedFileSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = DataUtil.toFile(DataUtil.toFile("BVL_FATHER_SLC52A2", DataType.VCFGZ), DataType.GORZ);
        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_FATHER_SLC52A2.vcf.gz.gorz", source.getFullPath());

        try(GenomicIterator iterator =  PluggableGorDriver.instance().createIterator(source)) {
            Assert.assertEquals("CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tHUUUART", iterator.getHeader());
        }
    }

    @Test
    public void testReadWithLinkFile() throws IOException {
        Path gorRoot  = workDirPath.resolve("some_project");
        Path linkFile = gorRoot.resolve(DataUtil.toLinkFile("a", DataType.GORZ));
        Files.createDirectory(gorRoot);
        Files.write(linkFile, DataUtil.toFile(DataUtil.toFile("s3data://project/user_data/BVL_INDEX_SLC52A2", DataType.VCFGZ), DataType.GORZ).getBytes(StandardCharsets.UTF_8));

        FileReader fileReader = new DriverBackedSecureFileReader(gorRoot.toString(), null,
                createSecurityContext("s3data", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET), null);
        DataSource source = fileReader.resolveUrl(DataUtil.toFile("a", DataType.GORZ));

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("a.gorz", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getSourceReference().getParentSourceReference().getUrl());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/user_data/BVL_INDEX_SLC52A2/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getFullPath());

        source = fileReader.resolveUrl(DataUtil.toLinkFile("a", DataType.GORZ));

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("a.gorz.link", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getSourceReference().getParentSourceReference().getUrl());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/user_data/BVL_INDEX_SLC52A2/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getFullPath());
    }

    @Test
    public void testReadWithUnaccessableLinkFile() throws IOException {
        Path gorRoot  = workDirPath.resolve("some_project");
        Path linkFile = workDirPath.resolve(DataUtil.toLinkFile("a", DataType.GORZ));
        Files.createDirectory(gorRoot);
        Files.write(linkFile, DataUtil.toFile(DataUtil.toFile("s3data://project/user_data/BVL_INDEX_SLC52A2", DataType.VCFGZ), DataType.GORZ).getBytes(StandardCharsets.UTF_8));

        FileReader fileReader = new DriverBackedSecureFileReader(gorRoot.toString(), null,
                createSecurityContext("s3data", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET), null);

        try {
            fileReader.resolveUrl(DataUtil.toFile("../a", DataType.GORZ));
            Assert.fail("Should not be resolved, link outside project");
        } catch (GorResourceException e) {
            // Expected
            Assert.assertTrue(e.getMessage().contains("File paths must be within project scope"));
        }

        try {
            fileReader.resolveUrl(DataUtil.toLinkFile("../a", DataType.GORZ));
            Assert.fail("Should not be resolved, link outside project");
        } catch (GorResourceException e) {
            // Expected
            Assert.assertTrue(e.getMessage().contains("File paths must be within project scope"));
        }
     }

    @Test
    @Ignore("Slow test, meant to be manually run")
    public void testReadServer() throws IOException {
        Path gorRoot  = workDirPath.resolve("some_project");
        Path linkFile = gorRoot.resolve(DataUtil.toLinkFile("a", DataType.GORZ));
        Files.createDirectory(gorRoot);
        //Files.write(linkFile, "s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz".getBytes(StandardCharsets.UTF_8));
        Files.writeString(linkFile, DataUtil.toFile("s3data://project/ref/dbsnp", DataType.GORZ));
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);

        String result = runGorPipeServer("a.gorz | top 1000000 | group genome -count", gorRoot.toString(), securityContext);

        Assert.assertEquals("Chrom\tbpStart\tbpStop\tallCount\n" +
                "chrA\t0\t1000000000\t1000000\n", result);
    }

    @Test
    public void testProjectWriteRootCLI() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("dummy", DataType.GOR);

        runGorPipeCLI("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath, DataType.LINK))));
    }

    @Test
    public void testProjectWriteUserDataCLI() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("user_data/dummy", DataType.GOR);

        runGorPipeCLI("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath, DataType.LINK))));
    }

    @Test
    public void testProjectWriteRootServer() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("dummy", DataType.GOR);

        try {
            runGorPipeServer("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);
            Assert.fail("Server context should not allow write to root");
        } catch (GorResourceException gre) {
            Assert.assertTrue(gre.getMessage().contains("File path not within folders allowed"));
        }
    }

    @Test
    public void testProjectWriteUserDataServer() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("user_data/dummy", DataType.GOR);

        runGorPipeServer("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath, DataType.LINK))));
    }

    @Test
    public void testProjectWriteUserDataWithIndexServer() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "user_data/dummy.gorz";

        runGorPipeServer("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        try (DataSource source = getDataSourceFromProvider(provider, dataPath + ".gori", Credentials.OwnerType.Project, "some_project")) {
            Assert.assertTrue(source.exists());
            source.delete();
        }

        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, dataPath + ".link")));
    }

    @Test
    public void testSharedWriteRootServer() throws IOException {
        String securityContext = createSecurityContext("s3region", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("dummy", DataType.GOR);

        try {
            runGorPipeServer("gorrow 1,2,3 | write s3region://shared/" + dataPath, gorRoot, securityContext);
            Assert.fail("Server context should not allow write to root");
        } catch (GorResourceException gre) {
            Assert.assertTrue(gre.getMessage().contains("File path not within folders allowed"));
        }
    }

    @Test
    public void testSharedWriteUserDataServer() throws IOException {
        String securityContext = createSecurityContext("s3region", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("user_data/dummy", DataType.GOR);

        runGorPipeServer("gorrow 1,2,3 | write s3region://shared/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3RegionSharedSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath, DataType.LINK))));
    }

    @Test
    public void testProjecSharedProjecttWriteUserDataServer() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = DataUtil.toFile("user_data/dummy", DataType.GOR);

        Assert.assertFalse(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath, DataType.LINK))));

        runGorPipeServer("gorrow 1,2,3 | write s3data://shared-project/" + dataPath, gorRoot, securityContext);

        // Access with shared-project
        S3ProjectSharedProjectSourceProvider providerSharedProject = new S3ProjectSharedProjectSourceProvider();
        providerSharedProject.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(providerSharedProject,  dataPath, Credentials.OwnerType.Project, "some_project")) {
            if (!source.exists()) {
                Assert.fail("Source should exists and be accessible using ProjectShredSource");
            }
        }

        // Access with just shared
        S3SharedSourceProvider providerShared = new S3ProjectSharedSourceProvider();
        providerShared.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(providerShared, "projects/some_project/" + dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath, DataType.LINK))));
    }

    //@Ignore("Runs too slowly")
    @Test
    public void testProjectWriteUserDataServerPgorGord() throws IOException {
        testProjectWriteUserDataServerPgorGordHelper(false);
    }

    //@Ignore("Runs too slowly")
    @Test
    public void testProjectWriteUserDataServerPgorGordSlash() throws IOException {
        testProjectWriteUserDataServerPgorGordHelper(true);
    }

    private void testProjectWriteUserDataServerPgorGordHelper(boolean useSlash) throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot = Path.of(workDir.getRoot().toString(), "some_project").toString();
        Files.createDirectories(Path.of(gorRoot).resolve("result_cache"));
        Files.createSymbolicLink(Path.of(gorRoot).resolve("genes.gor"), Path.of("../tests/data/gor/genes.gor").toAbsolutePath());
        String randomId = UUID.randomUUID().toString();
        String dataPath = DataUtil.toFile("user_data/dummy_" + randomId, DataType.GORD) + (useSlash ? "/" : "");
        try {
            runGorPipeServer("pgor -split 2 genes.gor | top 2 | write s3data://project/" + dataPath, gorRoot, securityContext);

            String expected = runGorPipeServer("pgor -split 2 genes.gor | top 2", gorRoot, securityContext);
            String result = runGorPipeServer("gor " + dataPath, gorRoot, securityContext);
            Assert.assertEquals(expected, result);

            Assert.assertTrue(Files.exists(Path.of(gorRoot, DataUtil.toFile(dataPath + "/" + DEFAULT_FOLDER_DICTIONARY_NAME, DataType.LINK))));

            Path linkPath = Path.of(gorRoot).resolve("test.gord.link");
            Files.writeString(linkPath, "s3data://project/" + dataPath + "/");
            result = runGorPipeServer("gor test.gord.link", gorRoot, securityContext);
            Assert.assertEquals(expected, result);

        } finally {
            FileReader fileReader = new DriverBackedFileReader(securityContext, gorRoot, null);
            fileReader.deleteDirectory("s3data://project/" + dataPath);
        }
    }

    @Ignore("Does not yet work")
    @Test
    public void testReadingExistingS3GordFolderWithDirectLinkToFolder() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot = Path.of(workDir.getRoot().toString(), "some_project").toString();
        Files.createDirectories(Path.of(gorRoot).resolve("result_cache"));
        Files.createSymbolicLink(Path.of(gorRoot).resolve("genes.gor"), Path.of("../tests/data/gor/genes.gor").toAbsolutePath());
        String dataPath = DataUtil.toFile("user_data/existing", DataType.GORD);

        String expected = runGorPipeServer("pgor -split 2 genes.gor | top 2", gorRoot, securityContext);

        Files.writeString(Path.of(gorRoot).resolve("linktest.gord.link"), PathUtils.markAsFolder("s3data://project/" + dataPath));
        String result = runGorPipeServer("gor linktest.gord.link", gorRoot, securityContext);
        Assert.assertEquals(expected, result);
    }

    @Ignore("Does not yet work")
    @Test
    public void testReadingExistingS3GordFolderWithDirectLinkToGord() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot = Path.of(workDir.getRoot().toString(), "some_project").toString();
        Files.createDirectories(Path.of(gorRoot).resolve("result_cache"));
        Files.createSymbolicLink(Path.of(gorRoot).resolve("genes.gor"), Path.of("../tests/data/gor/genes.gor").toAbsolutePath());
        String dataPath = DataUtil.toFile("user_data/existing", DataType.GORD);

        String expected = runGorPipeServer("pgor -split 2 genes.gor | top 2", gorRoot, securityContext);

        Files.writeString(Path.of(gorRoot).resolve("linktest.gord.link"), "s3data://project/" + dataPath + "/" + DEFAULT_FOLDER_DICTIONARY_NAME);
        String result = runGorPipeServer("gor linktest.gord.link", gorRoot, securityContext);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testWriteExplicitWrite() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "user_data/dummy2.gor";

        String result = runGorPipeCLI(String.format("create #x = gorrow chr1,1 | write s3data://shared/%s;\n" +
                "create #y = gor [#x] | calc x 4;\n" +
                "gor [#y]\n", dataPath), gorRoot, securityContext);

        Assert.assertEquals("chrom\tpos\tx\n" + "chr1\t1\t4\n", result);

        S3SharedSourceProvider provider = new S3ProjectSharedSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env")) {
            source.delete();
        }
    }

    private DataSource getDataSourceFromProvider(S3SharedSourceProvider provider, String relativePath,
                                                 Credentials.OwnerType ownerType, String owner) throws IOException {
        SourceReference sourceReference = new SourceReference.Builder(provider.getSharedUrlPrefix() + relativePath)
                .commonRoot("projects/some_project")
                .securityContext(createSecurityContext(provider.getService(), ownerType, owner, S3_KEY, S3_SECRET))
                .build();
        return provider.resolveDataSource(sourceReference);
    }


}
