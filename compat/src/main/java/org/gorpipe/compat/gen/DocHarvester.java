package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;

import java.util.Collections;
import java.util.List;

/** Placeholder replaced in Task 13 with the real documentation harvester. */
public final class DocHarvester {

    public static final class HarvestResult {
        public final List<CompatCase> cases;
        public final List<String> skipped;

        HarvestResult(List<CompatCase> cases, List<String> skipped) {
            this.cases = Collections.unmodifiableList(cases);
            this.skipped = Collections.unmodifiableList(skipped);
        }
    }

    private DocHarvester() {
    }

    public static HarvestResult harvest() {
        return new HarvestResult(Collections.emptyList(), Collections.emptyList());
    }
}
