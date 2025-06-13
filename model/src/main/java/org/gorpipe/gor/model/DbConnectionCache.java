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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DbConnectionCache {
    private static final Logger log = LoggerFactory.getLogger(DbConnectionCache.class);

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
        return mapSources.get(source);
    }

    /**
     * Read Database sources from configuration file
     *
     * @param credpath The path to the configuration file
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("WeakerAccess") // Used from gor-services
    public void initializeDbSources(String credpath) throws IOException {
        clearDbSources();
        if (credpath != null && credpath.trim().length() > 0) {
            final Path path = Paths.get(credpath);
            if (Files.notExists(path)) {
                throw new FileNotFoundException("Specified db credentials file (" + credpath + ") is not found");
            }

            final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            List<String[]> partsList = parseLinesForDbSourceInstallation(credpath, lines);

            for (String[] parts : partsList) {
                try {
                    installDbSourceFromParts(parts);
                } catch (ClassNotFoundException e) {
                    log.error("Failed to load driver class {} for db source {}. Please ensure the driver is in the classpath.",
                            parts[1], parts[0], e);
                }
            }
        } else {
            log.info("No db credential path specified");
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
            mapSources.remove(src.name);
        }
        mapSources.clear();
    }

    /**
     * @param source The source to install as available
     */
    public void install(final DbConnection source) {
        log.info("Installing DbSource with name {}, url {} and user {}", source.name, source.url, source.user);
        if (mapSources.containsKey(source.name)) {
            DbConnection existingSource = mapSources.get(source.name);
            log.warn("Installing over an existing source with name {}, url {} and user {}",
                    existingSource.name, existingSource.url, existingSource.user);
        }
        mapSources.put(source.name, source);
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
