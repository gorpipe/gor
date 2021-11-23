package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3ProjectSharedSourceType extends SourceType {
    public static final S3ProjectSharedSourceType S3PROJECTSHARED = new S3ProjectSharedSourceType();
    public static final String S3PROJECTSHARED_SERVICE = "s3data";
    public static final String S3PROJECTSHARED_PREFIX = "s3data://shared/";

    private S3ProjectSharedSourceType() {
        super("S3PROJECTSHARED", true,  S3PROJECTSHARED_PREFIX);
    }
}
