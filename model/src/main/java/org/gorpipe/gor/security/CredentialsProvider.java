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

package org.gorpipe.gor.security;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by villi on 30/03/16.
 */
public interface CredentialsProvider {
    /**
     * Get credentials that can be used for a service/lookup key pair.
     *
     * @param service   Service name (e.g. s3, dx).  Case insensitive - downcase preferred
     * @param lookupKey Lookup key (e.g. s3 bucket). A lookup key of null can be used to request default credentials if e.g. project is not known.  Case insensitive - downcase preferred
     * @return List of credentials. If more than one it should start with the most preferred one.
     */
    Collection<Credentials> getCredentials(String service, String lookupKey) throws IOException;
}
