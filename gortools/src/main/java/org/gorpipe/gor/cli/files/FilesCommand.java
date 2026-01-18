package org.gorpipe.gor.cli.files;

import org.gorpipe.gor.cli.BaseSubCommand;
import org.gorpipe.gor.model.DriverBackedFileReader;

import picocli.CommandLine;

@CommandLine.Command(
        name = "files",
        description = "File system commands using the GOR driver framework.",
        header = "File command wrapper",
        subcommands = {LsCommand.class, CpCommand.class, MvCommand.class, RmCommand.class, CatCommand.class})
public class FilesCommand extends BaseSubCommand{

    DriverBackedFileReader getFileReader() {
        return new DriverBackedFileReader(getSecurityContext(), getProjectRoot());
    }
}