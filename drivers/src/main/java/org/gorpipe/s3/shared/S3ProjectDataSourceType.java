package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3ProjectDataSourceType extends SourceType {
    public static final S3ProjectDataSourceType TYPE = new S3ProjectDataSourceType();
    public static final String SERVICE = "s3data";
    public static final String PREFIX = "s3data://project/";

    private S3ProjectDataSourceType() {
        super("S3PROJECTDATA", true, PREFIX);
    }
}
