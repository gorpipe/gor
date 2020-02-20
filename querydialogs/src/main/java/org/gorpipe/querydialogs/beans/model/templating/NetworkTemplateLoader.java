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

package org.gorpipe.querydialogs.beans.model.templating;

import org.gorpipe.model.genome.files.gor.FileReader;
import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;

/**
 * @author arnie
 * @version $Id$
 */
public class NetworkTemplateLoader implements TemplateLoader {
    private String basePath;
    private FileReader fileResolver;

    /**
     * @param basePath path to directory relative to project root where template files can be found
     */
    public NetworkTemplateLoader(String basePath, FileReader fileResolver) {
        this.fileResolver = fileResolver;
        this.basePath = basePath;
        if (!this.basePath.endsWith("/")) {
            this.basePath += "/";
        }
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        // TODO hacky way of determining if resource actually exists
        try (Reader r = fileResolver.getReader(basePath + name)) {
            /* intentionally empty */
        } catch (IOException e) {
            return null;
        }

        return new NetworkTemplateSource(basePath + name);
    }

    @Override
    public long getLastModified(Object templateSource) {
        return ((NetworkTemplateSource) templateSource).lastModified;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        NetworkTemplateSource source = (NetworkTemplateSource) templateSource;
        // TODO replace with the following when ready:
        // source.reader = new StringReader(new String(Files.readAllBytes(PathUtil.pathFromProjectRoot(source.path))));
        // Note: eliminates the need to close the reader in closeTemplateSource
        if (source.reader == null) {
            source.reader = fileResolver.getReader(source.path);
        }
        return source.reader;
    }

    @Override
    public void closeTemplateSource(Object templateSource)
            throws IOException {
        NetworkTemplateSource source = (NetworkTemplateSource) templateSource;
        if (source.reader != null) {
            source.reader.close();
            source.reader = null;
        }
    }

    private static class NetworkTemplateSource {
        private final String path;
        private final long lastModified;
        private Reader reader;

        public NetworkTemplateSource(String path) {
            this.path = path;
            this.lastModified = System.currentTimeMillis();
        }
    }
}