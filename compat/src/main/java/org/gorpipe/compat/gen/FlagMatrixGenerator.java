package org.gorpipe.compat.gen;

import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.Fixtures;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private FlagMatrixGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory, FlagValues values) {
        return generate(inventory, values, CommandArgs.load(), Exclusions.load());
    }

    public static GenerationResult generate(SurfaceInventory inventory, FlagValues values,
                                            CommandArgs commandArgs, Exclusions exclusions) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        for (SurfaceInventory.CommandSurface command : inventory.commands().values()) {
            // Skipping a command requires an entry in inventory/exclusions.yml
            // stating why, rather than a list buried in this file.
            if (exclusions.excludesCommand(command.name)) {
                continue;
            }

            // A bare invocation, so that the 32 commands declaring no flags at all
            // are covered too. Without it they produced no case whatsoever, which
            // was most of the uncovered command surface.
            //
            // Skipped where the command demands a companion flag: JOIN cannot run
            // without a join type, so its bare case would be byte-identical to its
            // -snpsnp case, and corpus lint rejects duplicate bodies.
            if (commandArgs.requiredFlags(command.name).isEmpty()) {
                cases.add(bareCaseFor(command, commandArgs));
            }

            for (String flag : command.valuelessFlags) {
                cases.add(caseFor(command, flag, null, commandArgs));
            }
            for (String flag : command.valueFlags) {
                String value = values.valueFor(command.name, flag);
                if (value == null) {
                    unmapped.add(command.name + " " + flag);
                    continue;
                }
                cases.add(caseFor(command, flag, value, commandArgs));
            }
        }
        return new GenerationResult(cases, unmapped);
    }

    private static CompatCase bareCaseFor(SurfaceInventory.CommandSurface command,
                                          CommandArgs commandArgs) {
        CompatCase c = new CompatCase();
        c.id = "cmd." + command.name.toLowerCase(Locale.ROOT) + ".bare";
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: " + command.name + " with no flags";
        c.query = buildQuery(command, null, null, commandArgs);
        c.inputs.addAll(inputsReferencedBy(c.query));
        return c;
    }

    private static CompatCase caseFor(SurfaceInventory.CommandSurface command,
                                      String flag, String value, CommandArgs commandArgs) {
        CompatCase c = new CompatCase();
        c.id = "cmd." + command.name.toLowerCase(Locale.ROOT)
                + ".flag_" + flag.substring(1).toLowerCase(Locale.ROOT);
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: " + command.name + " with " + flag;
        c.query = buildQuery(command, flag, value, commandArgs);
        c.inputs.addAll(inputsReferencedBy(c.query));
        return c;
    }

    /**
     * A minimal invocation: read the canonical gor fixture, apply the command with
     * one flag (or none, for the bare case), and cap the output so a baseline stays
     * small.
     *
     * Companion flags and the positional argument come from the curated
     * CommandArgs mapping, because a case that dies on a missing join type or on a
     * file supplied where a bin size was wanted records nothing about the flag it
     * was meant to exercise. Whether the result then succeeds or errors is not this
     * generator's concern — either outcome is a behaviour worth pinning.
     */
    private static String buildQuery(SurfaceInventory.CommandSurface command,
                                     String flag, String value, CommandArgs commandArgs) {
        StringBuilder q = new StringBuilder("gor ")
                .append(commandArgs.source(command.name))
                .append(" | ")
                .append(command.name);

        String required = commandArgs.requiredFlags(command.name);
        // Skipped when the flag under test is itself the required one, so that a
        // case never passes the same flag twice.
        if (!required.isEmpty() && (flag == null || !required.startsWith(flag))) {
            q.append(' ').append(required);
        }

        if (flag != null) {
            q.append(' ').append(flag);
            if (value != null) {
                q.append(' ').append(value);
            }
        }
        if (command.minArgs > 0 || commandArgs.hasExplicitPositional(command.name)) {
            q.append(' ').append(commandArgs.positional(command.name));
        }
        q.append(" | top 5");
        return q.toString();
    }

    /**
     * The canonical fixtures the finished query actually mentions. Derived from the
     * query rather than from the command's declared arity so that a curated
     * positional such as a bin size never drags in an unused fixture, which corpus
     * lint would — correctly — reject.
     */
    private static List<CompatInput> inputsReferencedBy(String query) {
        List<CompatInput> needed = new ArrayList<>();
        for (CompatInput in : Fixtures.canonicalInputs()) {
            if (query.contains(in.path)) {
                needed.add(in);
            }
        }
        return needed;
    }
}
