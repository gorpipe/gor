package org.gorpipe.s3.shared;

import org.gorpipe.gor.driver.meta.SourceType;

public class S3RegionSharedSourceType extends SourceType {
    public static final S3RegionSharedSourceType TYPE = new S3RegionSharedSourceType();
    public static final String SERVICE = "s3region";
    public static final String PREFIX = "s3region://shared/";

    private S3RegionSharedSourceType() {
        super("S3REGIONSHARED", true,  PREFIX);
    }
}
