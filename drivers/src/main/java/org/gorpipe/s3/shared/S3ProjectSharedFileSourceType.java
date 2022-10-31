package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3ProjectSharedFileSourceType extends SourceType {
    public static final S3ProjectSharedFileSourceType TYPE = new S3ProjectSharedFileSourceType();
    public static final String SERVICE = "s3data";
    public static final String PREFIX = "s3datafile://shared/";

    private S3ProjectSharedFileSourceType() {
        super("S3PROJECTSHAREDFILE", true, PREFIX);
    }
}
