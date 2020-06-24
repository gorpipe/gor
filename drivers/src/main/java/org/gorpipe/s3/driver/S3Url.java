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

package org.gorpipe.s3.driver;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * Handle s3 urls.
 * <p>
 * Supported formats:
 * <pre>
 *     s3://<bucket>/<path>/<to>/<file>
 *     s3://<provider>:<bucket>/<path>/<to>/<file>
 *
 * In the former case, the lookup key (e.g. to find credentials) is just the bucket name
 * In the latter case, it is <provider>:<bucket>
 *
 * </pre>
 * <p>
 * Created by villi on 03/04/17.
 */
public class S3Url {

    private String bucket;
    private String lookupKey;
    private String path;

    public static S3Url parse(String url) throws MalformedURLException {
        URI uri = URI.create(url);
        return parse(uri);
    }

    public static S3Url parse(URI uri) throws MalformedURLException {
        if (!uri.getScheme().equalsIgnoreCase("s3")) {
            throw new MalformedURLException("Expected scheme to be s3 in url: " + uri);
        }
        S3Url result = new S3Url();
        result.lookupKey = uri.getAuthority().toLowerCase();
        if (result.lookupKey.contains(":")) {
            String[] bucketParts = result.lookupKey.split(":");
            if (bucketParts.length != 2) {
                throw new MalformedURLException("Only one instance of : allowed in s3 url: " + uri);
            }
            result.bucket = bucketParts[1];
        } else {
            result.bucket = result.lookupKey;
        }
        result.path = uri.getPath();
        if (result.path.startsWith("/")) {
            result.path = result.path.substring(1);
        }
        return result;
    }

    public String getBucket() {
        return bucket;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getPath() {
        return path;
    }
}
