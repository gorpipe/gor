/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.googlecloudstorage.driver;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Google Cloud Storage helper
 * Created by simmi on 20/02/18.
 */
public class GoogleCloudStorageBlobHelper {
    public static final String GOOGLECLOUDSTORAGEPREFIX = "gs://";
    private static final Storage googlecloudstorage = StorageOptions.getDefaultInstance().getService();

    public static Storage getGoogleCloudStorage() {
        return googlecloudstorage;
    }

    public static String[] parseUrl(String url) {
        if (!url.toLowerCase().startsWith(GOOGLECLOUDSTORAGEPREFIX)) {
            throw new IllegalArgumentException("gs url '" + url + " does not start with " + GOOGLECLOUDSTORAGEPREFIX);
        }
        String[] bucketKey = url.substring(GOOGLECLOUDSTORAGEPREFIX.length()).split("/", 2);
        if (bucketKey.length != 2) {
            throw new IllegalArgumentException("gs url '" + url + " does not parse into bucket and path");
        }
        return bucketKey;
    }

    public static String makeUrl(String bucket, String key) {
        return GOOGLECLOUDSTORAGEPREFIX + bucket + "/" + key;
    }
}
