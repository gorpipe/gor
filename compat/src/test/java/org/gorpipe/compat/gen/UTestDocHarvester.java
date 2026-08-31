package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLint;
import org.gorpipe.compat.CompatCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;

public class UTestDocHarvester {

    private static DocHarvester.HarvestResult result;

    @BeforeClass
    public static void harvest() {
        result = DocHarvester.harvest();
    }

    @Test
    public void findsTheDocumentationTree() {
        Assert.assertTrue("documentation/src not found at " + DocHarvester.docRoot(),
                Files.isDirectory(DocHarvester.docRoot()));
    }

    @Test
    public void harvestsSelfContainedSnippets() {
        // 568 gor code blocks exist across 240 files; the self-contained ones are
        // those rooted in gorrow or norrows.
        Assert.assertFalse("expected at least one harvested snippet", result.cases.isEmpty());
        for (CompatCase c : result.cases) {
            String q = c.query.toLowerCase();
            Assert.assertTrue("harvested query is not self-contained: " + c.query,
                    q.startsWith("gorrow") || q.startsWith("norrows")
                            || q.startsWith("nor ") || q.startsWith("gor "));
        }
    }

    @Test
    public void skipsNonHermeticSnippetsWithAReason() {
        Assert.assertFalse("expected some snippets to be skipped", result.skipped.isEmpty());
        for (String entry : result.skipped) {
            Assert.assertTrue("skip entry must give a reason: " + entry, entry.contains(":"));
        }
    }

    @Test
    public void skipsSnippetsReferencingProjectData() {
        // Snippets using #dbsnp# and similar cannot run hermetically.
        Assert.assertTrue("expected project-data snippets to be reported as skipped",
                result.skipped.stream().anyMatch(s -> s.contains("#")));
    }

    @Test
    public void harvestedCasesAreBaselineTierAndWellFormed() {
        for (CompatCase c : result.cases) {
            Assert.assertEquals("baseline", c.tier);
            Assert.assertNull(c.expected);
            Assert.assertTrue("id violates the required pattern: " + c.id,
                    c.id.matches("^[a-z0-9]+(\\.[a-z0-9_]+)+$"));
            Assert.assertTrue("harvested case should cite its page",
                    c.behavior != null && c.behavior.contains(".rst"));
        }
    }

    @Test
    public void harvestedCasesPassCorpusLint() {
        // The docs contain snippets built on random(), whose output differs on
        // every run. Harvesting one produces a baseline that changes on its own,
        // which is precisely what lint exists to prevent, so the harvester screens
        // for the same properties rather than leaving them to fail later.
        java.util.List<String> violations = CaseLint.check(result.cases);
        Assert.assertTrue("harvested cases violate corpus lint:\n  "
                + String.join("\n  ", violations), violations.isEmpty());
    }

    @Test
    public void skipsNonDeterministicSnippets() {
        Assert.assertTrue("expected random()-based snippets to be reported as skipped",
                result.skipped.stream().anyMatch(s -> s.contains("non-deterministic")));
    }

    @Test
    public void harvestedIdsAreUnique() {
        long distinct = result.cases.stream().map(c -> c.id).distinct().count();
        Assert.assertEquals(result.cases.size(), distinct);
    }
}
