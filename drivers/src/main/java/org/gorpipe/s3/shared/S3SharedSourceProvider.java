package org.gorpipe.s3.shared;

import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.s3.driver.S3SourceProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class S3SharedSourceProvider extends S3SourceProvider {

    protected final S3SharedConfiguration s3SharedConfig;

    protected S3SharedSourceProvider() {
        s3SharedConfig = ConfigManager.getPrefixConfig("gor.s3", S3SharedConfiguration.class);
    }

    protected S3SharedSourceProvider(GorDriverConfig config, S3SharedConfiguration s3Config, FileCache cache,
                                  Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, s3Config, cache, initialFactories);
        this.s3SharedConfig = s3Config;
    }

    public abstract String getService();

    protected abstract String getSharedUrlPrefix();


    protected String getBucketPostfix(String project) {
        return "shared";
    }


    protected String getRelativePath(S3SharedSource source) {
        return getRelativePath(source.getSourceReference().getParentSourceReference().getUrl());
    }

    protected String getRelativePath(String url) {
        return url.startsWith(getSharedUrlPrefix()) ? url.substring(getSharedUrlPrefix().length()) : url;
    }

    /**
     * Get full s3 url (to the actual data).
     */
    private String getFullS3Url(String bucket, String project, String url) {
        Path relativePath = Path.of(getRelativePath(url));
        String fileName = relativePath.getFileName().toString() + (url.endsWith("/") ? "/" : "");
        String parentPath = relativePath.getParent() != null ? relativePath.getParent() + "/": "";
        String fullParentUrl = String.format("s3://%s/%s/%s",
                bucket,
                getBucketPostfix(project),
                parentPath);
        String fullUrl = String.format("%s%s%s",
                fullParentUrl,
                getExtraFolder(fileName),
                fileName);
        return fullUrl;
    }

    protected String getExtraFolder(String fileName) {
        String extraFolder = "";
        int fileNameDotIndex = fileName.indexOf('.');
        if (!fileName.endsWith("/") && fileNameDotIndex > 0 && fileNameDotIndex < fileName.length() - 1) {
            // Only add the extra folder if not folder (ends with /) and it has actual suffix (first dot is not first or last)
            extraFolder = fileName.substring(0, fileNameDotIndex > 0 ? fileNameDotIndex : fileName.length()) + "/";
        }
        return extraFolder;
    }


    protected String getFallbackUrl(String url) {
        return null;
    }

    @Override
    public S3SharedSource resolveDataSource(SourceReference sourceReference)
            throws IOException  {
        if (sourceReference.getCommonRoot() == null || sourceReference.getCommonRoot().isEmpty()) {
            throw new GorSystemException("S3 shared resources need to have project root set (that ends with the project name)", null);
        }

        S3SharedSource source = null;

        BundledCredentials bundledCreds = BundledCredentials.fromSecurityContext(sourceReference.securityContext);
        Credentials sharedCreds = getS3DataCredentials(getService(), bundledCreds );

        if (sharedCreds == null) {
            log.warn(String.format("No credentials found for %s. Returning emtpy source.", getService()));
            return source;
        }

        String s3SecurityContext = createSecurityContext(createS3CredsFromShared(sharedCreds), bundledCreds);
        String relativePath = getRelativePath(sourceReference.getUrl());

        String project = Path.of(sourceReference.getCommonRoot()).getFileName().toString();
        String bucket = sharedCreds.getLookupKey();
        SourceReference s3SourceReference = createS3SourceReference(sourceReference, project, bucket, s3SecurityContext);

        S3Client client = getClient(s3SecurityContext, bucket);
        S3AsyncClient asyncClient = getAsyncClient(s3SecurityContext, bucket);
        source = new S3SharedSource(client, asyncClient, s3SourceReference, relativePath, s3SharedConfig);
        //source.setFileSystem(S3ClientFileSystemProvider.getInstance().getFileSystem(s3SecurityContext, bucket));
        updateSharedSourceLink(source, project);

        source = handleFallback(sourceReference, source);

        return source;
    }

    protected void updateSharedSourceLink(S3SharedSource source, String project) {
        source.setProjectLinkFile(DataUtil.toLink(getRelativePath(source)));
        source.setProjectLinkFileContent(findSharedSourceLinkContent(source));
    }

    protected String findSharedSourceLinkContent(S3SharedSource source) {
        if (s3SharedConfig.useHighestTypeInLinks()) {
            return S3ProjectSharedSourceType.PREFIX + getRelativePath(source);
        } else {
            return getSharedUrlPrefix() + getRelativePath(source);
        }
    }

    private Credentials getS3DataCredentials(String service, BundledCredentials bundledCreds) {
        List<Credentials> credsList = bundledCreds.getCredentialsForService(service);
        return getHighestPriorityCredential(credsList, service);
    }

    // Note:  We are assuming that credentials owner has been taking into account when assembling the credslist.
    private Credentials getHighestPriorityCredential(List<Credentials> credsList, String service) {
        if (credsList.size() == 0) {
            log.error(String.format("No credentials found for %s", service));
            return null;
            //throw new GorSystemException(String.format("No credentials found for %s", service), null);
        }
        Credentials bestMatch = credsList.get(0);
        for (int i = 1; i < credsList.size(); i++) {
            Credentials candidate = credsList.get(i);
            if (bestMatch.getOwnerType().ordinal() > candidate.getOwnerType().ordinal()) {
                bestMatch = candidate;
            }
        }
        
        return bestMatch;
    }

    private String createSecurityContext(Credentials s3Creds, BundledCredentials bundledCreds) {
        BundledCredentials.Builder builder = new BundledCredentials.Builder().addCredentials(s3Creds);
        for (String services : bundledCreds.services()) {
            for (Credentials cred : bundledCreds.getCredentialsForService(services)) {
                builder.addCredentials(cred);
            }
        }

        return builder.build().addToSecurityContext("");
    }

    private Credentials createS3CredsFromShared(Credentials sharedCreds) {
        return new Credentials("s3", sharedCreds.getLookupKey(), sharedCreds.getOwnerType(),
                sharedCreds.getOwnerId(), sharedCreds.expires(), sharedCreds.isUserDefault(),
                (Map<String, String>) sharedCreds.toMap().get("credential_attributes"));
    }

    private SourceReference createS3SourceReference(SourceReference sourceReference, String project, String bucket, String securityContext) {
        String fullUrl = getFullS3Url(bucket, project, sourceReference.getUrl());
        SourceReference updatedSourceReference = new SourceReference(fullUrl, sourceReference, null, securityContext);
        return updatedSourceReference;
    }

    private S3SharedSource handleFallback(SourceReference sourceReference, S3SharedSource source) throws IOException {
        // TODO:  Using fallbacks could be costly.  Check how much it costs and evaluate caching strategies to speed things up.
        if (!s3SharedConfig.useFallback() || !sourceReference.isFallback() || sourceReference.isWriteSource() || (source != null && wrap(source).exists())) {
            // Have a valid source, or we don't support fallback for this source.
            return source;
        }

        // Try to find a fallback source.

        SourceReference fallbackSourceReference = createFallbackSourceReference(sourceReference);

        log.warn(String.format("File %s not found at %s, trying fallback %s",
                sourceReference.url,
                getSharedUrlPrefix(),
                fallbackSourceReference != null ? fallbackSourceReference.getUrl() : "None"));

        if (fallbackSourceReference != null) {
            try {
                S3SharedSource fallbackSource = (S3SharedSource) PluggableGorDriver.instance().resolveDataSource(fallbackSourceReference);
                source = fallbackSource != null && fallbackSource.exists() ? fallbackSource : source;
            } catch (GorResourceException e) {
                throw new GorResourceException(
                        String.format("%s\n%s", e.getMessage(), createErrorMessageForFailure(sourceReference, source)),
                        sourceReference.url, e);
            }
        }

        return source;
    }

    private String createErrorMessageForFailure(SourceReference sourceReference, S3SharedSource source) {
        if (source == null) {
            return String.format("- Found no creds/bucket for '%s'", sourceReference.url);
        } else {
            return String.format("- File '%s' does not exists", sourceReference.url);
        }
    }

    private SourceReference createFallbackSourceReference(SourceReference sourceReference) {
        String fallbackUrl = getFallbackUrl(sourceReference.getUrl());
        if (fallbackUrl != null) {
            return new SourceReference(fallbackUrl, sourceReference);
        }

        return null;
    }
}
