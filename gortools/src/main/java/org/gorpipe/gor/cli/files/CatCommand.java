package org.gorpipe.gor.cli.files;

import org.gorpipe.gor.cli.HelpOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@CommandLine.Command(name = "cat", header = "Concatenate files to standard output.", description = "Concatenate files to standard output.")
public class CatCommand extends HelpOptions implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "PATH")
    private String path;

    @CommandLine.Option(names = {"-n", "--number"}, description = "Number all output lines.")
    private boolean number;

    @CommandLine.Option(names = {"-b", "--number-nonblank"}, description = "Number nonempty output lines only, overrides -n.")
    private boolean numberNonblank;

    @CommandLine.Option(names = {"-s", "--squeeze-blank"}, description = "Suppress repeated empty output lines.")
    private boolean squeezeBlank;

    @CommandLine.Option(names = {"-E", "--show-ends"}, description = "Display $ at end of each line.")
    private boolean showEnds;

    @CommandLine.Option(names = {"-T", "--show-tabs"}, description = "Display TAB characters as ^I.")
    private boolean showTabs;

    @CommandLine.Option(names = {"-A", "--show-all"}, description = "Equivalent to -ET.")
    private boolean showAll;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        if (showAll) {
            showEnds = true;
            showTabs = true;
        }
        var reader = parent.getFileReader();
        var counter = new AtomicInteger(0);
        var prevBlank = new AtomicBoolean(false);
        try (var stream = reader.readFile(path)) {
            stream.forEach(rawLine -> {
                boolean isBlank = rawLine.isEmpty();
                if (squeezeBlank && isBlank) {
                    if (prevBlank.get()) return;
                    prevBlank.set(true);
                } else {
                    prevBlank.set(false);
                }
                String line = rawLine;
                if (showTabs) line = line.replace("\t", "^I");
                if (showEnds) line = line + "$";
                if (numberNonblank) {
                    if (!isBlank) {
                        System.out.printf("%6d\t%s%n", counter.incrementAndGet(), line);
                    } else {
                        System.out.println(line);
                    }
                } else if (number) {
                    System.out.printf("%6d\t%s%n", counter.incrementAndGet(), line);
                } else {
                    System.out.println(line);
                }
            });
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }
}
