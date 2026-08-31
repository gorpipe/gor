package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.gorpipe.compat.CompatCase;
import org.gorpipe.compat.CompatInput;
import org.gorpipe.compat.Fixtures;
import org.gorpipe.compat.SurfaceInventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates structurally varied queries by composing registry commands.
 *
 * Most output is semantically invalid, so most baselines here pin an error
 * message — which is genuine parser-contract coverage. Seeded from a committed
 * fixed seed so the corpus is stable between runs; an unstable fuzz corpus would
 * fill every pull request with meaningless baseline diffs.
 */
public final class GrammarFuzzer {

    private static final long FALLBACK_SEED = 20260831L;

    private GrammarFuzzer() {
    }

    public static long fuzzSeed() {
        Path file = CaseLoader.moduleRoot().resolve("inventory/fuzz-seed.txt");
        if (!Files.exists(file)) {
            return FALLBACK_SEED;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    return Long.parseLong(trimmed);
                }
            }
        } catch (IOException | NumberFormatException e) {
            return FALLBACK_SEED;
        }
        return FALLBACK_SEED;
    }

    public static List<CompatCase> generate(SurfaceInventory inventory, int count) {
        Random random = new Random(fuzzSeed());
        List<String> commandNames = new ArrayList<>(inventory.commands().keySet());

        List<CompatCase> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int stages = 1 + random.nextInt(3);
            StringBuilder query = new StringBuilder("gor ${ROOT}/left.gor");
            for (int s = 0; s < stages; s++) {
                String name = commandNames.get(random.nextInt(commandNames.size()));
                SurfaceInventory.CommandSurface surface = inventory.commands().get(name);
                query.append(" | ").append(name);
                if (!surface.valuelessFlags.isEmpty() && random.nextBoolean()) {
                    query.append(' ').append(surface.valuelessFlags.get(
                            random.nextInt(surface.valuelessFlags.size())));
                }
            }

            CompatCase c = new CompatCase();
            c.id = "fuzz.grammar.case_" + String.format(Locale.ROOT, "%04d", i);
            c.tier = "baseline";
            c.mode = "exact";
            c.behavior = "Grammar fuzz case " + i + " (seed " + fuzzSeed() + ")";
            c.query = query.toString();
            for (CompatInput in : Fixtures.canonicalInputs()) {
                if (in.path.equals("left.gor")) {
                    c.inputs.add(in);
                }
            }
            out.add(c);
        }
        return out;
    }
}
