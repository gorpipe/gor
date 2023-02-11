package org.gorpipe.s3.shared;

import com.google.auto.service.AutoService;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;

import java.util.Set;

@AutoService(SourceProvider.class)
public class S3ProjectSharedProjectSourceProvider extends S3SharedSourceProvider {

    public S3ProjectSharedProjectSourceProvider() {
    }

    public S3ProjectSharedProjectSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                                Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{S3ProjectSharedProjectSourceType.TYPE};
    }

    @Override
    public String getService() {
        return S3ProjectSharedProjectSourceType.SERVICE;
    }

    @Override
    protected String getSharedUrlPrefix() {
        return S3ProjectSharedProjectSourceType.PREFIX;
    }
    
    @Override
    protected String getFallbackUrl(String url) {
        return S3RegionSharedSourceType.PREFIX +  getRelativePath(url);
    }

    @Override
    protected void updateSharedSourceLink(S3SharedSource source, String project) {
        source.setProjectLinkFile(DataUtil.toFile(PathUtils.stripTrailingSlash(getRelativePath(source)), DataType.LINK));

        source.setProjectLinkFileContent(String.format("%sprojects/%s/%s",
                S3ProjectSharedSourceType.PREFIX, project, getRelativePath(source)));
    }

    @Override
    protected String getBucketPostfix(String project) {
        if (project == null || project.isEmpty() || ".".equals(project)) {
            throw new GorResourceException(
                    String.format("Project must be set for project based shared s3 folders (%s)",
                            S3ProjectSharedProjectSourceType.PREFIX), null);
        }
        return String.format("shared/projects/%s", project);
    }
}
