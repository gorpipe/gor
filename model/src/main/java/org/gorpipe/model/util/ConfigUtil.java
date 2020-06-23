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

package org.gorpipe.model.util;

import org.apache.commons.configuration.*;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Configuration Management for system and application config.
 * <p>
 * The key element in GOR configuration management is to always have System.properties represent ALL
 * configuration parameters.  This makes it very simple to pass configuration parameters to GOR from other
 * systems (they only have to update System.properties).
 * <p>
 * This class is a utility class to help us work with config parameters and make sure we honor the above rule.
 * <p>
 * Note:
 * 1. Supports variables within property names, i.e. replaces variable in name with lookup value, i.e.
 * ${xxx}yyy will be someyyy if a property named xxx is defined to some.
 * 2.  This class is loosely based on SystemConfig from Sequence Miner.
 *
 * @version $Id$
 */
public class ConfigUtil {

    public static Configuration config = null;

    /**
     * Loads the configuration from config file or factory
     */
    public static void loadConfig(String prefix) {
        // Return if we have already loaded the config.
        if (config != null) return;

        String fileName = System.getProperty(prefix + ".config.factory");
        try {
            if (fileName != null) {
                DefaultConfigurationBuilder dcb = new DefaultConfigurationBuilder(fileName);
                config = dcb.getConfiguration();
            } else {
                CompositeConfiguration cc = new CompositeConfiguration();
                SystemConfiguration sysconf = new SystemConfiguration();
                sysconf.setTrimmingDisabled(true);
                cc.addConfiguration(sysconf);

                fileName = System.getProperty(prefix + ".config.file");
                if (fileName != null) {
                    cc.addConfiguration(new PropertiesConfiguration(fileName));
                }

                File scriptFile = new File(ConfigUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath());


                // Check on defaults location for config file
                fileName = scriptFile.getParent() + "/../config/" + prefix + ".props";

                if (new File(fileName).exists()) {
                    cc.addConfiguration(new PropertiesConfiguration(fileName));
                }

                // Always add the supplied file as defaults, assume this bin lib setup.
                fileName = scriptFile.getParent() + "/../config/" + prefix + ".props.defaults";

                if (new File(fileName).exists()) {
                    cc.addConfiguration(new PropertiesConfiguration(fileName));
                }

                config = cc;

                // TODO:  Currently we are calling System.getProperty everywhere to access the properties, so
                //        we need to update the system props with the properties read.  This how ever is not
                //        optimal as it makes it hard for example to reload the properties and get the correct
                //        behaviour.
                Iterator<String> keysIt = config.getKeys();
                while (keysIt.hasNext()) {
                    String key = keysIt.next();
                    List valueList = config.getList(key);
                    System.setProperty(key, String.join(",", valueList));
                }
            }
        } catch (Throwable ex) {
            throw new RuntimeException("Could not load configuration file: " + fileName + "\n", ex);
        }
    }
}
