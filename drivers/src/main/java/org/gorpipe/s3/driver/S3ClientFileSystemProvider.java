package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.AmazonS3;
import com.upplication.s3fs.AmazonS3Client;
import com.upplication.s3fs.S3FileSystemProvider;
import com.upplication.s3fs.S3FileSystem;

import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.util.concurrent.ConcurrentHashMap;

public class S3ClientFileSystemProvider extends S3FileSystemProvider {

    final static ConcurrentHashMap<AmazonS3, S3FileSystem> fileSystems = new ConcurrentHashMap<>();

    private static S3ClientFileSystemProvider instance;

    public static S3ClientFileSystemProvider getInstance() {
        if (instance == null) {
            instance = new S3ClientFileSystemProvider();
        }
        return instance;
    }


    public  S3FileSystem getFileSystem(AmazonS3 client) {
        S3FileSystem fileSystem = fileSystems.get(client);

        if (fileSystem == null) {
            throw new FileSystemNotFoundException(
                    String.format("S3 filesystem not yet created. Use newFileSystem() instead"));
        }

        return fileSystem;
    }

    /**
     * Create the fileSystem
     *
     * @param client AmazonS3
     * @return S3FileSystem never null
     */
    public S3FileSystem createFileSystem(AmazonS3 client, String endPoint) {
        return new S3FileSystem(this, new AmazonS3Client(client), endPoint);
    }

    public FileSystem newFileSystem(AmazonS3 client, String endPoint) {
        if (fileSystems.contains(client)) {
            throw new FileSystemAlreadyExistsException(
                    "S3 filesystem already exists. Use getFileSystem() instead");
        }

        S3FileSystem result = createFileSystem(client, endPoint);
        fileSystems.put(client, result);

        return result;
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
