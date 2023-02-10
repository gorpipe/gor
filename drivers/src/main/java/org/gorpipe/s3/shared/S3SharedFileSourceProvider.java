package org.gorpipe.s3.shared;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;

import java.util.Set;

public abstract class S3SharedFileSourceProvider extends S3SharedSourceProvider {

    public S3SharedFileSourceProvider() {
    }

    public S3SharedFileSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                      Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }


    @Override
    protected String getExtraFolder(String fileName) {
        return "";
    }

    @Override
    protected String findSharedSourceLinkContent(S3SharedSource source) {
        if (s3SharedConfig.useHighestTypeInLinks()) {
            return S3ProjectSharedFileSourceType.PREFIX + getRelativePath(source);
        } else {
            return getSharedUrlPrefix() + getRelativePath(source);
        }
    }


}
