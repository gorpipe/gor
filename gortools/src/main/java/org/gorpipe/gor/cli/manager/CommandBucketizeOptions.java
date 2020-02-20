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

package org.gorpipe.gor.cli.manager;

import org.gorpipe.gor.manager.BucketManager;
import picocli.CommandLine;

public abstract class CommandBucketizeOptions extends ManagerOptions {


    @CommandLine.Option(names = {"--min_bucket_size"},
            description = "Minimum number of files in a bucket.  Can never be larger than the bucket size.  Default: " + BucketManager.DEFAULT_MIN_BUCKET_SIZE)
    protected int minBucketSize = BucketManager.DEFAULT_MIN_BUCKET_SIZE;

    @CommandLine.Option(names = {"--bucket_size"},
            description = "Preferred number of files in a bucket (effective maximum).  Default: " + BucketManager.DEFAULT_BUCKET_SIZE)
    protected int bucketSize = BucketManager.DEFAULT_BUCKET_SIZE;

    @CommandLine.Option(names = {"-w", "--workers"},
            description = "Number of workers/threads to use.  Default: " + BucketManager.DEFAULT_NUMBER_WORKERS)
    protected int workers = BucketManager.DEFAULT_NUMBER_WORKERS;

    @CommandLine.Option(names = {"-c", "--pack_level"},
            description = "Should we pack/compress the buckets.\n\tNO_PACKING = No packing.\n\tCONSOLIDATE = Merge small buckets into larger ones as needed.\n\tFULL_PACKING = Full packing (rebucketize all small buckets and rebucketize partially deleted buckets)\nDefault: CONSOLIDATE")
    protected BucketManager.BucketPackLevel bucketPackLevel = BucketManager.DEFAULT_BUCKET_PACK_LEVEL;

}
