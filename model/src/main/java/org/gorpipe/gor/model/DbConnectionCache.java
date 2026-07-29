package org.gorpipe.gor.model;

import org.gorpipe.exceptions.GorResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DbConnectionCache {
    private static final Logger log = LoggerFactory.getLogger(DbConnectionCache.class);

    /**
     * Map of DbConnection objects, keyed by the source name in lower case, to make the name case insensitive.
     */
    private final ConcurrentHashMap<String, DbConnection> mapSources = new ConcurrentHashMap<>();
    public String defaultDbSource = "rda";

    static final String ENV_RDA_SOURCE_NAME = "rda";
    static final String ENV_RDA_URL = "APPSERVER_RDA_URL";
    static final String ENV_RDA_USERNAME = "APPSERVER_RDA_USERNAME";
    static final String ENV_RDA_PASSWORD = "APPSERVER_RDA_PASSWORD";
    static final String ENV_RDA_DRIVER = "APPSERVER_RDA_DRIVER";

    public DbConnectionCache() {
    }

    public DbConnectionCache(String defaultDbSource) {
        this.defaultDbSource = defaultDbSource;
    }

    /**
     * Lookup the specified source
     *
     * @param source The name of the source
     * @return The DbSource object
     */
    public DbConnection lookup(String source) {
        return mapSources.get(source.toLowerCase());
    }

    /**
     * Read database sources from the environment and from the configuration file.
     *
     * Sources may come from two places. Environment variables carry credentials that rotate and so
     * cannot be baked into a file — see {@link #parseEnvForDbSourceInstallation}. The credentials
     * file carries the static set of additional databases gor can reach. A deployment typically
     * supplies the rotating system credentials through the environment and the remaining resources
     * through the file.
     *
     * @param credpath The path to the configuration file
     * @throws IOException if the credentials file is configured but missing, and no env source was installed
     */
    @SuppressWarnings("WeakerAccess") // Used from gor-services
    public void initializeDbSources(String credpath) throws IOException {
        initializeDbSources(credpath, System.getenv());
    }

    /**
     * Read database sources from the environment and from the configuration file.
     *
     * Env-defined sources are installed first so that a file row with the same name takes
     * precedence over it. A deployment that wants the environment to be authoritative for a given
     * source must therefore keep a row of that name out of the credentials file — otherwise the
     * file's static copy shadows the rotating one.
     *
     * @param credpath The path to the configuration file
     * @param env      The environment to read env-defined sources from
     */
    void initializeDbSources(String credpath, Map<String, String> env) throws IOException {
        clearDbSources();
        List<String[]> envParts = parseEnvForDbSourceInstallation(env);
        installAllFromParts(envParts);
        installAllFromParts(readFileForDbSourceInstallation(credpath, !envParts.isEmpty()));
    }

    private List<String[]> readFileForDbSourceInstallation(String credpath, boolean haveEnvSources) throws IOException {
        if (credpath == null || credpath.trim().length() == 0) {
            log.info("No db credential path specified");
            return Collections.emptyList();
        }

        final Path path = Paths.get(credpath);
        if (Files.notExists(path)) {
            if (haveEnvSources) {
                log.warn("Specified db credentials file ({}) is not found, continuing with db sources from the environment", credpath);
                return Collections.emptyList();
            }
            throw new FileNotFoundException("Specified db credentials file (" + credpath + ") is not found");
        }

        final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return parseLinesForDbSourceInstallation(credpath, lines);
    }

    private void installAllFromParts(List<String[]> partsList) {
        for (String[] parts : partsList) {
            try {
                installDbSourceFromParts(parts);
            } catch (ClassNotFoundException e) {
                log.error("Failed to load driver class {} for db source {}. Please ensure the driver is in the classpath.",
                        parts[1], parts[0], e);
            }
        }
    }

    public static List<String[]> parseLinesForDbSourceInstallation(String credpath, List<String> lines) {
        int linecnt = 1;
        String partsSplitRegex = "\t";

        if (lines.size() == 1 && lines.get(0).contains("\\n")) {
            log.debug("Fixing up credentials data, since its in one line form with \\n and \\t");
            List<String> newLines = Arrays.asList(lines.get(0).split("\\\\n"));
            partsSplitRegex = "\\\\t";
            log.debug("New lines is {}", newLines);
            lines.clear();
            lines.addAll(newLines);
        }

        log.debug("Credentials data is {}, line count is {}", lines, lines.size());

        List<String[]> partsList = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // Name, Driver, URL, User [, Password]
            log.debug("Working with line {} of credentials data containing data {}", i, lines.get(i));
            if (lines.get(i).trim().startsWith("#")) {
                log.debug("Ignoring line {}", lines.get(i));
                continue;
            }

            String[] parts = lines.get(i).split(partsSplitRegex);
            if (parts.length >= 4) {
                partsList.add(parts);
            } else {
                log.error("Line {} in {} doesn't contain valid db source definition\n", linecnt, credpath);
            }
            linecnt++;
        }
        return partsList;
    }

    /**
     * Build db source definitions from environment variables.
     *
     * Returns entries in the same {name, driver, url, user[, pwd]} shape as
     * parseLinesForDbSourceInstallation, so both paths share the install code.
     * Never logs credential values, only variable names.
     *
     * @param env the environment to read, normally System.getenv()
     * @return zero or one db source definition
     */
    static List<String[]> parseEnvForDbSourceInstallation(Map<String, String> env) {
        List<String[]> partsList = new ArrayList<>();
        if (env == null) {
            return partsList;
        }

        String url = trimToNull(env.get(ENV_RDA_URL));
        String user = trimToNull(env.get(ENV_RDA_USERNAME));
        // Blank counts as unset here, as it does for url and username: an env var that is present but
        // empty is a missing value, not a real empty password.
        String pwd = trimToNull(env.get(ENV_RDA_PASSWORD));

        if (url == null && user == null) {
            return partsList;
        }
        if (url == null || user == null) {
            log.warn("Incomplete db source configuration in environment for source {}: {} is not set. Ignoring it.",
                    ENV_RDA_SOURCE_NAME, url == null ? ENV_RDA_URL : ENV_RDA_USERNAME);
            return partsList;
        }

        String driver = trimToNull(env.get(ENV_RDA_DRIVER));
        if (driver == null) {
            driver = driverClassForUrl(url);
        }
        if (driver == null) {
            log.warn("Could not derive a jdbc driver for db source {} from its url, and {} is not set. Ignoring it.",
                    ENV_RDA_SOURCE_NAME, ENV_RDA_DRIVER);
            return partsList;
        }

        if (pwd == null) {
            partsList.add(new String[]{ENV_RDA_SOURCE_NAME, driver, url, user});
        } else {
            partsList.add(new String[]{ENV_RDA_SOURCE_NAME, driver, url, user, pwd});
        }
        return partsList;
    }

    /**
     * @param url a jdbc url
     * @return the matching driver class name, or null if the prefix is not recognized
     */
    static String driverClassForUrl(String url) {
        if (url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        if (url.startsWith("jdbc:oracle:")) {
            return "oracle.jdbc.driver.OracleDriver";
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void installDbSourceFromParts(String[] parts) throws ClassNotFoundException {
        Class.forName(parts[1]); // Just load the driver once and for all
        final DbConnection source = new DbConnection(parts[0], parts[2], parts[3], parts.length > 4 ? parts[4] : null);
        install(source);
    }

    /**
     * Remove and disconnect all data sources previously loaded with initializeDbSources
     */
    public void clearDbSources() {
        for (DbConnection src : mapSources.values()) {
            src.close();
            mapSources.remove(src.name.toLowerCase());
        }
        mapSources.clear();
    }

    /**
     * @param source The source to install as available
     */
    public void install(final DbConnection source) {
        log.info("Installing DbSource with name: {}, url: {} and user: {}", source.name, source.url, source.user);
        String key = source.name.toLowerCase();
        if (mapSources.containsKey(key)) {
            DbConnection existingSource = mapSources.get(key);
            log.warn("Installing over an existing source with name: {}, url: {} and user: {}",
                    existingSource.name, existingSource.url, existingSource.user);
        }
        mapSources.put(key, source);
    }


    /**
     * Wrap a DbNorIterator into Stream as a proper source to db source link files out into the gor system.
     *
     * @param content
     * @param constants
     * @param source
     * @return Stream tsv formatted lines with header first and data lines following.
     */
    @SuppressWarnings("squid:S2095") //resource should not be closed since it being closed by the return object
    public Stream<String> getDBLinkStream(String content, Map<String, Object> constants, String source) {
        final DbConnection dbsource = lookup(nullSafeSource(source));
        if (dbsource == null) {
            throw new GorResourceException("Error: Did not find database source named "+ nullSafeSource(source) +". ", content);
        }

        DbNorIterator dbnor = new DbNorIterator(content, constants, dbsource.getConnectionPool());
        Iterable<String> iterable = () -> dbnor;
        Stream<String> stream = StreamSupport.stream(iterable.spliterator(), false);
        stream.onClose(dbnor::close);
        return stream;
    }

    private String nullSafeSource(String source) {
        return (source == null) ? defaultDbSource : source;
    }
}
