package org.gorpipe.gor.cli.files;

import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command(name = "ls", description = "List directory contents.")
public class LsCommand implements Runnable {

    @CommandLine.Parameters(index = "0", defaultValue = ".", paramLabel = "PATH")
    private String path;

    @CommandLine.Option(names = {"-R", "--recursive"}, description = "Recursively list contents.")
    private boolean recursive;

    @CommandLine.Option(names = {"-l", "--long"}, description = "Use long listing format.")
    private boolean longFormat;

    @CommandLine.Option(names = {"-t"}, description = "Sort by modification time, newest first.")
    private boolean sortByTime;

    @CommandLine.Option(names = {"-h", "--human-readable"}, description = "With -l, print sizes in human-readable format (e.g. 1K, 234M).")
    private boolean humanReadable;

    @CommandLine.Option(names = {"-r", "--reverse"}, description = "Reverse order while sorting.")
    private boolean reverse;

    @CommandLine.Option(names = {"-d", "--directory"}, description = "List directory itself, not its contents.")
    private boolean directory;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        try {
            if (directory) {
                System.out.println(path);
                return;
            }

            if (longFormat || sortByTime) {
                int depth = recursive ? Integer.MAX_VALUE : 1;
                try (var stream = reader.iterateFile(path, depth, false, true)) {
                    List<String> all = stream.collect(Collectors.toList());
                    if (all.isEmpty()) return;
                    String header = all.get(0);
                    List<String> data = new ArrayList<>(all.subList(1, all.size()));

                    // Skip the root path itself when listing a directory
                    if (!data.isEmpty() && "true".equals(getColumn(data.get(0), 2))) {
                        data.remove(0);
                    }

                    if (sortByTime) {
                        // Sort newest first (ISO-8601 sorts lexicographically)
                        data.sort((a, b) -> getColumn(b, 7).compareTo(getColumn(a, 7)));
                    }
                    if (reverse) Collections.reverse(data);

                    if (longFormat) System.out.println(header);
                    for (String line : data) {
                        System.out.println(longFormat ? formatLine(line) : getColumn(line, 5));
                    }
                }
            } else {
                try (var stream = recursive ? reader.walk(path) : reader.list(path)) {
                    if (reverse) {
                        List<String> list = stream.collect(Collectors.toList());
                        Collections.reverse(list);
                        list.forEach(System.out::println);
                    } else {
                        stream.forEach(System.out::println);
                    }
                }
            }
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private String getColumn(String line, int index) {
        String[] parts = line.split("\t", -1);
        return index < parts.length ? parts[index] : "";
    }

    private String formatLine(String line) {
        if (!humanReadable) return line;
        String[] parts = line.split("\t", -1);
        if (parts.length > 1) {
            try {
                long bytes = Long.parseLong(parts[1]);
                parts[1] = toHumanReadable(bytes);
            } catch (NumberFormatException e) {
                // keep original
            }
        }
        return String.join("\t", parts);
    }

    private static String toHumanReadable(long bytes) {
        if (bytes < 1024) return bytes + "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f%c", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }
}
