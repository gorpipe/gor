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

package org.gorpipe.gor.driver;

import java.util.ServiceLoader;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.base.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GorDriverModule extends AbstractModule {
    private final static Logger log = LoggerFactory.getLogger(GorDriverModule.class);

    @Override
    protected void configure() {
        bind(GorDriver.class).to(PluggableGorDriver.class).asEagerSingleton();
        ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class);
        for (Plugin p : plugins) {
            log.debug("Loading GorDriver plugin: {}", p);
            install(p);
        }
    }

    /**
     * Get GorDriver configuration
     */
    @Provides
    @Singleton
    public GorDriverConfig getGorDriverConfig() {
        return ConfigManager.createPrefixConfig("gor", GorDriverConfig.class);
    }

    @Provides
    @Singleton
    public FileCache getFileCache(GorDriverConfig config) {
        // TODO: replace with a call to ManagedFileCache.forceCache or similar
        FileCache fileCache = new FileCache(config);
        fileCache.startSweepingThread();
        return fileCache;
    }

    public static void bindSourceProvider(Binder binder,
                                          Class<? extends SourceProvider> providerClass) {
        Multibinder<SourceProvider> multiBinder = Multibinder.newSetBinder(binder, SourceProvider.class);
        multiBinder.addBinding().to(providerClass);
    }
}
