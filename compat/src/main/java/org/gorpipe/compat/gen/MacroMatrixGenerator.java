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
 * Emits baseline cases for macros: a bare invocation plus one per flag.
 *
 * A macro expands into a script rather than running as a pipe step, so it leads
 * the query — PGOR reads a source and splits the work by chromosome. That is why
 * neither the flag matrix nor the input source matrix reached them, and macros
 * read 0 of 4 covered until now.
 */
public final class MacroMatrixGenerator {

    /** Generated cases plus the flags no curated value could supply. */
    public static final class GenerationResult {
        public final List<CompatCase> cases;
        public final List<String> unmappedValueFlags;

        GenerationResult(List<CompatCase> cases, List<String> unmappedValueFlags) {
            this.cases = Collections.unmodifiableList(cases);
            this.unmappedValueFlags = Collections.unmodifiableList(unmappedValueFlags);
        }
    }

    private MacroMatrixGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory) {
        return generate(inventory, FlagValues.load(), macroArgs(), Exclusions.load());
    }

    public static GenerationResult generate(SurfaceInventory inventory, FlagValues values,
                                            CommandArgs macroArgs, Exclusions exclusions) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();

        for (SurfaceInventory.CommandSurface macro : inventory.macros().values()) {
            if (exclusions.excludesMacro(macro.name)) {
                continue;
            }

            cases.add(caseFor(macro, null, null, macroArgs));
            for (String flag : macro.valuelessFlags) {
                cases.add(caseFor(macro, flag, null, macroArgs));
            }
            for (String flag : macro.valueFlags) {
                String value = values.valueFor(macro.name, flag);
                if (value == null) {
                    unmapped.add(macro.name + " " + flag);
                    continue;
                }
                cases.add(caseFor(macro, flag, value, macroArgs));
            }
        }
        return new GenerationResult(cases, unmapped);
    }

    static CommandArgs macroArgs() {
        return CommandArgs.loadFrom(
                CaseLoader.moduleRoot().resolve("inventory/macro-args.yml"));
    }

    private static CompatCase caseFor(SurfaceInventory.CommandSurface macro, String flag,
                                      String value, CommandArgs macroArgs) {
        CompatCase c = new CompatCase();
        c.id = "macro." + macro.name.toLowerCase(Locale.ROOT)
                + (flag == null ? ".bare"
                                : ".flag_" + flag.substring(1).toLowerCase(Locale.ROOT));
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: macro " + macro.name
                + (flag == null ? " with no flags" : " with " + flag);

        StringBuilder q = new StringBuilder(macro.name);
        if (flag != null) {
            q.append(' ').append(flag);
            if (value != null) {
                q.append(' ').append(value);
            }
        }
        q.append(' ').append(macroArgs.positional(macro.name)).append(" | top 5");
        c.query = q.toString();

        for (CompatInput in : Fixtures.canonicalInputs()) {
            if (c.query.contains(in.path)) {
                c.inputs.add(in);
            }
        }
        return c;
    }
}
