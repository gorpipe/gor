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
package org.gorpipe.querydialogs.factory;

import org.gorpipe.gor.model.FileReader;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.*;


/**
 * Base class for all argument builders.
 *
 * @author arnie
 * @version $Id$
 */
public abstract class ArgumentBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ArgumentBuilder.class);
    private static Map<String, String> pathMap = null;
    private final FileReader fileResolver;
    private boolean ignoreAllowedMismatch = false;

    public ArgumentBuilder(FileReader fileResolver) {
        this.fileResolver = fileResolver;
    }

    public void setIgnoreAllowedMismatch(boolean ignoreAllowedMismatch) {
        this.ignoreAllowedMismatch = ignoreAllowedMismatch;
    }

    public boolean ignoreAllowedMismatch() {
        return ignoreAllowedMismatch;
    }

    private static synchronized Map<String, String> getPathMap(FileReader fileResolver) throws IOException {
        if (pathMap == null) {
            pathMap = new HashMap<>();
            String path = System.getProperty("dialog.aliasmap", null);
            if (path != null) {
                for (String line : fileResolver.readAll(path)) {
                    String[] parts = line.split("\\t");
                    if (parts.length > 1) {
                        pathMap.put(parts[0], parts[1]);
                    }
                }
            }
        }

        return pathMap;
    }

    protected static String safeString(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    protected static List<? extends Object> getAllowedValues(Map<String, ? extends Object> attributes) {
        List<Object> allowedValues = new ArrayList<>();
        if (attributes.containsKey("values")) {
            for (Object o : (List<Object>) attributes.get("values")) {
                if (Number.class.isAssignableFrom(o.getClass())) {
                    allowedValues.add(o);
                } else if (Date.class.isAssignableFrom(o.getClass())) {
                    allowedValues.add(o);
                } else {
                    allowedValues.add(o.toString());
                }
            }
        } else if (attributes.containsKey("values_path")) {
            allowedValues = Argument.DEFERRED_LIST;
        }
        return allowedValues;
    }

    /**
     * Builds an arguments based on supplied attributes.
     *
     * @param name       - the name of the argument
     * @param attributes - attributes specific to an argument type
     * @return an appropriate argument
     */
    public abstract Argument build(String name, Map<String, ? extends Object> attributes);

    protected URI getValuesPath(Map<String, ? extends Object> attributes) {
        try {
            String resource = safeString(attributes.get("values_path"));
            if (resource == null) return null;
            if (getPathMap(fileResolver).containsKey(resource)) {
                resource = getPathMap(fileResolver).get(resource).trim();
            }
            return URI.create(resource);
        } catch (NullPointerException e) {
            return null;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid valuesPath", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load valuesPath", e);
        }
    }

    protected Integer getDisplayWidth(final Map<String, ? extends Object> attributes) {
        return (Integer) attributes.get("display_width");
    }

    protected ArgumentDescription getArgumentDescription(final Map<String, ? extends Object> attributes, final String name) {
        final String description = safeString(attributes.get("description"));
        final String displayName = safeString(attributes.get("display_name"));
        final String tooltip = safeString(attributes.get("tooltip"));
        return new ArgumentDescription(name, description, displayName, tooltip);
    }
}
