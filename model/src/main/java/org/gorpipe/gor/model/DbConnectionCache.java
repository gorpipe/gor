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
     * Read database sources from the configuration file.
     *
     * @param credpath The path to the configuration file
     * @throws IOException if the credentials file is configured but missing
     */
    @SuppressWarnings("WeakerAccess") // Used from gor-services
    public void initializeDbSources(String credpath) throws IOException {
        initializeDbSources(credpath, List.of());
    }

    /**
     * Read database sources from caller-supplied credentials and from the configuration file.
     *
     * Sources may come from two places. The caller can pass credentials directly — useful for
     * credentials that rotate and so cannot be baked into a file, which the host application may
     * source from its own configuration or a secret manager. The credentials file carries the
     * static set of additional databases gor can reach.
     *
     * Supplied credentials are installed first, so a file row with the same name takes precedence.
     * A deployment that wants its supplied credentials to be authoritative for a given source must
     * therefore keep a row of that name out of the credentials file — otherwise the file's static
     * copy shadows the rotating one.
     *
     * @param credpath    The path to the configuration file
     * @param credentials Credentials to install before reading the file. May be empty.
     * @throws IOException if the credentials file is configured but missing, and no credentials were installed
     */
    public void initializeDbSources(String credpath, List<DbCredentials> credentials) throws IOException {
        clearDbSources();
        List<String[]> suppliedParts = toPartsForInstallation(credentials);
        installAllFromParts(suppliedParts);
        installAllFromParts(readFileForDbSourceInstallation(credpath, !suppliedParts.isEmpty()));
    }

    private List<String[]> readFileForDbSourceInstallation(String credpath, boolean haveSuppliedSources) throws IOException {
        if (credpath == null || credpath.trim().length() == 0) {
            log.info("No db credential path specified");
            return Collections.emptyList();
        }

        final Path path = Paths.get(credpath);
        if (Files.notExists(path)) {
            if (haveSuppliedSources) {
                log.warn("Specified db credentials file ({}) is not found, continuing with the supplied db sources", credpath);
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
     * Convert caller-supplied credentials into the {name, driver, url, user[, pwd]} shape that
     * parseLinesForDbSourceInstallation produces, so both paths share the install code.
     *
     * Incomplete credentials are skipped with a warning rather than failing initialization. Blank
     * values count as unset, including the password: a secret manager or template that renders an
     * empty value is expressing a missing value, not a real empty password.
     *
     * Never logs credential values, only the source name and which field was missing.
     *
     * @param credentials the credentials to convert, may be null
     * @return one entry per usable credential
     */
    static List<String[]> toPartsForInstallation(List<DbCredentials> credentials) {
        List<String[]> partsList = new ArrayList<>();
        if (credentials == null) {
            return partsList;
        }

        for (DbCredentials cred : credentials) {
            if (cred == null) {
                continue;
            }

            String name = trimToNull(cred.name());
            String url = trimToNull(cred.url());
            String user = trimToNull(cred.user());
            String pwd = trimToNull(cred.pwd());

            if (name == null || url == null || user == null) {
                log.warn("Incomplete db source credentials for source {}: {} is not set. Ignoring it.",
                        name == null ? "<unnamed>" : name,
                        name == null ? "name" : (url == null ? "url" : "user"));
                continue;
            }

            String driver = trimToNull(cred.driver());
            if (driver == null) {
                driver = driverClassForUrl(url);
            }
            if (driver == null) {
                log.warn("Could not derive a jdbc driver for db source {} from its url, and no driver was given. Ignoring it.",
                        name);
                continue;
            }

            if (pwd == null) {
                partsList.add(new String[]{name, driver, url, user});
            } else {
                partsList.add(new String[]{name, driver, url, user, pwd});
            }
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
