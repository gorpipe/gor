package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3ProjectDataFileSourceType extends SourceType {
    public static final S3ProjectDataFileSourceType TYPE = new S3ProjectDataFileSourceType();
    public static final String SERVICE = "s3data";
    public static final String PREFIX = "s3datafile://project/";

    private S3ProjectDataFileSourceType() {
        super("S3PROJECTDATAFILE", true, PREFIX);
    }
}
