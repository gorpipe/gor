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

import org.gorpipe.util.string.StringUtil;
import gorsat.gorsatGorIterator.MapAndListUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for loading and accessing the current reference build. The default reference build is a generic one and not exact
 * but covers the whole genomes with a margin and has no split. This class loads the config file and loads the corresponding
 * build size, build split and reference build path. A warning is logged if any of the three i missing.
 */
public class ReferenceBuild {

    private static final Logger log = LoggerFactory.getLogger(ReferenceBuild.class);
    private String version = "generic";
    private String buildPath = "";
    private Map<String, Integer> buildSize;
    private Map<String, Integer> buildSplit;
    private String cramReferencePath;

    private final static String BUILDPATH_KEY = "buildPath";
    private final static String BUILDSIZE_KEY = "buildSizeFile";
    private final static String BUILDSPLIT_KEY = "buildSplitFile";
    private final static String CRAMREFERENCEPATH_KEY = "cramReferencePath";

    public ReferenceBuild() {
        this.buildSize = ReferenceBuildDefaults.buildSizeGeneric();
        this.buildSplit = ReferenceBuildDefaults.buildSplitGeneric();
    }

    public ReferenceBuild(String version,
                          String buildPath,
                          Map<String, Integer> buildSize,
                          Map<String, Integer> buildSplit,
                          String cramReferencePath) {
        this.version = version;
        this.buildPath = buildPath;
        this.buildSize = buildSize;
        this.buildSplit = buildSplit;
        this.cramReferencePath = cramReferencePath;
    }

    public String getVersion() {
        return version;
    }

    public String getBuildPath() {
        return buildPath;
    }

    public String getCramReferencePath() {
        return cramReferencePath;
    }

    public Map<String, Integer> getBuildSize() {
        return buildSize;
    }

    public Map<String, Integer> getBuildSplit() {
        return buildSplit;
    }

    /**
     * Creates an instance of reference build loaded from the input gor config file. The build size, build split and
     * reference path is loaded from the config file. If any of the three properties are missing a warning gets logged.
     * If the config file is not available the we revert to the default reference build and log a warning.
     *
     * @param configProperties Property map containing properties from the gor config file
     * @param session          Active gor session
     * @return New reference build instance initlaized with the content of the property map.
     */
    public static ReferenceBuild createFromConfig(Map<String, String> configProperties, GorSession session) {
        if (session.getSystemContext().getServer() || configProperties != null) {
            String buildPath = loadPath(BUILDPATH_KEY, session, configProperties, "GorConfig: Variable buildPath not specified in the config file.");
            Map<String, Integer> buildSize = loadConfigMap(BUILDSIZE_KEY, session, configProperties, "GorConfig: Variable buildSizeFile not specified in the config file.");
            Map<String, Integer> buildSplit = loadConfigMap(BUILDSPLIT_KEY, session, configProperties, "GorConfig: Variable buildSplitFile not specified in the config file.");
            String cramReferencePath = loadPath(CRAMREFERENCEPATH_KEY, session, configProperties, "GorConfig: Variable cramReferencePath not specified in the config file.");
            return new ReferenceBuild("",
                    buildPath,
                    buildSize == null ? ReferenceBuildDefaults.buildSizeGeneric() : buildSize,
                    buildSplit == null ? ReferenceBuildDefaults.buildSplitGeneric() : buildSplit,
                    cramReferencePath);
        } else {
            log.warn("No reference file set, reverting to generic reference build.");
            return new ReferenceBuild();
        }

    }

    private static String loadPath(String key, GorSession session, Map<String, String> configProperties, String message) {

        if (configProperties.containsKey(key)) {
            Path configPath = Paths.get(configProperties.get(key));

            if (!session.getSystemContext().getServer() && !configPath.isAbsolute()) {
                configPath = Paths.get(session.getProjectContext().getRealProjectRoot(), configPath.toString());
            }

            return configPath.toString();
        } else {
            log.warn(message);
            return "";
        }
    }

    private static Map<String, Integer> loadConfigMap(String key, GorSession session, Map<String, String> configProperties, String message) {
        String configPath = loadPath(key, session, configProperties, message);

        if (StringUtil.isEmpty(configPath)) {
            return null;
        }

        Map<String, String> tempPropertyMap = MapAndListUtilities.getSingleHashMap(configPath, false, false, session);
        Map<String, Integer> resultMap = new HashMap<>();

        for (Map.Entry<String, String> entry : tempPropertyMap.entrySet()) {
            resultMap.put(entry.getKey(), Integer.parseInt(entry.getValue()));
        }

        return resultMap;
    }
}
