package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3ProjectSharedProjectSourceType extends SourceType {
    public static final S3ProjectSharedProjectSourceType S3PROJECTSHAREDPROJECT = new S3ProjectSharedProjectSourceType();
    public static final String S3PROJECTSHAREDPROJECT_SERVICE = "s3data";
    public static final String S3PROJECTSHAREDPROJECT_PREFIX = "s3data://shared-project/";

    private S3ProjectSharedProjectSourceType() {
        super("S3PROJECTSHAREDPROJECT", true,  S3PROJECTSHAREDPROJECT_PREFIX);
    }
}
