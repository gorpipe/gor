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

package org.gorpipe.gor.analysis;

import com.google.inject.Inject;
import gorsat.Commands.Analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by sigmar on 11/02/16.
 */
public class GorAnalysisProvider implements AnalysisProvider {
    private final static Logger log = LoggerFactory.getLogger(GorAnalysisProvider.class);
    private List<AnalysisProvider> analysisProviders = new ArrayList();
    private Map<String, AnalysisProvider> cmdNameToAnalysisProvider = new HashMap();

    public GorAnalysisProvider() {
    }

    @Inject(optional = true)
    public void setProviders(Set<AnalysisProvider> initialAnalysisProviders) {
        if (initialAnalysisProviders != null) {
            for (AnalysisProvider provider : initialAnalysisProviders) {
                registerAnalyser(provider);
            }
        }
    }

    private void registerAnalyser(AnalysisProvider provider) {
        log.debug("Registering analysis provider {}", provider);
        analysisProviders.add(provider);

        String[] cmds = provider.getCommandNames();
        for (String cmd : cmds) cmdNameToAnalysisProvider.put(cmd, provider);
    }

    @Override
    public String[] getCommandNames() {
        return cmdNameToAnalysisProvider.keySet().toArray(new String[0]);
    }

    @Override
    public Analysis createAnalyser(String cmd) {
        if (cmdNameToAnalysisProvider.containsKey(cmd)) {
            AnalysisProvider ap = cmdNameToAnalysisProvider.get(cmd);
            if (ap != null) {
                return ap.createAnalyser(cmd);
            }
        }
        return null;
    }
}
