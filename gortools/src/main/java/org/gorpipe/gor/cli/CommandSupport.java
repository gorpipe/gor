package org.gorpipe.gor.cli;

import java.io.PrintStream;

public interface CommandSupport {
    String getSecurityContext();
    String getProjectRoot();
    PrintStream getStdOut();
    PrintStream getStdErr();
    void exit(int status);
    }
