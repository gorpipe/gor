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

package org.gorpipe.gor.table;

import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by gisli on 21/06/2017.
 */
public class PathUtils {

    private PathUtils() {}

    /**
     * Get normalized absolute path
     *
     * @param path input path.  Relative paths are considered relative to the dicionary root.
     * @return normalized absolute path of {@code path}.
     */

    public static URI resolve(URI root, URI path) {
        if (path == null) {
            return null;
        }

        if (isAbsolutePath(path)) {
            return normalize(path);
        }

        // The uri must end with / for the resolve to work correctly.
        if (!root.toString().endsWith("/")) {
            root = URI.create(root+"/");
        }
        return normalize(root.resolve(path));
    }

    public static String resolve(URI root, String path) {
        return resolve(root,URI.create(path)).toString();
    }

    public static String resolve(String root, String path) {
        return root!=null && root.length()>0 ? resolve(URI.create(root),path) : path;
    }

    public static Path resolve(Path root, Path path) {
        if (path == null) {
            return null;
        }

        if (path.isAbsolute()) {
            return normalize(path);
        }

        return normalize(root.resolve(path));
    }

    public static Path resolve(Path root, String path) {
        return resolve(root, Path.of(path));
    }

    /**
     * Get relativize to table or absolute path.
     *
     * @param path the path to relativize.
     * @return relative to the root path if {@code path} is relative or starts with the table root path, otherwise absolute path is returned.
     * The path is also normalized.
     */
    public static Path relativize(Path root, Path path) {
        if (path == null) {
            return null;
        }
        Path norm = normalize(path);
        // Need to help path to do this right.
        return norm.startsWith(root) ? root.relativize(norm) : norm;
    }

    public static String relativize(URI root, String path) {
        if (path == null) {
            return null;
        }
        URI relURI = normalize(root.relativize(URI.create(path)));
        return relURI.toString();
    }


    public static boolean isAbsolutePath(String path) {
        return path.startsWith("/") || path.contains("://");
    }

    public static boolean isAbsolutePath(URI path) {
        return path.isAbsolute() || path.getPath().startsWith("/");
    }

    /*
     * Needed to add some URI helper methods because of limitations in how URI works.
     *
     * Some problems with URI:
     * 1. Paths.get(uri) fails if uri does not contain schema or if contains fragment, i.e. does not work
     *    properly for relative paths.
     * 2. path.toUri() transforms the path to absolute path, i.e. does not work for relative paths.
     * 3. Some Uri functions (normalize and resolve) transform file:/// to file:/, but we want file:/// so this
     *    is consistant between uris and this is also needed by other Nextcode code.
     * 4. Uri equal reqiuires exact match file:/path and file:///path are not equal???? At least there is no startsWith
     *    method so we need to transform to string or path to do that comparison.     8
     */

    /**
     * Normailze URI.  Handles scheme for files better than the uri.normalize() method.
     *
     * @param uri URI to normalize.
     * @return normalized URI.
     */
    public static URI normalize(URI uri) {
        return fixFileSchema(uri.normalize());
    }

    public static String normalize(String uri) {
        return normalize(URI.create(uri)).toString();
    }

    public static Path normalize(Path path) {
        return Paths.get(fixFileSchema(path.normalize().toString()));
    }

    /**
     * Convert URI to path.
     * Handles scheme better than the Paths.get(uri), and removes file: from the resulting Path.
     *
     * @param uri uri to convert.
     * @return Path object representing the URI.
     */
    public static Path toPath(URI uri) {
        return Paths.get(formatUri(uri));
    }

    public static Path toPath(String uri) {
        return Paths.get(formatUri(uri));
    }

    /**
     * Format the uri as string.  Decodes and formats the URI.
     *
     * @param uri uri to format, must have valid file: format.
     * @return uri formatted as string.
     */
    public static String formatUri(URI uri) {
        return fixFileSchema(uri.toString());
    }

    public static String formatUri(String uri) {
        return fixFileSchema(uri);
    }

    public static URI toRealPath(URI uri) {
        if (isLocal(uri)) {
            try {
                return Path.of(uri.getPath()).toRealPath().toUri();
            } catch (IOException e) {
                // Ignore/
            }
        }

        return uri;
    }

    public static URI fixFileSchema(URI uri) {
        return URI.create(fixFileSchema(uri.toString()));
    }

    public static String fixFileSchema(String uri) {
        if (uri.startsWith("file:/")) {
            if (uri.startsWith("file:///")) {
                return uri.substring(7);
            } else {
                return uri.substring(5);
            }
        } else {
            return uri;
        }
    }

    public static URI fixFileSchemaUseFile(URI uri) {
        if ("file".equals(uri.getScheme())) {
            String uriStr = uri.toString();
            // If we have ssp we know it has the right format (file:////<path>), other wise it is just file:/<path>
            if (uriStr.startsWith("file:///")) {
                return uri;
            } else {
                return URI.create("file://" + uriStr.substring(5));
            }
        } else if (uri.getScheme() == null && uri.getPath().startsWith("/")) {
            return URI.create("file://" + uri.toASCIIString());
        } else {
            return uri;
        }
    }

    public static boolean isLocal(URI path) {
        return path.getScheme() == null || path.getScheme().equals("file");
    }

    public static boolean isLocal(String path) {
        return !path.contains(":/") || path.startsWith("file");
    }

    public static long getLastModifiedTime(String fileName, String securityContext, String commonRoot) throws IOException {
        //TODO: This method should really take in SourceReference or better yet be removed and replaced with calls to datasource.getUniqueId
        DataSource ds = GorDriverFactory.fromConfig().getDataSource(new SourceReferenceBuilder(fileName).securityContext(securityContext).commonRoot(commonRoot).build());
        if (ds != null) {
            return ds.getSourceMetadata().getLastModified();
        } else {
            return System.currentTimeMillis();
        }
    }

}
