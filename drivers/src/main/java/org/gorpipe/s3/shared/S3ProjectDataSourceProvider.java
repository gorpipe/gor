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
public class S3ProjectDataSourceProvider extends S3SharedSourceProvider {

    public S3ProjectDataSourceProvider() {
    }

    public S3ProjectDataSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                       Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3ProjectDataSourceType.S3PROJECTDATA};
    }

    @Override
    public String getService() {
        return S3ProjectDataSourceType.S3PROJECTDATA_SERVICE;
    }

    @Override
    protected String getSharedUrlPrefix() {
        return S3ProjectDataSourceType.S3POJECTDATA_PREFIX;
    }

    @Override
    protected String getBucketPostfix(String project) {
        if (project == null || project.isEmpty() || ".".equals(project)) {
            throw new GorResourceException(
                    String.format("Project must be set for project based shared s3 folders (%s)",
                            S3ProjectDataSourceType.S3POJECTDATA_PREFIX), null);
        }
        return String.format("projects/%s", project);
    }

    @Override
    protected String getFallbackUrl(String url) {
        String fallBackUrl = S3ProjectSharedSourceType.S3PROJECTSHARED_PREFIX + getRelativePath(url);
        return fallBackUrl;
    }

}
