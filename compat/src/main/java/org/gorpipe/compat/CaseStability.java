package org.gorpipe.compat;

/**
 * Screens out cases whose output is not reproducible.
 *
 * CaseRunner already puts ${ROOT} back wherever the temporary project root
 * appears, but some output depends on the root without quoting it: PIPESTEPS
 * reports the *length* of a path, which differs between runs and between
 * machines. A committed baseline for such a case could never match, and a
 * permanently failing case teaches reviewers to ignore the suite.
 *
 * Detected by running the case twice — each run gets a fresh temporary root — and
 * comparing the recorded form. This catches environment sensitivity that no
 * static inspection of the query could.
 */
public final class CaseStability {

    private CaseStability() {
    }

    private static final String SHORT_ROOT = "gor-compat-";
    private static final String LONG_ROOT = "gor-compat-deliberately-longer-root-name-";

    public static boolean isReproducible(CompatCase c) {
        String first = BaselineStore.render(CaseRunner.run(c, SHORT_ROOT));
        String second = BaselineStore.render(CaseRunner.run(c, LONG_ROOT));
        if (!first.equals(second)) {
            return false;
        }
        String third = BaselineStore.render(CaseRunner.run(c, SHORT_ROOT));
        return first.equals(third);
    }
}
