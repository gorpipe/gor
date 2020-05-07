/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.clients;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.gorpipe.client.FileCache;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.FileNature;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;

/**
 * Local implementation of file cache client working directly against the file system. This can be used for non server
 * implementations of file cache.
 */
public class LocalFileCacheClient implements FileCache {

    private static final Logger log = LoggerFactory.getLogger(LocalFileCacheClient.class);

    private Path rootPath;
    private boolean useSubFolders;
    private int subFolderSize;
    private Cache<String, String> cache;

    /**
     * Constructs an instance of local file cache. If sub folders are used the first subFolderSize letters of the
     * fingerprint are used to create a sub folder storing the result file.
     *
     * @param rootPath      File cache location
     * @param useSubFolders Inserts sub folders for fingerprints.
     * @param subFolderSize Insers sub folders for fingerprints.
     */
    public LocalFileCacheClient(Path rootPath, boolean useSubFolders, int subFolderSize) {
        this.rootPath = rootPath;
        this.useSubFolders = useSubFolders;
        this.subFolderSize = subFolderSize;
        this.cache = createCache();
    }

    public LocalFileCacheClient(Path rootPath) {
        this(rootPath, false, 0);
    }

    @Override
    public String lookupFile(String fingerprint) {
        try {
            // Note we do not touch the file here when it is already cached. As the local cache only survives a
            // single run.
            return cache.get(fingerprint, () -> findFileFromFingerPrint(fingerprint));
        } catch (Exception ex) {
            /* Do nothing */
        }

        return null;
    }

    @Override
    public String store(Path from, String fingerprint, String ext, long cost) {
        return storeWithSpecificCacheFilename(from, fingerprint, from.getFileName().toString(), cost);
    }

    @Override
    public String storeWithSpecificCacheFilename(Path path, String fingerprint, String cacheFilename, long cost) {
        try {
            // Atomic only store in cache if successful move operation
            Path subFolder = getFolderFromFingerprint(fingerprint, true);
            Path cacheFile = Paths.get(subFolder.toString(), cacheFilename);
            String resultPath = moveFile(path, cacheFile);

            if (!StringUtils.isEmpty(resultPath)) {
                cache.put(fingerprint, resultPath);
            }

            // If there is no occurance of the fingerprint in the cache file name we need to store a link file to
            // the original file
            if (!cacheFilename.contains(fingerprint)) {
                File md5File = new File(subFolder.toString(), fingerprint + ".md5link");
                FileUtils.writeStringToFile(md5File, resultPath, Charset.defaultCharset());
            }

            return resultPath;
        } catch (IOException ioe) {
            log.error("Error when attempting to store file in cache", ioe);
        }

        return null;
    }

    @Override
    public String storeSibling(Path path, String fingerprint) {
        try {
            String lookupFilePath = lookupFile(fingerprint);
            String lookupFileName = Paths.get(lookupFilePath).getFileName().toString();
            String parentFilePath = Paths.get(lookupFilePath).getParent().toString();
            String fromName = path.getFileName().toString();
            String fromNewName = fingerprint + fromName.substring(fromName.indexOf('.'));
            if (!fromName.equalsIgnoreCase(lookupFileName)
                    && !lookupFileName.equalsIgnoreCase(fromNewName)) {
                Path to = Paths.get(parentFilePath, fromNewName);
                moveFile(path, to);
                return to.toString();
            }

        } catch (IOException ioe) {
            log.error("Error when attempting to store a sibling file in cache", ioe);
        }

        return null;
    }

    @Override
    public String tempLocation(String fingerprint, String ext) {
        try {
            return Paths.get(getFolderFromFingerprint(fingerprint, true).toString(), fingerprint + ext).toString();
        } catch (IOException ioe) {
            log.error("Error when attempting return temp location", ioe);
        }

        return null;
    }

    @Override
    public String[] multiLookup(String[] fingerprints, boolean defer) {
        String[] results = new String[fingerprints.length];

        for (int i = 0; i < fingerprints.length; i++) {
            results[i] = lookupFile(fingerprints[i]);
        }

        return results;
    }

    private Path getFolderFromFingerprint(String fingerprint, boolean createSubFolder) throws IOException {

        if (fingerprint.length() < this.subFolderSize) {
            throw new IllegalArgumentException(String.format("Invalid fingerprint: %1$s, needs to be at least %2$d characters", fingerprint, subFolderSize));
        }

        if (useSubFolders) {
            Path resultPath = Paths.get(rootPath.toString(), fingerprint.substring(0, this.subFolderSize));
            if (createSubFolder) {
                Files.createDirectories(resultPath);
            }
            return resultPath;
        } else {
            return rootPath;
        }
    }

    private Cache<String, String> createCache() {

        RemovalListener<String, String> removalNotifier;
        removalNotifier = notification -> log.debug("Removing file from cache, fingerprint: {}, file: {}, cause: {}",
                notification.getKey(),
                notification.getValue(),
                notification.getCause());

        return CacheBuilder.newBuilder().removalListener(removalNotifier).build();
    }

    private String findFileFromFingerPrint(String fingerprint) throws IOException {
        Path storageFolder = getFolderFromFingerprint(fingerprint, false);

        File dir = new File(storageFolder.toString());
        FileFilter fileFilter = new WildcardFileFilter(fingerprint + "*");
        File[] files = dir.listFiles(fileFilter);
        String foundFile = null;

        if (files != null && files.length > 0) {
            for (File file : files) {
                // We should touch all the files for this fingerprint
                if (foundFile != null) continue;

                DataType dataType = DataType.fromFileName(file.getName());

                if (dataType != null) {
                    if (dataType.nature == FileNature.MD5_LINK) {
                        String md5File = FileUtils.readFileToString(file, Charset.defaultCharset()).trim();
                        foundFile = md5File;
                    } else if (dataType.nature == FileNature.VARIANTS || dataType.nature == FileNature.TABLE) {
                        foundFile = file.toString();
                    }
                }
            }

            if (foundFile == null) {
                log.warn("Found more than one file for fingerprint {} and none of them is a valid data file.", fingerprint);
            }
        }

        return foundFile;
    }

    private String moveFile(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException am) {
            log.warn("Falling back to non-atomic storage ({} -> {})", from.toAbsolutePath(), to.toAbsolutePath());
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }

        return to.toString();
    }

    @Override
    public void close() throws Exception {
        cache.cleanUp();
    }
}
