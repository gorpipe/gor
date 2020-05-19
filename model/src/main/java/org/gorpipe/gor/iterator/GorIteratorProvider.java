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

package org.gorpipe.gor.iterator;

import org.gorpipe.model.genome.files.gor.GenomicIterator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by sigmar on 12/02/16.
 */
public class GorIteratorProvider implements IteratorProvider {
    private final static Logger log = LoggerFactory.getLogger(GorIteratorProvider.class);
    private List<IteratorProvider> iteratorProviders = new ArrayList();
    private Map<String, IteratorProvider> cmdNameToIteratorProvider = new HashMap();

    @Inject
    public GorIteratorProvider(Set<IteratorProvider> initialIteratorProviders) {
        if (initialIteratorProviders != null) {
            for (IteratorProvider provider : initialIteratorProviders) {
                registerIterator(provider);
            }
        }
    }

    private void registerIterator(IteratorProvider provider) {
        log.debug("Registering analysis provider {}", provider);
        iteratorProviders.add(provider);
        String[] cmds = provider.getIteratorNames();
        for (String cmd : cmds) {
            cmdNameToIteratorProvider.put(cmd, provider);
        }
    }

    @Override
    public String[] getIteratorNames() {
        return cmdNameToIteratorProvider.keySet().toArray(new String[0]);
    }

    @Override
    public GenomicIterator createIterator(String command) {
        String[] split = command.split(" ");
        return cmdNameToIteratorProvider.get(split[0]).createIterator(command);
    }
}
