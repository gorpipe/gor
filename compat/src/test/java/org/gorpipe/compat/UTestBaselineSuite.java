package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Every baseline case, diffed against its committed output.
 *
 * A missing baseline is a failure, not a pass: it means a case was generated and
 * nobody recorded what the engine does with it.
 */
@RunWith(Parameterized.class)
public class UTestBaselineSuite {

    @Parameterized.Parameter
    public CompatCase compatCase;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        for (CompatCase c : CaseLoader.loadAll()) {
            if (c.isBaseline()) {
                params.add(new Object[]{c});
            }
        }
        return params;
    }

    @Test
    public void outputMatchesCommittedBaseline() {
        String committed = BaselineStore.load(compatCase);
        if (committed == null) {
            Assert.fail("[" + compatCase.id + "] has no committed baseline in "
                    + BaselineStore.pathFor(compatCase)
                    + "\n  This case is new. Review what the engine does with it, then run:"
                    + "\n    ./gradlew :compat:accept");
        }

        String actual = BaselineStore.render(CaseRunner.run(compatCase));
        if (!committed.equals(actual)) {
            Assert.fail("[" + compatCase.id + "] behaviour changed"
                    + "\n  query: " + compatCase.query
                    + "\n--- committed ---\n" + committed.replace("\t", "<TAB>")
                    + "--- now ---\n" + actual.replace("\t", "<TAB>")
                    + "\n  If this change is intended, run ./gradlew :compat:accept and"
                    + " commit the rewritten baselines so the diff is reviewable.");
        }
    }
}
