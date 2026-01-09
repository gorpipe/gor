package org.gorpipe.gor.cli.files;

import org.gorpipe.gor.cli.GorExecCLI;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.model.DriverBackedFileReader;

import picocli.CommandLine;

@CommandLine.Command(
        name = "files",
        description = "File system commands using the GOR driver framework.",
        header = "File command wrapper",
        subcommands = {LsCommand.class, CpCommand.class, MvCommand.class, RmCommand.class, CatCommand.class})
public class FilesCommand extends HelpOptions implements Runnable {

    @CommandLine.ParentCommand
    private GorExecCLI parent;

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }

    DriverBackedFileReader getFileReader() {
        return new DriverBackedFileReader(parent.getSecurityContext(), parent.getProjectRoot());
    }
}