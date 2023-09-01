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

package org.gorpipe.base.config;

import org.gorpipe.base.config.annotations.ConfigComponent;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.aeonbits.owner.Config.Key;

/**
 * Configuration Management for system and application config.
 * <p>
 * This utility class helps with loading configuration files to populate configuration interfaces.
 * <p>
 * The backend system used for configuration is called <a href="http://owner.aeonbits.org/">OWNER</a>.
 */
public class ConfigManager {

    private ConfigManager() {

    }

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private static String configRootPath = null;

    private static final List<Map<?, ?>> globalProperties = new ArrayList<>();

    private static final ConcurrentHashMap<String, Config> prefixConfigMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Map<?, ?>>> prefixAllConfigsMap = new ConcurrentHashMap<>();

    // TODO: change to support multiple configRootPaths
    // env variable defined
    // user home
    // code base path
    // system config path

    /**
     * Determine the default location for config files. The default location will be the parent directory of the
     * location of the class files if that location can be determined. If not (a {@link SecurityManager} may prevent
     * this from happening) the configuration root will be the working directory of the application.
     *
     * If the system property <code>nextcode.config.path</code> is set and has a non empty value, the value of that
     * will be used.
     */
    static {
        String configRootPath = null;
        String syspropConfigPath = System.getProperty("nextcode.config.path");

        if (syspropConfigPath != null && !syspropConfigPath.isEmpty()) {
            configRootPath = syspropConfigPath;
            log.debug("Config path determined from system property");
        } else {
            try {
                ProtectionDomain protectionDomain = ConfigManager.class.getProtectionDomain();
                CodeSource codeSource = protectionDomain.getCodeSource();
                String sourcePath = codeSource.getLocation().getPath();
                if (sourcePath != null && !sourcePath.isEmpty()) {
                    configRootPath = new File(sourcePath).getParent() + "/../config/";
                    log.debug("Config path determined from code source location");
                }
            } catch (Exception t) {
                log.warn("Problem determining configuration root path.", t);
            }
        }

        if (configRootPath == null || configRootPath.isEmpty()) {
            configRootPath = System.getProperty("user.dir");
            log.debug("Config path determined from working directory");
        }

        setConfigRootPath(configRootPath);
    }

    /**
     * Overrides the default configuration root path. This method is mostly useful for testing purposes.
     *
     * @param configRootPath the configuration root path that should be used to load config files from.
     */
    public static void setConfigRootPath(String configRootPath) {
        if (!configRootPath.endsWith("/")) {
            log.trace("Config root path should end with a trailing slash. Appending it automatically.");
            configRootPath += "/";
        }
        if (ConfigManager.configRootPath != null && !ConfigManager.configRootPath.isEmpty()) {
            log.debug("Overriding previously set configuration root path {} with {}", ConfigManager.configRootPath, configRootPath);
        }
        log.debug("Using configuration root path: {}", configRootPath);
        ConfigManager.configRootPath = configRootPath;
    }

    public static String getConfigRootPath() {
        return configRootPath;
    }

    /**
     * Creates a <code>String</code> array with the names of the configuration files that should be loaded for the given
     * prefix. The array is ordered by configuration file precedence, that is, if two sources define the same
     * configuration option, the source that comes earlier in the list will have precedence.
     * <p>
     * The list may contain null values or invalid file paths, no checks are made by this method.
     *
     * @param prefix the configuration prefix for which to create the default source list.
     * @return a list of configuration sources to load for the given prefix.
     */
    public static List<String> getDefaultConfigFileNamesForPrefix(String prefix) {
        List<String> files = new ArrayList<>();

        List<String> parts = Arrays.asList(prefix.split(Pattern.quote("."), -1));
        for (int i = parts.size() - 1; i >= 0; i--) {
            String part = String.join(".", parts.subList(0, i + 1));
            addFileIfNotNullOrEmpty(System.getProperty(part + ".config.file"), files);
            addFileIfNotNullOrEmpty(configRootPath + part + ".props", files);
            addFileIfNotNullOrEmpty(configRootPath + part + ".props.defaults", files); // TODO: remove defaults, use the DefaultValue annotation instead
        }

        return files;
    }

    private static void addFileIfNotNullOrEmpty(String fileName, List<String> files) {
        if (fileName != null && !fileName.isEmpty() && !fileName.isBlank()) {
            files.add(fileName);
        }
    }

    /**
     * Goes through the default configuration file names and tries to load them into property objects, ignoring files
     * that either don't exist or can't be loaded (in which case we log out a warning message).
     *
     * @param prefix the prefix of the configuration file to load.
     * @return a list of {@link Properties} objects that were successfully loaded.
     */
    private static List<Properties> loadDefaultConfigPropertiesForPrefix(String prefix) {
        List<Properties> props = new ArrayList<>();

        // Create a list of the sources that we want to check
        for (String fileName : getDefaultConfigFileNamesForPrefix(prefix)) {
            try {
                if (fileName != null && !fileName.isEmpty() && Files.exists(Paths.get(fileName))) {
                    Properties p = new Properties();
                    p.load(new FileInputStream(new File(fileName)));
                    props.add(p);
                    log.debug("Loaded configuration file {}", fileName);
                } else {
                    log.trace("Not loading configuration file {}", fileName);
                }
            } catch (IOException e) {
                // This means that the file exists but is not loadable, we want to know about that.
                log.warn("Unable to load configuration file {} - ignoring", fileName, e);
            }
        }

        return props;
    }

    /**
     * Adds a global properties source for config options. These sources will have higher precedence than the default
     * values (specified as annotations on the config classes) but lower than anything else.
     *
     * @param properties the properties to add as a global properties source.
     */
    public static void addGlobalPropertiesSource(Map<?, ?> properties) {
        globalProperties.add(properties);
    }

    /**
     * Removes the specified properties object from the list of global properties sources.
     *
     * @param properties the properties to remove from the global properties sources.
     */
    public static void removeGlobalPropertiesSource(Map<?, ?> properties) {
        globalProperties.remove(properties);
    }

    /**
     * Removes all properties object from the list of global properties sources.
     */
    public static void clearGlobalPropertiesSources() {
        globalProperties.clear();
    }

    /**
     * Clears the prefixed config cache.
     */
    public static void clearPrefixConfigCache() {
        prefixConfigMap.clear();
    }

    /**
     * Get or create a config object populated by the following config maps:
     * <p>
     * <ul>
     * <li>System properties</li>
     * <li>Config maps passed to this method</li>
     * <li>Default config file sources for the specified <code>prefix</code></li>
     * </ul>
     * <p>
     * The provided config maps are evaluated in descending precedence order (first <code>Map</code> overrides values
     * from all succeeding maps). The same applies to the list of config maps passed as the parameter
     * <code>configs</code> to this method, maps that appear earlier in the list have precedence.
     * <p>
     * On the first call to this method for the given prefix and clazz the config object will be created, on subsequent
     * calls the config object will be retrieved from cache.
     *
     * @param prefix  the prefix to use to locate the configuration files from which to load config properties.
     * @param clazz   the configuration object type that will be populated.
     * @param configs the configuration map objects that will be used to populate the config object that is returned.
     * @return a populated config object.
     */
    public static <T extends Config> T getPrefixConfig(String prefix, Class<? extends T> clazz, Map<?, ?>... configs) {
        String key = prefix + "_" + clazz.getName();
        if (prefixConfigMap.containsKey(key)) {
            return clazz.cast(prefixConfigMap.get(key));
        }

        T config = createPrefixConfig(prefix, clazz, configs);
        prefixConfigMap.putIfAbsent(key, config);

        return config;
    }

    public static <T extends Config> T getPrefixConfig(Class<? extends T> clazz, Map<?, ?>... configs) {
        ConfigComponent configComponentAnnotation = clazz.getAnnotation(ConfigComponent.class);

        if (configComponentAnnotation != null) {
            return getPrefixConfig(configComponentAnnotation.value(), clazz, configs);
        } else {
            log.warn("Expected a ConfigComponent annotation on configuration interface {} but found none. " +
                    "Loading only with system properties.", clazz.getName());
            return createConfig(clazz, configs);
        }
    }

    /**
     * Get a config object populated by the following config maps:
     * <p>
     * <ul>
     * <li>System properties</li>
     * <li>Config maps passed to this method</li>
     * <li>Default config file sources for the {@link ConfigComponent} that the config class is annotated with</li>
     * </ul>
     * <p>
     * The provided config maps are evaluated in descending precedence order (first <code>Map</code> overrides values
     * from all succeeding maps). The same applies to the list of config maps passed as the parameter
     * <code>configs</code> to this method, maps that appear earlier in the list have precedence.
     *
     * @param clazz   the configuration object type that will be populated.
     * @param configs the configuration map objects that will be used to populate the config object that is returned.
     * @return a populated config object.
     */
    public static <T extends Config> T createPrefixConfig(Class<? extends T> clazz, Map<?, ?>... configs) {
        ConfigComponent configComponentAnnotation = clazz.getAnnotation(ConfigComponent.class);

        if (configComponentAnnotation != null) {
            return createPrefixConfig(configComponentAnnotation.value(), clazz, configs);
        } else {
            log.warn("Expected a ConfigComponent annotation on configuration interface {} but found none. " +
                    "Loading only with system properties.", clazz.getName());
            return createConfig(clazz, configs);
        }
    }

    /**
     * Get a config object populated by the following config maps:
     * <p>
     * <ul>
     * <li>System properties</li>
     * <li>Config maps passed to this method</li>
     * <li>Default config file sources for the specified <code>prefix</code></li>
     * </ul>
     * <p>
     * The provided config maps are evaluated in descending precedence order (first <code>Map</code> overrides values
     * from all succeeding maps). The same applies to the list of config maps passed as the parameter
     * <code>configs</code> to this method, maps that appear earlier in the list have precedence.
     *
     * @param prefix  the prefix to use to locate the configuration files from which to load config properties.
     * @param clazz   the configuration object type that will be populated.
     * @param configs the configuration map objects that will be used to populate the config object that is returned.
     * @return a populated config object.
     */
    public static <T extends Config> T createPrefixConfig(String prefix, Class<? extends T> clazz, Map<?, ?>... configs) {

        List<Map<?, ?>> allConfigs = new ArrayList<>();

        if (prefixAllConfigsMap.containsKey(prefix)) {
            allConfigs =  prefixAllConfigsMap.get(prefix);
        } else {
            // system properties always take precedence
            allConfigs.add(System.getProperties());

            // make the config maps supplied as parameters take precedence over the default ones
            allConfigs.addAll(Arrays.asList(configs));

            // Add system envs.
            allConfigs.add(System.getenv());

            // now add all the defaults
            allConfigs.addAll(loadDefaultConfigPropertiesForPrefix(prefix));

            // finally add the registered global properties
            allConfigs.addAll(globalProperties);

            prefixAllConfigsMap.put(prefix, allConfigs);
        }

        return createConfig(clazz, allConfigs.toArray(new Map<?, ?>[allConfigs.size()]));
    }

    /**
     * Get a config object populated by the supplied configuration maps.
     * <p>
     * The provided maps are evaluated in descending precedence order (first <code>Map</code> overrides values from all
     * succeeding maps).
     *
     * @param clazz   the configuration object type that will be populated.
     * @param configs the configuration map objects that will be used to populate the config object that is returned.
     * @return a populated config object.
     */
    public static <T extends Config> T createConfig(Class<? extends T> clazz, Map<?, ?>... configs) {
        log.debug("Creating config for {} including {}", clazz, configs);
        return ConfigFactory.create(clazz, configs);
    }

    public static String[] getKeysForConfig(Class<? extends Config> clazz) {
        // Filter out any methods that do not have the @Key annotation,
        // get the value of each Key annotation and return as a String array

        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getAnnotation(Key.class) != null)
                .map(m -> m.getAnnotation(Key.class).value())
                .toArray(String[]::new);
    }

    /**
     * Return major.minor version number extracted from version
     * 7.0-fo =&gt; 7.0
     * 7.0.1 =&gt; 7.0
     * fofof7.0.1fofo =&gt; 7.0
     *
     * @param version The version string to extract from
     * @return major.minor version number
     */
    public static String getMajorMinorVersionFromString(String version) {
        String displayVersion = "";
        if (version != null && !version.isEmpty()) {
            Pattern p = Pattern.compile("[0-9]+\\.[0-9]+");
            Matcher m = p.matcher(version);
            if (m.find()) {
                displayVersion += m.group();
            }
        }
        return displayVersion;
    }

    /**
     * Inject all the properties in props into config.
     *
     * @param props  properties to inject into config
     * @param config recipient of injection
     */
    public static void injectProperties(Properties props, Properties config, ArrayList<String> filter) {
        for (String propertyName : props.stringPropertyNames()) {
            String newValue = props.getProperty(propertyName);

            // Do not overwrite property with the same value
            if (config.getProperty(propertyName) != null && config.getProperty(propertyName).equals(newValue)) {
                continue;
            }

            // Do not overwrite properties set as input arguments (-D properties)
            if (filter.contains(propertyName)) {
                log.info("Ignoring property {} because it is a startup argument", propertyName);
                continue;
            }

            config.setProperty(propertyName, newValue);
        }
    }
}
