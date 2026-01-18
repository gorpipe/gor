package org.gorpipe.gor.cli;

import picocli.CommandLine;
import java.io.PrintStream;

public abstract class BaseSubCommand extends HelpOptions implements CommandSupport, Runnable {

    @CommandLine.ParentCommand
    protected CommandSupport parentSupport;

    @Override
    public void run() { CommandLine.usage(this, getStdErr()); }

    public String getSecurityContext() {
        return parentSupport != null ? parentSupport.getSecurityContext() : "";
    }

    public String getProjectRoot() {
        return parentSupport != null ? parentSupport.getProjectRoot() : "";
    }

    public PrintStream getStdOut() {
        return parentSupport != null ? parentSupport.getStdOut() : System.out;
    }

    public PrintStream getStdErr() {
        return parentSupport != null ? parentSupport.getStdErr() : System.err;
    }

    public void exit(int status) {
        if (parentSupport != null) parentSupport.exit(status);
        else System.exit(status);
    }
}

