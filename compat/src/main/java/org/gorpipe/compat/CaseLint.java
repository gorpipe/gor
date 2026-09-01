package org.gorpipe.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structural checks over the corpus.
 *
 * These run over both tiers. A baseline case that embeds wall-clock time produces
 * a baseline that changes on its own, which would train reviewers to accept diffs
 * without reading them — the failure mode this suite most needs to avoid.
 */
public final class CaseLint {

    /**
     * Constructs whose output depends on machine state. A case using any of these
     * cannot have a stable expectation, so it is rejected rather than left to fail
     * intermittently later.
     */
    private static final String[] NON_DETERMINISTIC = {
            // Clock and randomness.
            "random(", "rand(", "now(", "currentdate", "curdate", "today(",
            "gettime", "systime", "timestamp(", "time(", "date(", "edate(",
            // Machine identity.
            "hostname(", "threadid(", "availcpu(",
            // JVM and host state. These are stable within a single JVM, which is
            // why the runtime reproducibility probe cannot see them: the probe
            // runs both attempts in one process, while the suite runs in another.
            "maxmem(", "totalmem(", "freemem(", "free(", "openfiles(", "maxfiles(",
            "cpuload(", "syscpuload(",
            // Timing measurements of the host's storage.
            "avgseektimemillis(", "randomaccesstiming(",
            // Depends on files in the project rather than on the query.
            "fileinfo(", "timesignature("
    };

    /**
     * Absolute paths into machine-specific locations. ${ROOT} is stripped before
     * matching, so the per-case project root is allowed.
     */
    private static final Pattern ABSOLUTE_PATH =
            Pattern.compile("(?<![\\w$])/(?:Users|home|tmp|var|opt|mnt|private)/");

    private CaseLint() {
    }

    public static List<String> check(List<CompatCase> cases) {
        List<String> violations = new ArrayList<>();
        Map<String, String> bodies = new HashMap<>();

        for (CompatCase c : cases) {
            checkTierInvariants(c, violations);
            checkDeterminism(c, violations);
            checkAbsolutePaths(c, violations);
            checkInputsReferenced(c, violations);
            checkDuplicateBody(c, bodies, violations);
        }
        return violations;
    }

    private static void checkTierInvariants(CompatCase c, List<String> violations) {
        if (c.isSpec()) {
            if (c.cites == null || c.cites.isEmpty()) {
                violations.add("[" + c.id + "] spec case has no citation; a spec case must say "
                        + "where its expectation came from");
            }
            if ("exact".equals(c.mode) && (c.expected == null || c.expected.isEmpty())) {
                violations.add("[" + c.id + "] spec case in exact mode has no expected output");
            }
        }
        if (c.isBaseline() && c.expected != null) {
            violations.add("[" + c.id + "] baseline case carries an inline expected block; "
                    + "baseline outputs belong in baselines/");
        }
    }

    /**
     * The non-deterministic construct a query uses, or null when it uses none.
     *
     * Public so that generators can screen a candidate query before emitting it:
     * a case that trips this rule must never reach the corpus, and the rule should
     * be stated in exactly one place.
     */
    public static String nonDeterministicToken(String query) {
        String q = query.toLowerCase(Locale.ROOT).replace(" ", "");
        for (String token : NON_DETERMINISTIC) {
            if (q.contains(token)) {
                return token;
            }
        }
        return null;
    }

    private static void checkDeterminism(CompatCase c, List<String> violations) {
        String token = nonDeterministicToken(c.query);
        if (token != null) {
            violations.add("[" + c.id + "] query uses the non-deterministic construct '"
                    + token + "'");
        }
    }

    private static void checkAbsolutePaths(CompatCase c, List<String> violations) {
        Matcher m = ABSOLUTE_PATH.matcher(c.query.replace("${ROOT}", ""));
        if (m.find()) {
            violations.add("[" + c.id + "] query contains an absolute path outside ${ROOT}: "
                    + m.group());
        }
    }

    private static void checkInputsReferenced(CompatCase c, List<String> violations) {
        for (CompatInput in : c.inputs) {
            if (!c.query.contains(in.path)) {
                violations.add("[" + c.id + "] declares the input '" + in.path
                        + "' which the query never references");
            }
        }
    }

    private static void checkDuplicateBody(CompatCase c, Map<String, String> bodies,
                                           List<String> violations) {
        StringBuilder key = new StringBuilder();
        key.append(c.query).append(' ').append(c.mode)
                .append(' ').append(c.expected == null ? "" : c.expected);
        for (CompatInput in : c.inputs) {
            key.append(' ').append(in.path).append('=')
                    .append(in.content == null ? in.contentFile : in.content);
        }

        String previous = bodies.put(key.toString(), c.id);
        if (previous != null) {
            violations.add("[" + c.id + "] duplicate body — identical to " + previous);
        }
    }
}
