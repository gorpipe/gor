package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3RegionSharedFileSourceType extends SourceType {
    public static final S3RegionSharedFileSourceType TYPE = new S3RegionSharedFileSourceType();
    public static final String SERVICE = "s3region";
    public static final String PREFIX = "s3regionfile://shared/";

    private S3RegionSharedFileSourceType() {
        super("S3REGIONSHAREDFILE", true,  PREFIX);
    }
}
