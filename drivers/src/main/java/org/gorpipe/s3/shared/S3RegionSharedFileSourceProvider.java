package org.gorpipe.s3.shared;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;

import java.util.Set;

@AutoService(SourceProvider.class)
public class S3RegionSharedFileSourceProvider extends S3SharedFileSourceProvider {

    public S3RegionSharedFileSourceProvider() {
    }

    public S3RegionSharedFileSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                            Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3RegionSharedFileSourceType.TYPE};
    }

    @Override
    public String getService() {
        return S3RegionSharedFileSourceType.SERVICE;
    }

    @Override
    protected String getSharedUrlPrefix() {
        return S3RegionSharedFileSourceType.PREFIX;
    }

    @Override
    protected String getFallbackUrl(String url) {
        String fallBackUrl = S3GlobalSharedFileSourceType.PREFIX + getRelativePath(url);
        return fallBackUrl;
    }
}
