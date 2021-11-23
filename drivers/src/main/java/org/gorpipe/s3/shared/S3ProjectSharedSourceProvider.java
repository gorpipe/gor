package org.gorpipe.s3.shared;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;

import java.util.Set;

@AutoService(SourceProvider.class)
public class S3ProjectSharedSourceProvider extends S3SharedSourceProvider {

    public S3ProjectSharedSourceProvider() {
    }

    public S3ProjectSharedSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                         Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3ProjectSharedSourceType.S3PROJECTSHARED};
    }

    @Override
    public String getService() {
        return S3ProjectSharedSourceType.S3PROJECTSHARED_SERVICE;
    }

    @Override
    protected String getSharedUrlPrefix() {
        return S3ProjectSharedSourceType.S3PROJECTSHARED_PREFIX;
    }
    
    @Override
    protected String getFallbackUrl(String url) {
        String fallBackUrl = S3RegionSharedSourceType.S3REGIONSHARED_PREFIX +  getRelativePath(url);
        return fallBackUrl;
    }
}
