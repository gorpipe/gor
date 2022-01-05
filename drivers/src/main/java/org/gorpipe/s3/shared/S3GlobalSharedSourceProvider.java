package org.gorpipe.s3.shared;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;

import java.util.Set;

@AutoService(SourceProvider.class)
public class S3GlobalSharedSourceProvider extends S3SharedSourceProvider {

    public S3GlobalSharedSourceProvider() {
    }

    public S3GlobalSharedSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                        Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3GlobalSharedSourceType.S3GLOBALSHARED};
    }

    @Override
    public String getService() {
        return S3GlobalSharedSourceType.S3GLOBALSHARED_SERVICE;
    }

    @Override
    protected String getSharedUrlPrefix() {
        return S3GlobalSharedSourceType.S3GLOBALSHARED_PREFIX;
    }
}
