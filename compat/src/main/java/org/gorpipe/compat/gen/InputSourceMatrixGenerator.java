package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.Fixtures;
import org.gorpipe.compat.SurfaceInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Emits baseline cases for input sources: a bare invocation plus one per flag.
 *
 * The flag matrix reaches pipe commands only, so this surface was untouched even
 * though it is where every query begins — GOR alone declares 23 flags. The
 * argument an input source takes varies more than for a pipe command (GOR wants a
 * file, GORROW a position, NORROWS a row count), so it comes from
 * inventory/input-source-args.yml rather than being assumed.
 */
public final class InputSourceMatrixGenerator {

    /** Generated cases plus the flags no curated value could supply. */
    public static final class GenerationResult {
        public final List<CompatCase> cases;
        public final List<String> unmappedValueFlags;

        GenerationResult(List<CompatCase> cases, List<String> unmappedValueFlags) {
            this.cases = Collections.unmodifiableList(cases);
            this.unmappedValueFlags = Collections.unmodifiableList(unmappedValueFlags);
        }
    }

    private InputSourceMatrixGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory) {
        return generate(inventory, FlagValues.load(), sourceArgs(), Exclusions.load());
    }

    public static GenerationResult generate(SurfaceInventory inventory, FlagValues values,
                                            CommandArgs sourceArgs, Exclusions exclusions) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        for (SurfaceInventory.CommandSurface source : inventory.inputSources().values()) {
            if (exclusions.excludesInputSource(source.name)) {
                continue;
            }

            cases.add(caseFor(source, null, null, sourceArgs));
            for (String flag : source.valuelessFlags) {
                cases.add(caseFor(source, flag, null, sourceArgs));
            }
            for (String flag : source.valueFlags) {
                String value = values.valueFor(source.name, flag);
                if (value == null) {
                    unmapped.add(source.name + " " + flag);
                    continue;
                }
                cases.add(caseFor(source, flag, value, sourceArgs));
            }
        }
        return new GenerationResult(cases, unmapped);
    }

    static CommandArgs sourceArgs() {
        return CommandArgs.loadFrom(
                CaseLoader.moduleRoot().resolve("inventory/input-source-args.yml"));
    }

    private static CompatCase caseFor(SurfaceInventory.CommandSurface source, String flag,
                                      String value, CommandArgs sourceArgs) {
        CompatCase c = new CompatCase();
        c.id = "is." + source.name.toLowerCase(Locale.ROOT)
                + (flag == null ? ".bare"
                                : ".flag_" + flag.substring(1).toLowerCase(Locale.ROOT));
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: input source " + source.name
                + (flag == null ? " with no flags" : " with " + flag);
        c.query = buildQuery(source, flag, value, sourceArgs);
        c.inputs.addAll(inputsReferencedBy(c.query));
        return c;
    }

    /**
     * The source, its curated argument, and at most one flag, capped with top 5.
     *
     * The flag goes before the positional argument: an input source parses its
     * options first, and a flag after the file name reads as a second argument.
     */
    private static String buildQuery(SurfaceInventory.CommandSurface source, String flag,
                                     String value, CommandArgs sourceArgs) {
        StringBuilder q = new StringBuilder(source.name);

        String required = sourceArgs.requiredFlags(source.name);
        if (!required.isEmpty() && (flag == null || !required.startsWith(flag))) {
            q.append(' ').append(required);
        }
        if (flag != null) {
            q.append(' ').append(flag);
            if (value != null) {
                q.append(' ').append(value);
            }
        }
        q.append(' ').append(sourceArgs.positional(source.name));
        q.append(" | top 5");
        return q.toString();
    }

    /** Flag case ids are lowercase, so a flag differing only in case would collide. */
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
