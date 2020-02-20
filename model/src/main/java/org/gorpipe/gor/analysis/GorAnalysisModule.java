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

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Created by sigmar on 11/02/16.
 */
public class GorAnalysisModule extends AbstractModule {
    private static AnalysisProvider gorAnalysis;

    private final static Logger log = LoggerFactory.getLogger(GorAnalysisModule.class);

    @Override
    protected void configure() {
        ServiceLoader<AnalysisPlugin> plugins = ServiceLoader.load(AnalysisPlugin.class);
        for (AnalysisPlugin p : plugins) {
            log.debug("Loading GorAnalysis plugin: {}", p);
            install(p);
        }
        bind(AnalysisProvider.class).to(GorAnalysisProvider.class);
    }

    /**
     * Static entry point for code outside the scope of this module
     */
    public synchronized static AnalysisProvider gorAnalysers() {
        if (gorAnalysis == null) {
            Injector injector = Guice.createInjector(new GorAnalysisModule());
            gorAnalysis = injector.getInstance(GorAnalysisProvider.class);
        }
        return gorAnalysis;

    }

    public static void bindAnalysisProvider(Binder binder, Class<? extends AnalysisProvider> providerClass) {
        Multibinder<AnalysisProvider> multiBinder = Multibinder.newSetBinder(binder, AnalysisProvider.class);
        multiBinder.addBinding().to(providerClass);
    }
}
