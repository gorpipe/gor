package org.gorpipe.s3.shared;

import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.ExpectedException;

import java.io.IOException;

/**
 * Note, there are S3Shared integration tests in gor-services (ITestS3Shared).
 */
public class UTestS3Shared {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testResolveSourceProjectData() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3ProjectDataSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3data://project/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/projects/some_project/user_data/a/a.gor", source.getFullPath());
        Assert.assertEquals("s3data://project/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceProjectShared() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3ProjectSharedSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3data://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a/a.gor", source.getFullPath());
        Assert.assertEquals("s3data://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceProjectSharedProject() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3ProjectSharedProjectSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3data://shared-project/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/projects/some_project/user_data/a/a.gor", source.getFullPath());
        Assert.assertEquals("s3data://shared/projects/some_project/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceRegion() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3RegionSharedSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3region://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a/a.gor", source.getFullPath());
        Assert.assertEquals("s3data://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceGlobal() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3GlobalSharedSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3global://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a/a.gor", source.getFullPath());
        Assert.assertEquals("s3data://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceProjectDataFile() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3ProjectDataFileSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3datafile://project/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/projects/some_project/user_data/a.gor", source.getFullPath());
        Assert.assertEquals("s3datafile://project/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceProjectSharedFile() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3ProjectSharedFileSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3datafile://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a.gor", source.getFullPath());
        Assert.assertEquals("s3datafile://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceRegionFile() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3RegionSharedFileSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3regionfile://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a.gor", source.getFullPath());
        Assert.assertEquals("s3datafile://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceGlobalFile() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3GlobalSharedFileSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3globalfile://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a.gor", source.getFullPath());
        Assert.assertEquals("s3datafile://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    @Test
    public void testResolveSourceGlobaluseHighestTypeInLinksFalse() throws IOException {
        // Fallback triggers call to exists (which needs mock or actaul backend), turn that off for now.
        environmentVariables.set("GOR_S3_SHARED_USE_FALLBACK", "false");
        environmentVariables.set("GOR_S3_SHARED_USE_HIGHEST_TYPE_IN_LINKS", "false");
        ConfigManager.clearPrefixConfigCache();

        S3SharedSourceProvider provider = new S3GlobalSharedSourceProvider();

        DataSource source = getDataSourceFromProvider(provider, "user_data/a.gor" , Credentials.OwnerType.System, "some_env");

        Assert.assertNotNull("Source should be resolved", source);
        Assert.assertEquals("S3", source.getSourceType().getName());
        Assert.assertEquals("s3global://shared/user_data/a.gor", source.getSourceReference().getOriginalSourceReference().getUrl());
        Assert.assertEquals("s3://some_s3_bucket/shared/user_data/a/a.gor", source.getFullPath());
        Assert.assertEquals("s3global://shared/user_data/a.gor", source.getProjectLinkFileContent());
    }

    private DataSource getDataSourceFromProvider(S3SharedSourceProvider provider, String relativePath,
                                                 Credentials.OwnerType ownerType, String owner) throws IOException {
        SourceReference sourceReference = new SourceReferenceBuilder(provider.getSharedUrlPrefix() + relativePath)
                .commonRoot("projects/some_project")
                .securityContext(createSecurityContext(provider.getService(), "some_s3_bucket", ownerType, owner))
                .build();
        DataSource source = provider.resolveDataSource(sourceReference);
        return source;
    }

    private String createSecurityContext(String service, String bucket, Credentials.OwnerType ownerType, String owner) {
        Credentials creds = new Credentials.Builder().service(service).lookupKey(bucket).ownerType(ownerType).ownerId(owner)
                .set("key", "dummykey")
                .set("secret", "dummysecret")
                .build();
        BundledCredentials bundleCreds = new BundledCredentials.Builder().addCredentials(creds).build();
        return bundleCreds.addToSecurityContext("");

    }
}
