package org.gorpipe.oci.driver;

import org.gorpipe.gor.driver.meta.SourceType;

public class OCIObjectStorageSourceType extends SourceType {
    public static final OCIObjectStorageSourceType OCI_OBJECT_STORAGE = new OCIObjectStorageSourceType();

    private OCIObjectStorageSourceType() {
        super("OCI", true,  "oci:", "oc:", "https:", "http:");
    }

    @Override
    public PRIORITY getPriority() {
        return PRIORITY.HIGH;
    }

    @Override
    public boolean match(String file) {
        return OCIUrl.isOCIUrl(file);
    }
}
