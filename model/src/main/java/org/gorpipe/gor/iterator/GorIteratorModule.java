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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Created by sigmar on 12/02/16.
 */
public class GorIteratorModule extends AbstractModule {
    private final static Logger log = LoggerFactory.getLogger(GorIteratorModule.class);
    private static GorIteratorProvider gorIterator;

    /**
     * Static entry point for code outside the scope of this module
     */
    public synchronized static GorIteratorProvider gorIterators() {
        if (gorIterator == null) {
            Injector injector = Guice.createInjector(new GorIteratorModule());
            gorIterator = injector.getInstance(GorIteratorProvider.class);
        }
        return gorIterator;
    }

    @Override
    protected void configure() {
        ServiceLoader<IteratorPlugin> plugins = ServiceLoader.load(IteratorPlugin.class);
        for (IteratorPlugin p : plugins) {
            log.info("Loading GorIterator plugin: {}", p);
            install(p);
        }
        bind(IteratorProvider.class).to(GorIteratorProvider.class);
    }
}
