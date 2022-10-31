package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3GlobalSharedFileSourceType extends SourceType {
    public static final S3GlobalSharedFileSourceType TYPE = new S3GlobalSharedFileSourceType();
    public static final String SERVICE = "s3global";
    public static final String PREFIX = "s3globalfile://shared/";

    private S3GlobalSharedFileSourceType() {
        super("S3GLOBALSHAREDFILE", true, PREFIX);
    }
}
