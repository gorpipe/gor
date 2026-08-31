package org.gorpipe.compat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Every spec case, each as its own named JUnit case. These gate the build:
 * a spec case is a claim that GOR behaves a particular way, backed by a citation.
 */
@RunWith(Parameterized.class)
public class UTestSpecSuite {

    @Parameterized.Parameter
    public CompatCase compatCase;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<>();
        for (CompatCase c : CaseLoader.loadAll()) {
            if (c.isSpec()) {
                params.add(new Object[]{c});
            }
        }
        return params;
    }

    @Test
    public void specCaseHoldsItsContract() {
        CaseRunner.assertSpec(compatCase);
    }
}
