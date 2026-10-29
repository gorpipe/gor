package org.gorpipe.compat.gen;

import org.gorpipe.compat.CaseLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

/**
 * Surface deliberately left uncovered, each entry with a reason.
 *
 * Read by the generator rather than duplicated in code, so that skipping a
 * command requires writing down why, in a file whose growth shows up in review.
 */
public final class Exclusions {

    /** One excluded element: an id prefix such as "cmd.CMD", plus why. */
    public static final class Entry {
        public final String element;
        public final String reason;
        public final String ticket;

        Entry(String element, String reason, String ticket) {
            this.element = element;
            this.reason = reason;
            this.ticket = ticket;
        }
    }

    private final List<Entry> entries;
    private final Set<String> excludedCommands;
    private final Set<String> excludedFunctions;
    private final Set<String> excludedInputSources;
    private final Set<String> excludedMacros;
    private final Set<String> excludedCases;
    private final Set<String> excludedFlags;

    private Exclusions(List<Entry> entries, Set<String> excludedCommands,
                       Set<String> excludedFunctions, Set<String> excludedInputSources,
                       Set<String> excludedMacros, Set<String> excludedCases,
                       Set<String> excludedFlags) {
        this.entries = Collections.unmodifiableList(entries);
        this.excludedCommands = Collections.unmodifiableSet(excludedCommands);
        this.excludedFunctions = Collections.unmodifiableSet(excludedFunctions);
        this.excludedInputSources = Collections.unmodifiableSet(excludedInputSources);
        this.excludedMacros = Collections.unmodifiableSet(excludedMacros);
        this.excludedCases = Collections.unmodifiableSet(excludedCases);
        this.excludedFlags = Collections.unmodifiableSet(excludedFlags);
    }

    public static Exclusions load() {
        return loadFrom(CaseLoader.moduleRoot().resolve("inventory/exclusions.yml"));
    }

    @SuppressWarnings("unchecked")
    public static Exclusions loadFrom(Path file) {
        List<Entry> entries = new ArrayList<>();
        Set<String> commands = new TreeSet<>();
        Set<String> functions = new TreeSet<>();
        Set<String> inputSources = new TreeSet<>();
        Set<String> macros = new TreeSet<>();
        Set<String> cases = new TreeSet<>();
        Set<String> flags = new TreeSet<>();
        if (!Files.exists(file)) {
            return new Exclusions(entries, commands, functions, inputSources, macros,
                    cases, flags);
        }
        try (InputStream in = Files.newInputStream(file)) {
            Object raw = new Yaml().load(in);
            if (raw instanceof List) {
                for (Object item : (List<Object>) raw) {
                    if (!(item instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> map = (Map<String, Object>) item;
                    String element = asString(map.get("element"));
                    if (element == null) {
                        continue;
                    }
                    entries.add(new Entry(element, asString(map.get("reason")),
                            asString(map.get("ticket"))));
                    if (element.startsWith("cmd.")) {
                        commands.add(element.substring("cmd.".length()));
                    } else if (element.startsWith("fn.")) {
                        functions.add(element.substring("fn.".length()));
                    } else if (element.startsWith("is.")) {
                        inputSources.add(element.substring("is.".length()));
                    } else if (element.startsWith("macro.")) {
                        macros.add(element.substring("macro.".length()));
                    } else if (element.startsWith("case.")) {
                        cases.add(element.substring("case.".length()));
                    } else if (element.startsWith("flag.")) {
                        flags.add(element.substring("flag.".length()));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
        return new Exclusions(entries, commands, functions, inputSources, macros,
                cases, flags);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public List<Entry> entries() {
        return entries;
    }

    public boolean excludesCommand(String command) {
        return excludedCommands.contains(command);
    }

    public boolean excludesFunction(String function) {
        return excludedFunctions.contains(function);
    }

    public boolean excludesInputSource(String inputSource) {
        return excludedInputSources.contains(inputSource);
    }

    public boolean excludesMacro(String macro) {
        return excludedMacros.contains(macro);
    }

    /**
     * Whether one specific generated case is excluded by id.
     *
     * For a case that is known to be unstable rather than a whole command that is:
     * excluding KING outright would lose the seven of its cases that are fine.
     */
    public boolean excludesCase(String caseId) {
        return excludedCases.contains(caseId);
    }

    /**
     * Whether one flag of an otherwise covered command is excluded, named as
     * "flag.&lt;COMMAND&gt; &lt;flag&gt;".
     *
     * For a flag that cannot have a stable baseline though its command can: one
     * whose output depends on the clock or on an order the engine does not fix.
     * Written against the flag rather than the generated case id so that the
     * coverage report counts it as excluded rather than as outstanding work —
     * a case-level exclusion stops the case being generated but leaves the flag
     * looking merely unreached.
     */
    public boolean excludesFlag(String command, String flag) {
        return excludedFlags.contains(command + " " + flag);
    }

    public int size() {
        return entries.size();
    }
}
