package org.gorpipe.s3.shared;

import com.amazonaws.services.s3.AmazonS3Client;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.PluggableGorDriver;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.s3.driver.S3SourceProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class S3SharedSourceProvider extends S3SourceProvider {

    protected final S3SharedConfiguration s3SharedConfig;

    protected S3SharedSourceProvider() {
        s3SharedConfig = ConfigManager.createPrefixConfig("gor.s3", S3SharedConfiguration.class);
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

    protected String getRelativePath(String url) {
        return url.substring(getSharedUrlPrefix().length());
    }

    /**
     * Get full s3 url (to the actual data).
     */
    private String getFullS3Url(String bucket, String project, String url) {
        Path relativePath = Path.of(getRelativePath(url));
        String fileName = relativePath.getFileName().toString();
        String parentPath = relativePath.getParent() != null ? relativePath.getParent().toString() + "/": "";
        int fileNameDotIndex = fileName.indexOf('.');
        String extraFolder = fileName.substring(0, fileNameDotIndex > 0 ? fileNameDotIndex : fileName.length());
        String fullUrl = String.format("s3://%s/%s/%s%s/%s",
                bucket,
                getBucketPostfix(project),
                parentPath,
                extraFolder,
                fileName);
        return fullUrl;
    }


    protected String getFallbackUrl(String url) {
        return null;
    }

    @Override
    public S3SharedSource resolveDataSource(SourceReference sourceReference)
            throws IOException  {
        S3SharedSource source = null;

        Credentials sharedCreds = getS3DataCredentials(getService(), sourceReference.getSecurityContext());

        if (sharedCreds != null) {
            if (sourceReference.getCommonRoot() == null || sourceReference.getCommonRoot().isEmpty()) {
                throw new GorSystemException("S3 shared resources need to have project root set (that ends with the project name)", null);
            }

            String project = Path.of(sourceReference.getCommonRoot()).getFileName().toString();
            String bucket = sharedCreds.getLookupKey();
            String s3SecurityContext = createS3SecurityContext(sharedCreds);
            String relativePath = getRelativePath(sourceReference.getUrl());

            SourceReference s3SourceReference = createS3SourceReference(sourceReference, project, bucket, s3SecurityContext);

            AmazonS3Client client = getClient(s3SecurityContext, bucket);
            source = new S3SharedSource(client, s3SourceReference, relativePath, s3SharedConfig);

            source.setLinkFile(relativePath + ".link");
            source.setLinkFileContent(sourceReference.getUrl());
        }

        source = handleFallback(sourceReference, source);

        return source;
    }

    private Credentials getS3DataCredentials(String service, String securityContext) {
        BundledCredentials bundledCreds = BundledCredentials.fromSecurityContext(securityContext);
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

    private String createS3SecurityContext(Credentials sharedCreds) {
        BundledCredentials bundledCredentials = new BundledCredentials.Builder().addCredentials(
                        new Credentials("s3", sharedCreds.getLookupKey(), sharedCreds.getOwnerType(),
                                sharedCreds.getOwnerId(), sharedCreds.expires(), sharedCreds.isUserDefault(),
                                (Map<String, String>) sharedCreds.toMap().get("credential_attributes")))
                .build();
        String securityContext = bundledCredentials.addToSecurityContext("");
        return securityContext;
    }

    private SourceReference createS3SourceReference(SourceReference sourceReference, String project, String bucket, String securityContext) {
        String fullUrl = getFullS3Url(bucket, project, sourceReference.getUrl());
        SourceReference updatedSourceReference = new SourceReference(fullUrl, sourceReference, null, securityContext);
        return updatedSourceReference;
    }

    private S3SharedSource handleFallback(SourceReference sourceReference, S3SharedSource source) throws IOException {
        // TODO:  Using fallbacks could be costly.  Check how much it costs and evaluate caching strategies to speed things up.
        if (!s3SharedConfig.useFallback() || sourceReference.isWriteSource() || (source != null && source.exists())) {
            return source;
        }

        SourceReference fallbackSourceReference = createFallbackSourceReference(sourceReference);

        log.warn(String.format("File %s not found at %s, trying fallback %s",
                sourceReference.url,
                getSharedUrlPrefix(),
                fallbackSourceReference != null ? fallbackSourceReference.getUrl() : "None"));

        if (fallbackSourceReference != null) {
            try {
                return (S3SharedSource) PluggableGorDriver.instance().resolveDataSource(fallbackSourceReference);
            } catch (GorResourceException e) {
                throw new GorResourceException(
                        String.format("%s\n%s", e.getMessage(), createErrorMessageForFailure(sourceReference, source)),
                        sourceReference.url);
            }
        }

        throw new GorResourceException(
                String.format("Resource could not be resolved and has no working fallback.\n%s",
                        createErrorMessageForFailure(sourceReference, source)),
                sourceReference.url);
    }

    private String createErrorMessageForFailure(SourceReference sourceReference, S3SharedSource source) {
        if (source == null) {
            return String.format("- Found no creds/bucket for '%s' for project %s", sourceReference.url,
                    sourceReference.commonRoot != null ? Path.of(sourceReference.getCommonRoot()).getFileName() : "Unknown");
        } else {
            return String.format("- File '%s' does not exists", sourceReference.url);
        }
    }

    private SourceReference createFallbackSourceReference(SourceReference sourceReference) {
        String fallbackUrl = getFallbackUrl(sourceReference.getUrl());
        if (fallbackUrl != null) {
            SourceReference updatedSourceReference = new SourceReference(fallbackUrl, sourceReference.securityContext, sourceReference.commonRoot,
                    sourceReference.getLookup(), sourceReference.chrSubset, sourceReference.getLinkSubPath(),
                    sourceReference.isWriteSource());
            return updatedSourceReference;
        }

        return null;
    }
}
