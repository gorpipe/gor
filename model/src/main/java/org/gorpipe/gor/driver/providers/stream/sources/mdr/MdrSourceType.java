package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import org.gorpipe.gor.driver.meta.SourceType;

public class MdrSourceType extends SourceType {
    public static final MdrSourceType MDR = new MdrSourceType();

    private MdrSourceType() {
        super("MDR", true, "mdr:");
    }

    @Override
    public boolean supportsPreparation() {
        return true;
    }
}
