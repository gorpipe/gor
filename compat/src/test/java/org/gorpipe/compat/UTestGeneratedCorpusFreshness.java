package org.gorpipe.compat;

import org.gorpipe.compat.gen.GenerateMain;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The committed generated corpus must match what the generators produce now.
 *
 * Without this, a command losing a flag would quietly remove its coverage with
 * nothing in the diff to show it.
 */
public class UTestGeneratedCorpusFreshness {

    @Test
    public void committedGeneratedCasesMatchTheGenerators() {
        Map<Path, List<CompatCase>> intended = GenerateMain.generateAll();

        Set<String> intendedIds = new TreeSet<>();
        for (List<CompatCase> cases : intended.values()) {
            intendedIds.addAll(cases.stream().map(c -> c.id).collect(Collectors.toList()));
        }

        Set<String> onDiskIds = CaseLoader.loadAll().stream()
                .filter(CompatCase::isBaseline)
                .map(c -> c.id)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> missing = new ArrayList<>(intendedIds);
        missing.removeAll(onDiskIds);
        List<String> extra = new ArrayList<>(onDiskIds);
        extra.removeAll(intendedIds);

        if (!missing.isEmpty() || !extra.isEmpty()) {
            Assert.fail("The committed generated corpus is stale.\n"
                    + "  missing " + missing.size() + " case(s), e.g. "
                    + missing.stream().limit(5).collect(Collectors.toList()) + "\n"
                    + "  extra " + extra.size() + " case(s), e.g. "
                    + extra.stream().limit(5).collect(Collectors.toList()) + "\n"
                    + "  Run ./gradlew :compat:generate then ./gradlew :compat:accept "
                    + "and commit both.");
        }
    }

    @Test
    public void everyBaselineCaseHasACommittedOutput() {
        List<String> withoutBaseline = new ArrayList<>();
        for (CompatCase c : CaseLoader.loadAll()) {
            if (c.isBaseline() && BaselineStore.load(c) == null) {
                withoutBaseline.add(c.id);
            }
        }
        if (!withoutBaseline.isEmpty()) {
            Assert.fail(withoutBaseline.size() + " baseline case(s) have no committed output, "
                    + "e.g. " + withoutBaseline.stream().limit(5).collect(Collectors.toList())
                    + "\n  Run ./gradlew :compat:accept and commit the result.");
        }
    }
}
