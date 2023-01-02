package org.gorpipe.s3.shared;

import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.utils.DriverUtils;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.model.DriverBackedSecureFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.session.GorContext;
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

import static gorsat.TestUtils.runGorPipeCLI;
import static gorsat.TestUtils.runGorPipeServer;
import static org.gorpipe.utils.DriverUtils.createSecurityContext;

@Category(IntegrationTests.class)
public class ITestS3Shared {

    private static String S3_KEY;
    private static String S3_SECRET;

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

        String dataPath = "user_data/a.gor";
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

        String dataPath = "BVL_MOTHER_SLC52A2.vcf.gz.gorz"; // Exists in shared but not in project.

        DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env");

        Assert.assertEquals("s3://nextcode-unittest/shared/BVL_MOTHER_SLC52A2/BVL_MOTHER_SLC52A2.vcf.gz.gorz", source.getSourceReference().getUrl());
    }

    @Test
    public void testProjectRead() throws IOException {
        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));

        String dataPath = "BVL_FATHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_MOTHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_MOTHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_MOTHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_MOTHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_FATHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_FATHER_SLC52A2.vcf.gz.gorz";
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

        String dataPath = "BVL_FATHER_SLC52A2.vcf.gz.gorz";
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
        Path linkFile = gorRoot.resolve("a.gorz.link");
        Files.createDirectory(gorRoot);
        Files.write(linkFile, "s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz".getBytes(StandardCharsets.UTF_8));

        FileReader fileReader = new DriverBackedSecureFileReader(gorRoot.toString(), null,
                createSecurityContext("s3data", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET), null);
        DataSource source = fileReader.resolveUrl("a.gorz");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("a.gorz", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getSourceReference().getParentSourceReference().getUrl());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/user_data/BVL_INDEX_SLC52A2/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getFullPath());

        source = fileReader.resolveUrl("a.gorz.link");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("a.gorz.link", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getSourceReference().getParentSourceReference().getUrl());
        Assert.assertEquals("s3://nextcode-unittest/projects/some_project/user_data/BVL_INDEX_SLC52A2/BVL_INDEX_SLC52A2.vcf.gz.gorz", source.getFullPath());
    }

    @Test
    public void testReadWithUnaccessableLinkFile() throws IOException {
        Path gorRoot  = workDirPath.resolve("some_project");
        Path linkFile = workDirPath.resolve("a.gorz.link");
        Files.createDirectory(gorRoot);
        Files.write(linkFile, "s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz".getBytes(StandardCharsets.UTF_8));

        FileReader fileReader = new DriverBackedSecureFileReader(gorRoot.toString(), null,
                createSecurityContext("s3data", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET), null);

        try {
            fileReader.resolveUrl("../a.gorz");
            Assert.fail("Should not be resolved, link outside project");
        } catch (GorResourceException e) {
            // Expected
            Assert.assertTrue(e.getMessage().contains("File paths must be within project scope"));
        }

        try {
            DataSource source = fileReader.resolveUrl("../a.gorz.link");
            Assert.fail("Should not be resolved, link outside project");
        } catch (GorResourceException e) {
            // Expected
            Assert.assertTrue(e.getMessage().contains("File paths must be within project scope"));
        }
     }

    @Test
    public void testReadDirectly() {
        Path gorRoot  = workDirPath.resolve("some_project");
        FileReader fileReader = new DriverBackedSecureFileReader(gorRoot.toString(), null,
                createSecurityContext("s3data", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET), null);

        try {
            fileReader.resolveUrl("s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz");
            Assert.fail("Should not be able to read shared link directly");
        } catch (GorResourceException e) {
            // Expected
            Assert.assertEquals("S3 shared resources can only be accessed using links.", e.getMessage());
        }
    }

    @Test
    @Ignore("Slow test, ment to be manually run")
    public void testReadServer() throws IOException {
        Path gorRoot  = workDirPath.resolve("some_project");
        Path linkFile = gorRoot.resolve("a.gorz.link");
        Files.createDirectory(gorRoot);
        //Files.write(linkFile, "s3data://project/user_data/BVL_INDEX_SLC52A2.vcf.gz.gorz".getBytes(StandardCharsets.UTF_8));
        Files.write(linkFile, "s3data://project/ref/dbsnp.gorz".getBytes(StandardCharsets.UTF_8));
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);

        String result = runGorPipeServer("a.gorz | top 1000000 | group genome -count", gorRoot.toString(), securityContext);

        Assert.assertEquals("Chrom\tbpStart\tbpStop\tallCount\n" +
                "chrA\t0\t1000000000\t1000000\n", result);
    }

    @Test
    public void testProjectWriteRootCLI() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "dummy.gor";

        runGorPipeCLI("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, dataPath + ".link")));
    }

    @Test
    public void testProjectWriteUserDataCLI() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "user_data/dummy.gor";

        runGorPipeCLI("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, dataPath + ".link")));
    }

    @Test
    public void testProjectWriteRootServer() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "dummy.gor";

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
        String dataPath = "user_data/dummy.gor";

        runGorPipeServer("gorrow 1,2,3 | write s3data://project/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.Project, "some_project")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, dataPath + ".link")));
    }

    @Test
    public void testSharedWriteRootServer() throws IOException {
        String securityContext = createSecurityContext("s3region", Credentials.OwnerType.System, "some_env", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "dummy.gor";

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
        String dataPath = "user_data/dummy.gor";

        runGorPipeServer("gorrow 1,2,3 | write s3region://shared/" + dataPath, gorRoot, securityContext);

        S3SharedSourceProvider provider = new S3RegionSharedSourceProvider();
        provider.setConfig(ConfigManager.getPrefixConfig("gor", GorDriverConfig.class));
        try (DataSource source = getDataSourceFromProvider(provider, dataPath, Credentials.OwnerType.System, "some_env")) {
            source.delete();
        }

        Assert.assertTrue(Files.exists(Path.of(gorRoot, dataPath + ".link")));
    }

    @Test
    public void testProjecSharedProjecttWriteUserDataServer() throws IOException {
        String securityContext = createSecurityContext("s3data", Credentials.OwnerType.Project, "some_project", S3_KEY, S3_SECRET);
        String gorRoot  = Path.of(workDir.getRoot().toString(), "some_project").toString();
        String dataPath = "user_data/dummy.gor";

        Assert.assertFalse(Files.exists(Path.of(gorRoot, dataPath + ".link")));

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

        Assert.assertTrue(Files.exists(Path.of(gorRoot, dataPath + ".link")));
    }

    private DataSource getDataSourceFromProvider(S3SharedSourceProvider provider, String relativePath,
                                                 Credentials.OwnerType ownerType, String owner) throws IOException {
        SourceReference sourceReference = new SourceReference.Builder(provider.getSharedUrlPrefix() + relativePath)
                .commonRoot("projects/some_project")
                .securityContext(createSecurityContext(provider.getService(), ownerType, owner, S3_KEY, S3_SECRET))
                .build();
        DataSource source = provider.resolveDataSource(sourceReference);
        return source;
    }


}
