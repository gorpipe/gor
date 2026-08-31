package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UTestCorpusLint {

    @Test
    public void corpusPassesLint() {
        List<String> violations = CaseLint.check(CaseLoader.loadAll());
        if (!violations.isEmpty()) {
            Assert.fail("Corpus lint violations (" + violations.size() + "):\n  "
                    + String.join("\n  ", violations));
        }
    }
}
