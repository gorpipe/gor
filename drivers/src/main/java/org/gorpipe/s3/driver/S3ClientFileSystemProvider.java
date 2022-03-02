package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.AmazonS3;
import com.upplication.s3fs.AmazonS3Client;
import com.upplication.s3fs.S3FileSystemProvider;
import com.upplication.s3fs.S3FileSystem;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Map;

public class S3ClientFileSystemProvider extends S3FileSystemProvider {

    /**
     * Create the fileSystem
     *
     * @param uri   URI
     * @param props Properties
     * @param client AmazonS3
     * @return S3FileSystem never null
     */
    public FileSystem createFileSystem(URI uri, Map<String, ?> props, AmazonS3 client) {
        return new S3FileSystem(this, new AmazonS3Client(client), uri.getHost());
    }

    // We we are not constructing the FileSystem as expected (using the newFileSystem method) as we
    // want to reuse the client (could change that and stop reusing the client and pass in creds in the props
    // var)
    protected boolean isAES256Enabled() {
        try {
            return super.isAES256Enabled();
        } catch (NullPointerException npe) {
            // ignore
        }
        return false;
    }
}
