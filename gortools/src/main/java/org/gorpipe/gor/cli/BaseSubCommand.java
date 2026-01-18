package org.gorpipe.gor.cli;

import picocli.CommandLine;

import java.io.PrintWriter;

public abstract class BaseSubCommand extends HelpOptions implements CommandSupport, Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.ParentCommand
    protected CommandSupport parentSupport;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public String getSecurityContext() {
        return parentSupport != null ? parentSupport.getSecurityContext() : "";
    }

    public String getProjectRoot() {
        return parentSupport != null ? parentSupport.getProjectRoot() : "";
    }

    public PrintWriter out() {
        return spec.commandLine().getOut();
    }

    public PrintWriter err() {
        return spec.commandLine().getErr();
    }

    public void exit(int status) {
        if (parentSupport != null) parentSupport.exit(status);
        else System.exit(status);
    }
}

