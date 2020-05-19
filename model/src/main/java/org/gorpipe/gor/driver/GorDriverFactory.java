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

import com.google.inject.Injector;
import org.gorpipe.gor.GorInjectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by stefan on 7.9.2016.
 */
public class GorDriverFactory {

    // TODO: use Guice for everything and remove this class! The sooner the better (08-09-2016).

    private static final Logger log = LoggerFactory.getLogger(GorDriverFactory.class);

    private static Map<GorDriver, Injector> lookup = new ConcurrentHashMap<>();
    private static GorDriver standardDriver;

    /**
     * Constructs a GorDriver using the {@link GorModulesConfig} config interface, loaded from the 'gor.modules' prefix.
     * That is, the config directory will be checked for a file named 'gor.modules.props' which will be used to populate
     * the configuration interface.
     *
     * @return a GorDriver configured via the ConfigManager using the prefix 'gor.modules'.
     */
    public synchronized static GorDriver fromConfig() {
        if (standardDriver == null) {
            standardDriver = fromConfig(null);
        }
        return standardDriver;
    }

    /**
     * Constructs a GorDriver using the {@link GorModulesConfig} config interface, loaded from the 'gor.modules' prefix and
     * using the supplied properties for overriding any values that are specified in it.
     *
     * @return a GorDriver configured via the ConfigManager using the prefix 'gor.modules'.
     * @see #fromConfig()
     */
    public static GorDriver fromConfig(Properties props) {
        Injector injector = GorInjectorFactory.fromConfig(props);
        return withInjector(injector);
    }

    /**
     * Constructs a GorDriver using the specified injector.
     *
     * @return a GorDriver configured via the given injector.
     */
    public static GorDriver withInjector(Injector injector) {
        GorDriver driver = injector.getInstance(GorDriver.class);
        lookup.put(driver, injector);
        log.debug("Retrieving GOR driver [{}] using injector [{}]", driver.hashCode(), injector.hashCode());
        return driver;
    }

    public static Injector getInjectorForDriver(GorDriver driver) {
        return lookup.get(driver);
    }

    public static void clear() {
        lookup.clear();
    }
}
