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
 * Emits one NOR-context case per command that works in both GOR and NOR.
 *
 * NOR is a second execution context, not a variation on the first: rows carry
 * synthetic position columns, some commands are refused outright, and others take
 * different arguments — GROUP wants a bin size in GOR and none in NOR. The
 * registry records which commands are valid where, and 66 of 108 accept NOR, yet
 * the corpus reached that path for almost none of them.
 *
 * A .gor file is used as the NOR source so the curated column names still resolve:
 * read as NOR it keeps Chrom, Pos, Ref and Alt as ordinary columns behind the
 * synthetic pair.
 */
public final class NorContextGenerator {

    /** Generated cases plus the commands skipped for having no NOR context. */
    public static final class GenerationResult {
        public final List<CompatCase> cases;
        public final List<String> gorOnly;

        GenerationResult(List<CompatCase> cases, List<String> gorOnly) {
            this.cases = Collections.unmodifiableList(cases);
            this.gorOnly = Collections.unmodifiableList(gorOnly);
        }
    }

    private static final String NOR_SOURCE = "${ROOT}/left.gor";

    private NorContextGenerator() {
    }

    public static GenerationResult generate(SurfaceInventory inventory) {
        return generate(inventory, CommandArgs.load(), Exclusions.load());
    }

    public static GenerationResult generate(SurfaceInventory inventory, CommandArgs commandArgs,
                                            Exclusions exclusions) {
        List<CompatCase> cases = new ArrayList<>();
        List<String> gorOnly = new ArrayList<>();

        for (SurfaceInventory.CommandSurface command : inventory.commands().values()) {
            if (exclusions.excludesCommand(command.name)) {
                continue;
            }
            if (!command.norCommand) {
                gorOnly.add(command.name);
                continue;
            }
            // A nor-only command is already generated in NOR by the flag matrix,
            // which takes its context from the registry too.
            if (!command.gorCommand) {
                continue;
            }
            cases.add(caseFor(command, commandArgs));
        }
        return new GenerationResult(cases, gorOnly);
    }

    private static CompatCase caseFor(SurfaceInventory.CommandSurface command,
                                      CommandArgs commandArgs) {
        CompatCase c = new CompatCase();
        c.id = "nor." + command.name.toLowerCase(Locale.ROOT) + ".bare";
        c.tier = "baseline";
        c.mode = "exact";
        c.behavior = "Generated: " + command.name + " in a NOR query";

        StringBuilder q = new StringBuilder("nor ").append(NOR_SOURCE)
                .append(" | ").append(command.name);

        String required = commandArgs.requiredFlags(command.name);
        if (!required.isEmpty()) {
            q.append(' ').append(required);
        }
        if (command.minArgs > 0 || commandArgs.hasNorPositional(command.name)
                || commandArgs.hasExplicitPositional(command.name)) {
            q.append(' ').append(commandArgs.norPositional(command.name));
        }
        q.append(" | top 5");
        c.query = q.toString();

        for (CompatInput in : Fixtures.canonicalInputs()) {
            if (c.query.contains(in.path)) {
                c.inputs.add(in);
            }
        }
        return c;
    }
}
