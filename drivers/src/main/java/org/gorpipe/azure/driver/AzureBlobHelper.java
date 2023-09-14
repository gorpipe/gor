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

package org.gorpipe.azure.driver;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import org.gorpipe.exceptions.GorSystemException;

/**
 * S3 helper
 * Created by simmi on 10/09/15.
 */
public class AzureBlobHelper {
    public static final String AZUREPREFIX = "az://";
    private static BlobClient azure;




    public static BlobClient getAzure(String bucket, String path) {
            String name = System.getProperty("gor.azure.account.name");
            String key = System.getProperty("gor.azure.account.key");

            if (name == null) {
                name = System.getenv("AZURE_STORAGE_ACCOUNT");
                key = System.getenv("AZURE_STORAGE_ACCESS_KEY");
            }

            if (name == null) {
                throw new GorSystemException("No azure account name specified", null);
            }

            return new BlobClientBuilder()
                    .connectionString("DefaultEndpointsProtocol=https;AccountName=" + name + ";AccountKey=" + key + ";EndpointSuffix=core.windows.net")
                    .containerName(bucket)
                    .blobName(path)
                    .buildClient();

    }

    public static String[] parseUrl(String url) {
        if (!url.toLowerCase().startsWith(AZUREPREFIX)) {
            throw new IllegalArgumentException("azure url '" + url + " does not start with " + AZUREPREFIX);
        }
        String[] bucketKey = url.substring(AZUREPREFIX.length()).split("/", 2);
        if (bucketKey.length != 2) {
            throw new IllegalArgumentException("azure url '" + url + " does not parse into bucket and path");
        }
        return bucketKey;
    }

    public static String makeUrl(String bucket, String key) {
        return AZUREPREFIX + bucket + "/" + key;
    }
}
