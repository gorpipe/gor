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

package org.gorpipe.client;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Stores result data and associates with a unique key (e.g. query fingerprint) that identifies the data or the operations
 * used to create it.
 */

public interface FileCache extends AutoCloseable {
    /**
     * Check if file exists. Updates file timestamp
     *
     * @param fingerprint File/Query fingerprint
     * @return File path (relative to root) if it exists, null if it does not exist.
     */
    String lookupFile(String fingerprint);

    /**
     * Stores file in cache with a predefined name
     * Repeated calls to store will overwrite the existing file.
     * <p>
     * If cache is accessed through a remote connection, use only 'small' files.
     *
     * @param from        Path to data file.  If cache is local, the file will be moved into the cache location and must not be modified after this call
     *                    If cache is remote, the file is read into memory and sent.  Original file is unchanged.
     *
     * @param fingerprint File/Query fingerprint
     * @param ext         File extension (should include .)
     * @param cost        Measure of cost - can be ms of cpu time needed to recreate the file, download size, etc. Used for WTK.
     * @return New file path
     */
    String store(Path from, String fingerprint, String ext, long cost);

    String storeWithSpecificCacheFilename(Path path, String fingerprint, String cacheFilename, long cost);

    /**
     * Stores a sibling file in cache, tied to the fingerprint so that it will be stored in the same path as the cached file
     * @param from
     * @param fingerprint
     * @return
     */
    String storeSibling(Path from, String fingerprint);

    /**
     * Returns a fully qualified location, including name and extension to where a file should be written
     * before asking the cache to store it.
     * This method makes no attempt to detect if a file is already stored in the cache under the given fingerprint
     *
     * @param fingerprint File/Query fingerprint
     * @param ext         File extension (should include .) and is critical to write out the correct file format
     * @return New file path
     */
    String tempLocation(String fingerprint, String ext);

    /**
     * Lookup multiple files at once.
     *
     * @param fingerprints Fingerprints
     * @param defer        if true, cache update (i.e. touch timestamp) and file existence checks will be deferred (i.e. optimistic).
     * @return File paths, in same positions as fingerprints.  Null entry indicates file not found in cache.
     */
    String[] multiLookup(String[] fingerprints, boolean defer) throws IOException;

}
