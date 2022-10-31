package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3GlobalSharedSourceType extends SourceType {
    public static final S3GlobalSharedSourceType TYPE = new S3GlobalSharedSourceType();
    public static final String SERVICE = "s3global";
    public static final String PREFIX = "s3global://shared/";

    private S3GlobalSharedSourceType() {
        super("S3GLOBALSHARED", true, PREFIX);
    }
}
