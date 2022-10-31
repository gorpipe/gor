package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3ProjectSharedSourceType extends SourceType {
    public static final S3ProjectSharedSourceType TYPE = new S3ProjectSharedSourceType();
    public static final String SERVICE = "s3data";
    public static final String PREFIX = "s3data://shared/";

    private S3ProjectSharedSourceType() {
        super("S3PROJECTSHARED", true, PREFIX);
    }
}
