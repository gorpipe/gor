package org.gorpipe.compat;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Surface coverage, derived from the corpus rather than declared.
 *
 * Attribution is textual: the query is split on the pipe and the first token of
 * each stage is resolved against the registry. It will over-attribute
 * occasionally (a command name inside a string literal) and under-attribute where
 * a flag arrives indirectly. It is a ledger of what the corpus reaches, not proof
 * that code ran — JaCoCo remains the check on execution.
 */
public final class CoverageReport {

    private final List<CompatCase> cases;
    private final SurfaceInventory inventory;
    private final Set<String> coveredCommands = new TreeSet<>();
    private final Set<String> coveredFlags = new TreeSet<>();
    private final Set<String> coveredFunctions = new TreeSet<>();
    private final Set<String> coveredInputSources = new TreeSet<>();
    private final Set<String> coveredMacros = new TreeSet<>();
    private final Set<String> coveredInputSourceFlags = new TreeSet<>();
    private final int excluded;

    private CoverageReport(List<CompatCase> cases, SurfaceInventory inventory) {
        this.cases = cases;
        this.inventory = inventory;
        this.excluded = countExclusions();
        attribute();
    }

    public static CoverageReport of(List<CompatCase> cases, SurfaceInventory inventory) {
        return new CoverageReport(cases, inventory);
    }

    private void attribute() {
        for (CompatCase c : cases) {
            String upper = c.query.toUpperCase(Locale.ROOT);

            for (String stage : c.query.split("\\|")) {
                String trimmed = stage.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String first = trimmed.split("\\s+")[0].toUpperCase(Locale.ROOT);

                // A name can belong to more than one registry, so each is checked
                // rather than matched exclusively: CMD is both a pipe command and
                // an input source.
                SurfaceInventory.CommandSurface asSource = inventory.inputSources().get(first);
                if (asSource != null) {
                    coveredInputSources.add(first);
                    for (String flag : asSource.allFlags()) {
                        if (mentionsFlag(trimmed, flag)) {
                            coveredInputSourceFlags.add(first + " " + flag);
                        }
                    }
                }
                if (inventory.macros().containsKey(first)) {
                    coveredMacros.add(first);
                }

                SurfaceInventory.CommandSurface surface = inventory.commands().get(first);
                if (surface == null) {
                    continue;
                }
                coveredCommands.add(first);
                for (String flag : surface.allFlags()) {
                    if (mentionsFlag(trimmed, flag)) {
                        coveredFlags.add(first + " " + flag);
                    }
                }
            }

            for (String fn : inventory.functionNames()) {
                if (upper.contains(fn.toUpperCase(Locale.ROOT) + "(")) {
                    coveredFunctions.add(fn);
                }
            }
        }
    }

    /** Word-boundary match, so that -s does not count as -snpsnp. */
    private static boolean mentionsFlag(String stage, String flag) {
        return stage.matches(".*(^|\\s)" + java.util.regex.Pattern.quote(flag) + "($|\\s).*");
    }

    @SuppressWarnings("unchecked")
    private int countExclusions() {
        Path file = CaseLoader.moduleRoot().resolve("inventory/exclusions.yml");
        if (!Files.exists(file)) {
            return 0;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Object parsed = new Yaml().load(in);
            return parsed instanceof List ? ((List<Object>) parsed).size() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    public int commandsCovered() {
        return coveredCommands.size();
    }

    public int flagsCovered() {
        return coveredFlags.size();
    }

    public int functionsCovered() {
        return coveredFunctions.size();
    }

    public int inputSourcesCovered() {
        return coveredInputSources.size();
    }

    public int macrosCovered() {
        return coveredMacros.size();
    }

    public int inputSourceFlagsCovered() {
        return coveredInputSourceFlags.size();
    }

    public int excludedCount() {
        return excluded;
    }

    public List<String> uncoveredCommands() {
        List<String> uncovered = new ArrayList<>();
        for (String name : inventory.commands().keySet()) {
            if (!coveredCommands.contains(name)) {
                uncovered.add(name);
            }
        }
        return uncovered;
    }

    public String render() {
        long spec = cases.stream().filter(CompatCase::isSpec).count();
        long baseline = cases.stream().filter(CompatCase::isBaseline).count();

        int totalCommands = inventory.commands().size();
        int totalFlags = inventory.totalFlagCount();
        int totalFunctions = inventory.functionNames().size();

        StringBuilder sb = new StringBuilder();
        sb.append("COMPATIBILITY SUITE\n");
        sb.append(String.format(Locale.ROOT, "  SPEC      %5d cases%n", spec));
        sb.append(String.format(Locale.ROOT, "  BASELINE  %5d cases%n", baseline));
        sb.append("  SURFACE\n");
        sb.append(String.format(Locale.ROOT, "    commands   %4d/%-4d  %d gaps%n",
                commandsCovered(), totalCommands, totalCommands - commandsCovered()));
        sb.append(String.format(Locale.ROOT, "    flags      %4d/%-4d  %d gaps%n",
                flagsCovered(), totalFlags, totalFlags - flagsCovered()));
        sb.append(String.format(Locale.ROOT, "    functions  %4d/%-4d  %d gaps%n",
                functionsCovered(), totalFunctions, totalFunctions - functionsCovered()));
        int totalInputSources = inventory.inputSources().size();
        int totalMacros = inventory.macros().size();
        sb.append(String.format(Locale.ROOT, "    inputsrc   %4d/%-4d  %d gaps%n",
                inputSourcesCovered(), totalInputSources,
                totalInputSources - inputSourcesCovered()));
        sb.append(String.format(Locale.ROOT, "    macros     %4d/%-4d  %d gaps%n",
                macrosCovered(), totalMacros, totalMacros - macrosCovered()));
        int totalSourceFlags = inventory.inputSources().values().stream()
                .mapToInt(c -> c.allFlags().size()).sum();
        sb.append(String.format(Locale.ROOT, "    src flags  %4d/%-4d  %d gaps%n",
                inputSourceFlagsCovered(), totalSourceFlags,
                totalSourceFlags - inputSourceFlagsCovered()));
        sb.append(String.format(Locale.ROOT, "  EXCLUDED  %d element(s)"
                + " (see inventory/exclusions.yml)%n", excluded));
        return sb.toString();
    }
}
