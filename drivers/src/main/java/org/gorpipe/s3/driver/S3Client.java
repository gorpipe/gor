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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Replace AmazonS3Client to make client configuration testable.
 * <p>
 * Created by villi on 08/04/17.
 */
public class S3Client extends AmazonS3Client {

    public S3Client(AWSCredentials defaultAWSCredentialsProviderChain, ClientConfiguration clientconfig) {
        super(defaultAWSCredentialsProviderChain, clientconfig);
    }

    public S3Client(AWSCredentialsProviderChain chain, ClientConfiguration clientconfig) {
        super(chain, clientconfig);
    }

    public S3Client(AWSCredentials creds) {
        super(creds);
    }


}
