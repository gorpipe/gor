package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.Fixtures;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Emits one baseline case per command flag, plus a bare invocation per command.
 *
 * Recomputed from the registry rather than maintained by hand, so a command that
 * gains a flag produces a new case — and therefore a missing baseline and a failing
 * build — the day the flag lands.
 */
public final class FlagMatrixGenerator {

    /** Generated cases plus what could not be generated and why. */
    public static final class GenerationResult {
        public final List<CompatCase> cases;
        public final List<String> unmappedValueFlags;

        GenerationResult(List<CompatCase> cases, List<String> unmappedValueFlags) {
            this.cases = Collections.unmodifiableList(cases);
            this.unmappedValueFlags = Collections.unmodifiableList(unmappedValueFlags);
        }
    }

    /**
     * Commands that cannot run hermetically in a generated case: they shell out,
     * touch a database, or block. Excluded here rather than allowed to produce
     * noise; the exclusions file in Task 13 records the reasoning.
     */
    private static final Set<String> SKIP_COMMANDS = new HashSet<>(Arrays.asList(
            "CMD", "SQL", "BINARYWRITE", "WRITE", "TEE", "WAIT", "BUG", "LOG",
            "LOGLEVEL", "ROOTLOGLEVEL", "GORSQL", "NORSQL", "PGOR", "PARTGOR"));

    private FlagMatrixGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory, FlagValues values) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        for (SurfaceInventory.CommandSurface command : inventory.commands().values()) {
            if (SKIP_COMMANDS.contains(command.name)) {
                continue;
            }

            for (String flag : command.valuelessFlags) {
                cases.add(caseFor(command, flag, null));
            }
            for (String flag : command.valueFlags) {
                String value = values.valueFor(command.name, flag);
                if (value == null) {
                    unmapped.add(command.name + " " + flag);
                    continue;
                }
                cases.add(caseFor(command, flag, value));
            }
        }
        return new GenerationResult(cases, unmapped);
    }

    private static CompatCase caseFor(SurfaceInventory.CommandSurface command,
                                      String flag, String value) {
        CompatCase c = new CompatCase();
        c.id = "cmd." + command.name.toLowerCase(Locale.ROOT)
                + ".flag_" + flag.substring(1).toLowerCase(Locale.ROOT);
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: " + command.name + " with " + flag;
        c.query = buildQuery(command, flag, value);
        c.inputs.addAll(requiredInputs(command));
        return c;
    }

    /**
     * A minimal invocation: read the canonical gor fixture, apply the command with
     * one flag, and cap the output so a baseline stays small.
     *
     * Positional arguments are supplied when the command declares a minimum, using
     * the canonical right-hand fixture. Whether the result succeeds or errors is
     * not this generator's concern — either outcome is a behaviour worth pinning.
     */
    private static String buildQuery(SurfaceInventory.CommandSurface command,
                                     String flag, String value) {
        StringBuilder q = new StringBuilder("gor ${ROOT}/left.gor | ");
        q.append(command.name).append(' ').append(flag);
        if (value != null) {
            q.append(' ').append(value);
        }
        if (command.minArgs > 0) {
            q.append(" ${ROOT}/right.gor");
        }
        q.append(" | top 5");
        return q.toString();
    }

    private static List<CompatInput> requiredInputs(SurfaceInventory.CommandSurface command) {
        List<CompatInput> all = Fixtures.canonicalInputs();
        List<CompatInput> needed = new ArrayList<>();
        for (CompatInput in : all) {
            if (in.path.equals("left.gor")
                    || (command.minArgs > 0 && in.path.equals("right.gor"))) {
                needed.add(in);
            }
        }
        return needed;
    }
}
