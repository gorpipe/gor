package org.gorpipe.gor.cli;

public interface CommandSupport {
    String getSecurityContext();
    String getProjectRoot();
    void exit(int status);
    }
