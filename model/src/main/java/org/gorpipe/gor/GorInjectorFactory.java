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

import org.gorpipe.model.util.Util;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.gorpipe.gor.driver.GorModulesConfig;
import org.gorpipe.base.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Guice Injector factory that will create injectors based on configuration settings defined by
 * {@link GorModulesConfig} via the {@link ConfigManager}. It will cache injectors that it creates so multiple requests
 * for injectors using the exact same module configuration will yield the same injector instance.
 */
public class GorInjectorFactory {
    private static Logger log = LoggerFactory.getLogger(GorInjectorFactory.class);

    private static Map<String, Injector> injectorRegistry = new ConcurrentHashMap<>();

    /**
     * Constructs a Guice Injector using the {@link GorModulesConfig} config interface, loaded from the 'gor.modules'
     * prefix. That is, the config directory will be checked for a file named 'gor.modules.props' which will be used to
     * populate the configuration interface and if not found, the normal {@link ConfigManager} hierarchy will be used,
     * checking for a 'gor.props' file before defaulting to the values specified in {@link GorModulesConfig}.
     *
     * @return a Guice Injector configured via the ConfigManager using the prefix 'gor.modules'.
     */
    public static Injector fromConfig() {
        return fromConfig(null);
    }

    /**
     * Constructs a Guice Injector using the {@link GorModulesConfig} config interface, loaded from the 'gor.modules'
     * prefix and using the supplied properties for overriding any values that are specified in it.
     *
     * @param props a key/value map that overrides any configuration for {@link GorModulesConfig} read by the {@link ConfigManager}.
     * @return a GorDriver configured via the ConfigManager using the prefix 'gor.modules'.
     * @see #fromConfig()
     */
    public static Injector fromConfig(Properties props) {
        GorModulesConfig cfg = ConfigManager.createPrefixConfig("gor.modules", GorModulesConfig.class);
        if (props != null) {
            for (Object key : props.keySet()) {
                cfg.setProperty(key.toString(), props.getProperty(key.toString()));
            }
        }
        List<Module> modules = new LinkedList<>();
        modules.add(cfg.driverModule());
        modules.addAll(cfg.extraModules());
        return getOrCreateInjector(modules, cfg.overrideModules());
    }

    /**
     * Creates a GorDriver from the supplied modules and stores it using a key constructed from the module class
     * names.
     */
    private static Injector getOrCreateInjector(List<Module> modules, List<Module> overrideModules) {
        // Since module order is not important, we sort the module lists alphabetically to always get the same
        // signature for any combination of the same modules.
        Comparator<Module> lexicalModuleComparator = Comparator.comparing(o -> o.getClass().getCanonicalName());
        LinkedList<Module> sortedModules = new LinkedList<>(modules);
        sortedModules.sort(lexicalModuleComparator);
        LinkedList<Module> sortedOverrideModules = new LinkedList<>(overrideModules);
        sortedOverrideModules.sort(lexicalModuleComparator);

        StringBuilder modulesString = new StringBuilder();
        for (Module m : sortedModules) {
            if (modulesString.length() != 0) {
                modulesString.append(", ");
            }
            modulesString.append(m.getClass().getCanonicalName());
        }
        StringBuilder overridesString = new StringBuilder();
        for (Module m : sortedOverrideModules) {
            if (overridesString.length() != 0) {
                overridesString.append(", ");
            }
            overridesString.append(m.getClass().getCanonicalName());
        }
        String key = Util.md5("modules: " + modulesString + " | overrides: " + overridesString);
        return injectorRegistry.computeIfAbsent(key, k -> {
            Injector injector;
            if (overrideModules != null && overrideModules.isEmpty()) {
                injector = Guice.createInjector(Modules.override(modules).with(overrideModules));
            } else {
                injector = Guice.createInjector(modules);
            }
            log.debug("Added a Gor Injector to the registry from the modules (key: [{}] modules: [{}] overrides: [{}]", key, modulesString, overridesString);
            return injector;
        });
    }

    public static void clear() {
        injectorRegistry.clear();
    }
}
