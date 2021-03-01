/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.session;

import org.gorpipe.gor.monitor.GorMonitor;

/**
 * System context stores immutable services and settings related to system configuration. This includes
 * report builder, white listing etc. It supports a Builder pattern to initialize the context.
 */
public class SystemContext {
    private int workers = 0;
    private long startTime = -1;
    private boolean server = true;
    private GorReportBuilder reportBuilder;
    private GorMonitor monitor;
    private GorRunnerFactory runnerFactory;
    private Object commandWhitelist;

    public static class Builder {
        private int workers = 0;
        private long startTime = -1;
        private boolean server = true;
        private GorReportBuilder reportBuilder;
        private GorMonitor monitor;
        private GorRunnerFactory runnerFactory;
        private Object commandWhitelist;

        public Builder setWorkers(int workers) {
            this.workers = workers;
            return this;
        }

        public Builder setStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder setServer(boolean server) {
            this.server = server;
            return this;
        }

        public Builder setReportBuilder(GorReportBuilder reportBuilder) {
            this.reportBuilder = reportBuilder;
            return this;
        }

        public Builder setMonitor(GorMonitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder setRunnerFactory(GorRunnerFactory runnerFactory) {
            this.runnerFactory = runnerFactory;
            return this;
        }

        public Builder setCommandWhitelist(Object commandWhitelist) {
            this.commandWhitelist = commandWhitelist;
            return this;
        }

        public SystemContext build() {
            SystemContext systemContext = new SystemContext();
            systemContext.monitor = this.monitor;
            systemContext.reportBuilder = this.reportBuilder;
            systemContext.runnerFactory = this.runnerFactory;
            systemContext.server = this.server;
            systemContext.startTime = this.startTime;
            systemContext.workers = this.workers <= 0 ? Runtime.getRuntime().availableProcessors() : this.workers;
            systemContext.commandWhitelist = this.commandWhitelist;

            return systemContext;
        }
    }

    private SystemContext() { }

    public int getWorkers() {
        return workers;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean getServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public GorReportBuilder getReportBuilder() {
        return reportBuilder;
    }

    public GorMonitor getMonitor() {
        return monitor;
    }
    public void setMonitor(GorMonitor monitor) {
        this.monitor = monitor;
    }

    public GorRunnerFactory getRunnerFactory() {
        return runnerFactory;
    }

    public Object getCommandWhitelist() {
        return commandWhitelist;
    }
}
