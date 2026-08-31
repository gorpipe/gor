package org.gorpipe.compat;

import gorsat.Commands.CommandArguments;
import gorsat.Commands.CommandInfo;
import gorsat.parser.CalcFunctions;
import gorsat.process.GorPipeCommands;
import gorsat.process.PipeInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The language surface, read from the live registries.
 *
 * The registries are populated by instantiating command classes
 * (addInfo(new gorsat.Commands.Join)), so neither names nor flag specifications
 * can be grepped out of source — they exist only at runtime.
 */
public final class SurfaceInventory {

    /** One command's declared argument surface. */
    public static final class CommandSurface {
        public final String name;
        public final List<String> valuelessFlags;
        public final List<String> valueFlags;
        public final int minArgs;
        public final int maxArgs;

        CommandSurface(String name, List<String> valuelessFlags, List<String> valueFlags,
                       int minArgs, int maxArgs) {
            this.name = name;
            this.valuelessFlags = Collections.unmodifiableList(valuelessFlags);
            this.valueFlags = Collections.unmodifiableList(valueFlags);
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
        }

        public List<String> allFlags() {
            List<String> all = new ArrayList<>(valuelessFlags);
            all.addAll(valueFlags);
            return all;
        }
    }

    private final Map<String, CommandSurface> commands;
    private final Set<String> functionNames;

    private SurfaceInventory(Map<String, CommandSurface> commands, Set<String> functionNames) {
        this.commands = Collections.unmodifiableMap(commands);
        this.functionNames = Collections.unmodifiableSet(functionNames);
    }

    public static SurfaceInventory read() {
        PipeInstance.initialize();

        Map<String, CommandSurface> commands = new TreeMap<>();
        scala.collection.Iterator<scala.Tuple2<String, CommandInfo>> it =
                GorPipeCommands.commandMap().iterator();
        while (it.hasNext()) {
            scala.Tuple2<String, CommandInfo> entry = it.next();
            String name = entry._1();
            CommandArguments args = entry._2().commandArguments();
            commands.put(name, new CommandSurface(
                    name,
                    splitFlags(args.options()),
                    splitFlags(args.valueOptions()),
                    args.minimumNumberOfArguments(),
                    args.maximumNumberOfArguments()));
        }

        // FunctionRegistry is a class; the CALC/WHERE surface lives in the
        // self-registering CalcFunctions.registry singleton.
        return new SurfaceInventory(commands,
                new TreeSet<>(CalcFunctions.registry().functionNames()));
    }

    /** Flags are declared as one space-separated string, e.g. "-snpsnp -segseg -l". */
    private static List<String> splitFlags(String declaration) {
        List<String> flags = new ArrayList<>();
        if (declaration == null) {
            return flags;
        }
        for (String token : declaration.trim().split("\\s+")) {
            if (!token.isEmpty()) {
                flags.add(token);
            }
        }
        Collections.sort(flags);
        return flags;
    }

    public Map<String, CommandSurface> commands() {
        return commands;
    }

    public Set<String> functionNames() {
        return functionNames;
    }

    public int totalFlagCount() {
        int total = 0;
        for (CommandSurface c : commands.values()) {
            total += c.valuelessFlags.size() + c.valueFlags.size();
        }
        return total;
    }

    /**
     * Stable JSON, hand-rendered rather than via Jackson so that key order and
     * whitespace are fully determined and the committed file diffs cleanly.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"commands\": {\n");
        int ci = 0;
        for (CommandSurface c : commands.values()) {
            sb.append("    \"").append(c.name).append("\": {\n");
            sb.append("      \"valuelessFlags\": ").append(jsonArray(c.valuelessFlags)).append(",\n");
            sb.append("      \"valueFlags\": ").append(jsonArray(c.valueFlags)).append(",\n");
            sb.append("      \"minArgs\": ").append(c.minArgs).append(",\n");
            sb.append("      \"maxArgs\": ").append(c.maxArgs).append('\n');
            sb.append("    }").append(++ci < commands.size() ? "," : "").append('\n');
        }
        sb.append("  },\n  \"functions\": ").append(jsonArray(new ArrayList<>(functionNames)));
        sb.append('\n').append("}\n");
        return sb.toString();
    }

    private static String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"')
              .append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\""))
              .append('"');
        }
        return sb.append(']').toString();
    }
}
