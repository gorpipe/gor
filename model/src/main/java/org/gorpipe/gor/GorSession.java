/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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

package org.gorpipe.gor;

/**
 * Gor session object. Stores project context, system context and the session cache.
 */
public class GorSession implements AutoCloseable {

    private boolean norContext;
    private final String requestId;
    private ProjectContext projectContext;
    private SystemContext systemContext;
    private GorSessionCache cache;
    private EventLogger eventLogger;

    private final GorContext gorContext = new GorContext(this);

    /**
     * Constructs a session object for gor for a gived request id.
     *
     * @param requestId Request id used for session
     */
    public GorSession(String requestId) {
        this.requestId = requestId;
    }

    public void init(ProjectContext projectContext, SystemContext systemContext, GorSessionCache cache) {
        init(projectContext, systemContext, cache, new DefaultEventLogger());
    }

    public void init(ProjectContext projectContext, SystemContext systemContext, GorSessionCache cache,
                     EventLogger eventLogger) {
        this.projectContext = projectContext;
        this.systemContext = systemContext;
        this.cache = cache;
        this.eventLogger = eventLogger;

        // load the reference build
        getProjectContext().loadReferenceBuild(this);
    }

    public String getRequestId() {
        return this.requestId;
    }

    public boolean getNorContext() {
        return this.norContext;
    }

    public void setNorContext(boolean norContext) {
        this.norContext = norContext;
    }

    public ProjectContext getProjectContext() {
        return this.projectContext;
    }

    public SystemContext getSystemContext() {
        return this.systemContext;
    }

    public GorSessionCache getCache() {
        return this.cache;
    }

    public EventLogger getEventLogger() {
        return eventLogger;
    }

    public GorContext getGorContext() {
        return gorContext;
    }

    public void close() {
        if (eventLogger != null) {
            eventLogger.endSession();
        }
    }
}
