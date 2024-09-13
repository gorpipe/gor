package org.gorpipe.s3.driver;

import org.carlspring.cloud.storage.s3fs.S3Factory;
import org.carlspring.cloud.storage.s3fs.S3FileSystem;
import org.carlspring.cloud.storage.s3fs.S3FileSystemProvider;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.util.StringUtil;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.ProviderNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3ClientFileSystemProvider extends S3FileSystemProvider {

    private static S3ClientFileSystemProvider instance;

    public static S3ClientFileSystemProvider getInstance() {
        if (instance == null) {
            instance = new S3ClientFileSystemProvider();
        }
        return instance;
    }

    public S3FileSystem getFileSystem(S3Client client) {
        String key = client.toString(); //Use the client instead of calling: getFileSystemKey(URI.create(String.format("s3://%s@", authority)), loadAmazonProperties());
        return getFilesystems().computeIfAbsent(key, k -> createFileSystemWithClient(k, client));
    }

    private S3FileSystem createFileSystemWithClient(String key, S3Client client) {
        return new S3FileSystem(this, key, client, "");
    }

    public static S3FileSystem getFileSystem(String bucket, String region, String endPoint, String securityContext) {
        URI uri = URI.create(String.format("s3://%s/%s", endPoint, bucket));
        try {
            return (S3FileSystem) FileSystems.getFileSystem(uri);
        } catch (ProviderNotFoundException | FileSystemNotFoundException fsnfe) {
            try {
                Map<String, String> env = new HashMap<>();

                Credentials cred = getCredentials(securityContext, "s3", bucket);
                if (cred != null) {
                    var awsKey = cred.getOrDefault(Credentials.Attr.KEY, "");
                    var awsSecret = cred.getOrDefault(Credentials.Attr.SECRET, "");
                    var sessionToken = cred.getOrDefault(Credentials.Attr.SESSION_TOKEN, "");
                    if (!StringUtil.isEmpty(awsKey)) env.put(S3Factory.ACCESS_KEY, awsKey);
                    if (!StringUtil.isEmpty(awsSecret)) env.put(S3Factory.SECRET_KEY, awsSecret);
                }
                if (!StringUtil.isEmpty(region)) env.put(S3Factory.REGION, region);

                env.put(S3Factory.MAX_CONNECTIONS, "5000");
                env.put(S3Factory.CONNECTION_TIMEOUT, "5000");
                env.put(S3Factory.MAX_ERROR_RETRY, "10");
                env.put(S3Factory.SOCKET_TIMEOUT, "5000");
                return (S3FileSystem) FileSystems.newFileSystem(uri,
                        env,
                        Thread.currentThread().getContextClassLoader());
            } catch (IOException ioe) {
                throw new GorResourceException(bucket, uri.toString(), ioe);
            }
        }
    }

    public static Credentials getCredentials(String securityContext, String service, String key) {
        List<Credentials> creds = BundledCredentials.fromSecurityContext(securityContext).getCredentials(service, key);
        if (!creds.isEmpty()) {
            return creds.get(0);
        }
        return null;
    }

}
