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

package org.gorpipe.gor.session;

import gorsat.gorsatGorIterator.MapAndListUtilities;
import org.gorpipe.client.FileCache;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.*;
import org.gorpipe.gor.reference.ReferenceBuild;
import org.gorpipe.model.gor.iterators.RefSeq;
import org.gorpipe.model.gor.iterators.RefSeqFactory;
import org.gorpipe.model.gor.iterators.RefSeqFromConfigFactory;
import org.gorpipe.gor.util.StringUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Project context stores immutable services and settings directly related to the project in question. This includes
 * reference build, file reader, file cache etc. It supports a Builder pattern to initialize the context.
 */
public class ProjectContext {


    public static final FileReader DEFAULT_READER = new DriverBackedFileReader(System.getProperty("gor.security.context", ""), ".", null);
    public static final String DEFAULT_CACHE_DIR = System.getProperty("java.io.tmpdir");

    private String aliasFile;
    private String configFile;
    private String varJoinType = VARIANT_JOIN_TYPE_DEFAULT;
    private String cacheDir;
    private String logDirectory;
    private ReferenceBuild referenceBuild = new ReferenceBuild();
    private String root;
    private String projectName;
    private FileReader fileReader;
    private FileCache fileCache;
    private GorParallelQueryHandler queryHandler;
    private QueryEvaluator queryEvaluator;
    private RefSeqFactory refSeqFactory;

    private static final String VARIANT_JOIN_TYPE_KEY = "varjointype";
    private static final String VARIANT_JOIN_TYPE_DEFAULT = "undefined";

    private ProjectContext() {}

    public static class Builder {

        private String aliasFile = "gor_aliases.txt";
        private String configFile = "gor_config.txt";
        private String cacheDir = DEFAULT_CACHE_DIR;
        private String logDirectory;
        private String root = ".";
        private String projectName;
        private FileReader fileReader = DEFAULT_READER;
        private FileCache fileCache;
        private GorParallelQueryHandler queryHandler;
        private QueryEvaluator queryEvaluator;
        private RefSeqFactory refSeqFactory;

        public Builder setAliasFile(String aliasFile) {
            this.aliasFile = aliasFile;
            return this;
        }

        public Builder setConfigFile(String configFile) {
            this.configFile = configFile;
            return this;
        }

        public Builder setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        public Builder setLogDirectory(String logDirectory) {
            this.logDirectory = logDirectory;
            return this;
        }

        public Builder setRoot(String root) {
            this.root = root;
            return this;
        }

        public Builder setProjectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder setFileReader(FileReader fileReader) {
            this.fileReader = fileReader;
            return this;
        }

        public Builder setFileCache(FileCache fileCache) {
            this.fileCache = fileCache;
            return this;
        }

        public Builder setQueryHandler(GorParallelQueryHandler queryHandler) {
            this.queryHandler = queryHandler;
            return this;
        }

        public Builder setQueryEvaluator(QueryEvaluator queryEvaluator) {
            this.queryEvaluator = queryEvaluator;
            return this;
        }

        public Builder setRefSeqFactory(RefSeqFactory refSeqFactory) {
            this.refSeqFactory = refSeqFactory;
            return this;
        }

        public ProjectContext build() {
            ProjectContext projectContext = new ProjectContext();
            projectContext.root = root;
            projectContext.aliasFile = aliasFile;
            projectContext.cacheDir = cacheDir;
            projectContext.configFile = configFile;
            projectContext.fileCache = fileCache;
            projectContext.fileReader = fileReader;
            projectContext.logDirectory = logDirectory;
            projectContext.projectName = projectName;
            projectContext.queryEvaluator = queryEvaluator;
            projectContext.queryHandler = queryHandler;
            projectContext.refSeqFactory = refSeqFactory;
            return projectContext;
        }
    }

    public RefSeq createRefSeq() {
        if (refSeqFactory == null) {
            createRefSeqFactory();
        }

        // Lets create the default behaviour if it is not set
        return refSeqFactory.create();
    }

    public String getProjectRoot() {
        return getProjectRootPath().toString();
    }

    public Path getProjectRootPath() {
        String projectRoot = this.root.split("[ \t]+")[0];
        return Paths.get(projectRoot);
    }

    /**
     * The GorRoot is used for the file root AND to store options.  This method parses
     * the project root from the gor root and returns the real path of it (if it exist).
     * <p>
     * TODO: Stop using the GorRoot for options and remove this function.
     *
     * @return the real path of the project root if it exists, otherwise the project root as specified.
     */
    public Path getRealProjectRootPath() {
        Path rootPath = getProjectRootPath();
        if (Files.exists(rootPath)) {
            try {
                rootPath = rootPath.toRealPath();
            } catch (IOException ioe) {
                throw new GorSystemException("Failed to get root folder.", ioe);
            }
        }
        return rootPath;
    }

    /**
     * The GorRoot is used for the file root AND to store options.  This method parses
     * the project root from the gor root and returns the real path of it (if it exist).
     *
     * @return the real path (as string) of the project root if it exists, otherwise the project root as specified.
     */
    public String getRealProjectRoot() {
        return getRealProjectRootPath().toString();
    }


    public String getGorAliasFile() {
        return this.aliasFile;
    }

    public String getGorConfigFile() {
        return this.configFile;
    }

    public String getRoot() {
        return this.root;
    }

    public String getVarJoinType() {
        return this.varJoinType;
    }

    public String getCacheDir() {
        return this.cacheDir;
    }

    public String getLogDirectory() {
        return this.logDirectory;
    }

    public ReferenceBuild getReferenceBuild() {
        return this.referenceBuild;
    }

    public String getProjectName() {
        return this.projectName;
    }

    public FileReader getFileReader() {
        return this.fileReader;
    }

    public FileCache getFileCache() {
        return this.fileCache;
    }

    public GorParallelQueryHandler getQueryHandler() {
        return this.queryHandler;
    }

    public QueryEvaluator getQueryEvaluator() {
        return this.queryEvaluator;
    }

    public void loadReferenceBuild(GorSession session) {
        if (StringUtil.isEmpty(this.configFile)) return;

        Map<String, String> configProperties = MapAndListUtilities.getSingleHashMap(this.configFile, false, false, session);
        this.referenceBuild = ReferenceBuild.createFromConfig(configProperties, session);
        this.varJoinType = configProperties.getOrDefault(VARIANT_JOIN_TYPE_KEY, VARIANT_JOIN_TYPE_DEFAULT);
        createRefSeqFactory();
    }

    private void createRefSeqFactory() {
        if (this.refSeqFactory == null) {
            this.refSeqFactory = new RefSeqFromConfigFactory(this.referenceBuild.getBuildPath(), this.fileReader);
        }
    }
}
