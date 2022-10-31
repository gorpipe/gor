package org.gorpipe.s3.shared;

import com.google.auto.service.AutoService;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;

import java.util.Set;

@AutoService(SourceProvider.class)
public class S3ProjectDataFileSourceProvider extends S3SharedFileSourceProvider {

    public S3ProjectDataFileSourceProvider() {
    }

    public S3ProjectDataFileSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                           Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3ProjectDataFileSourceType.TYPE};
    }

    @Override
    public String getService() {
        return S3ProjectDataFileSourceType.SERVICE;
    }

    @Override
    protected String getSharedUrlPrefix() {
        return S3ProjectDataFileSourceType.PREFIX;
    }

    @Override
    protected String getBucketPostfix(String project) {
        if (project == null || project.isEmpty() || ".".equals(project)) {
            throw new GorResourceException(
                    String.format("Project must be set for project based shared s3 folders (%s)",
                            S3ProjectDataFileSourceType.PREFIX), null);
        }
        return String.format("projects/%s", project);
    }

    @Override
    protected String getFallbackUrl(String url) {
        String fallBackUrl = S3ProjectSharedFileSourceType.PREFIX + getRelativePath(url);
        return fallBackUrl;
    }

    @Override
    protected String findSharedSourceLinkContent(S3SharedSource source) {
        return getSharedUrlPrefix() + source.getRelativePath();
    }
}
