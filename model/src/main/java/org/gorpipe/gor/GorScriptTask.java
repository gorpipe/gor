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

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GorScriptTask {
    public String name;
    public String origin;
    public String signature;
    public int level;
    public String query;
    public String[] dependsOn;
    public boolean cached;
    public long startedAt;
    public long endedAt;
    public String host;
    public String thread;
    public String cacheFile;
    public String commandExecuted;

    public void update(GorScriptTask other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.origin != null) {
            origin = other.origin;
        }
        if (other.signature != null) {
            signature = other.signature;
        }
        if (other.query != null) {
            query = other.query;
        }
        if (other.host != null) {
            host = other.host;
        }
        if (other.thread != null) {
            thread = other.thread;
        }
        if (other.cacheFile != null) {
            cacheFile = other.cacheFile;
        }
        if (other.commandExecuted != null) {
            commandExecuted = other.commandExecuted;
        }
        level = Math.max(level, other.level);
        startedAt = Math.max(startedAt, other.startedAt);
        endedAt = Math.max(endedAt, other.endedAt);
        if (other.cached) {
            cached = other.cached;
        }
        if (other.dependsOn != null) {
            dependsOn = other.dependsOn;
        }
    }
}
