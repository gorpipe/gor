package org.gorpipe.gor.util;

import org.gorpipe.gor.table.util.PathUtils;

import java.net.URI;
import java.nio.file.Path;

public class DataSourceUri {

    private final URI uri;

    private DataSourceUri(URI uri) {
        assert(uri != null);
        this.uri = uri;
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    public URI getUri() {
        return uri;
    }

    public DataSourceUri toFolder() {
        // The uri folder path must end with / for the resolve to work as Path resolve.
        var folder = uri.toString();
        folder = folder.endsWith("/") ? folder : folder + "/";

        return normalize(folder);
    }

    public Path getPath() {
        return Path.of(uri);
    }

    public boolean isLocal() {
        return uri.getScheme() == null || uri.getScheme().equals("file");
    }

    public DataSourceUri getParent() {
        return new DataSourceUri(uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve("."));
    }

    public static DataSourceUri parse(String uri) {
        uri = fixFileSchema(fixLegacySchema(uri));
        return new DataSourceUri(URI.create(uri));
    }

    public static DataSourceUri parse(URI uri) {
        return parse(uri.toString());
    }

    public static DataSourceUri normalize(DataSourceUri uri) {
        return   normalize(uri.toString());
    }

    public static DataSourceUri normalize(String uri) {
        return new DataSourceUri(URI.create(fixFileSchema(fixLegacySchema(convertSlashes(uri)))).normalize());
    }

    public static DataSourceUri normalize(Path path) {
        return normalize(path.toString());
    }

    private static String fixLegacySchema(String uri) {
        var uriLc = uri.toLowerCase();
        return PathUtils.fixDbSchema(uriLc);
    }

    private  static String fixFileSchema(String uri) {
        // TODO:  Should we search for file: or file:/ (and file:// or file:///).  Difference is are we doing this
        //        only for abs pahts or all paths.
        if (uri.startsWith("file:")) {
            if (uri.startsWith("file://")) {
                uri = uri.substring(7);
            } else {
                uri = uri.substring(5);
            }

            // Windows full path hack
            if (uri.length() > 3 && uri.charAt(2) == ':' && Util.isWindowsOS() ) {
                uri = uri.substring(1);
            }
        }

        return uri;
    }

    private static String convertSlashes(String path) {
        return path.replace('\\', '/');
    }
}
