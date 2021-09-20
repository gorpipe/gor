package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.AmazonS3;
import com.upplication.s3fs.S3FileSystemProvider;
import com.upplication.s3fs.S3FileSystem;

import java.net.URI;
import java.util.Properties;

public class S3ClientFileSystemProvider extends S3FileSystemProvider {

    /**
     * Create the fileSystem
     *
     * @param uri   URI
     * @param props Properties
     * @param client AmazonS3
     * @return S3FileSystem never null
     */
    public S3FileSystem createFileSystem(URI uri, Properties props, AmazonS3 client) {
        return new S3FileSystem(this, getFileSystemKey(uri, props), client, uri.getHost());
    }
}
