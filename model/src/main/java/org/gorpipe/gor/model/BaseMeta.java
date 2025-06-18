package org.gorpipe.gor.model;


import org.apache.commons.lang3.StringUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.util.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Helper class to implement meta files.
 * - Contains common properties.
 * - Helper methods to read and write meta files.
 */
public class BaseMeta {
    private static final Logger log = LoggerFactory.getLogger(BaseMeta.class);

    public static final String NO_SERIAL = "0";

    // Basic properties
    public static final String HEADER_VERSION_KEY = "VERSION";
    public static final String HEADER_FILE_FORMAT_KEY = "FILE_FORMAT";
    public static final String HEADER_SERIAL_KEY = "SERIAL";
    public static final String HEADER_CREATED_KEY = "CREATED";
    public static final String HEADER_LINE_COUNT_KEY = "LINE_COUNT";
    public static final String HEADER_SCHEMA_KEY = "SCHEMA";
    public static final String MAXSEG_SCHEMA_KEY = "MAXSEG";
    public static final String HEADER_MD5_KEY = "MD5";
    public static final String HEADER_TAGS_KEY = "TAGS";
    public static final String HEADER_COLUMNS_KEY = "COLUMNS";

    protected ConcurrentHashMap<String, String> headerProps;
    String[] fileHeader;                     // Columns of the table itself.
    protected boolean saveHeaderLine = false;
    private String metaPathStr;

    /**
     *
     */
    public BaseMeta() {
        this.headerProps = new ConcurrentHashMap<>();
        clear();
    }

    /**
     * Get header property.
     *
     * @param key name of the property.
     * @return the header property identifed with [key], null if the key does not exists.
     */
    public String getProperty(String key) {
        return headerProps.get(key);
    }

    /**
     * Get header property.
     *
     * @param key name of the property.
     * @param defValue default value
     * @return the header property identified with [key]
     */
    public String getProperty(String key, String defValue) {
        return headerProps.containsKey(key) ? headerProps.get(key) : defValue;
    }

    /**
     * Get header property.
     *
     * @param key name of the property.
     * @param defValue default value
     * @return the header property identified with [key]
     */
    public long getPropertyLong(String key, long defValue) {
        return headerProps.containsKey(key) ? Long.parseLong(headerProps.get(key)) : defValue;
    }

    /**
     * Get header property.
     *
     * @param key name of the property.
     * @param defValue default value
     * @return the header property identified with [key]
     */
    public int getPropertyInt(String key, int defValue) {
        return headerProps.containsKey(key) ? Integer.parseInt(headerProps.get(key)) : defValue;
    }

    /**
     * Set header property.
     *
     * @param key   name of the property.
     * @param value new value of the property.
     */
    public void setProperty(String key, String value) {
        headerProps.put(key, value != null ? value : "");
    }

    /**
     * Set header properties.
     *
     * @param props   map of properites
     */
    public void setProperties(Map<String, String> props) {
        headerProps.putAll(props);
    }

    /**
     * Returns <code>true</code> if this header contains the property.
     *
     * @param key The property key.
     * @return <code>true</code> if this header contains the property.
     */
    public boolean containsProperty(String key) {
        return headerProps.containsKey(key);
    }

    /**
     * Get the list of available properties.
     */
    public Set<String> getPropertyKeys() {
        return headerProps.keySet();
    }

    /**
     * Clear the header info.
     */
    public void clear() {
        this.headerProps.clear();
        setProperty(HEADER_SERIAL_KEY, NO_SERIAL);
    }

    /**
     * Check if proper header.
     *
     * @return true if the header is a proper header otherwise false.  Header is proper if it has defined contentColumns and they are not dummy contentColumns.
     */
    public boolean isProper() {
        String[] contentColumns = getColumns();
        return contentColumns != null && contentColumns.length > 2 && contentColumns[2].equalsIgnoreCase("col3");
    }

    public void setColumns(String[] columns) {
        String[] contentColumns = Stream.of(columns).filter(Objects::nonNull).map(String::trim).filter(h -> h.length() > 0).toArray(size -> new String[size]);
        setProperty(HEADER_COLUMNS_KEY, String.join(",", contentColumns));
    }

    public String[] getColumns() {
        return containsProperty(HEADER_COLUMNS_KEY) ? getProperty(HEADER_COLUMNS_KEY).split(",") : new String[]{};
    }

    public void setMd5(String md5) {
        setProperty(HEADER_MD5_KEY, md5);
    }

    public String getMd5() {
        return getProperty(HEADER_MD5_KEY);
    }

    public void setTags(String tags) {
        setProperty(HEADER_TAGS_KEY, tags);
    }

    public String getTags() {
        return getProperty(HEADER_TAGS_KEY);
    }

    public long getLineCount() {
        if (headerProps.containsKey(HEADER_LINE_COUNT_KEY)) {
            return Long.parseLong(headerProps.get(HEADER_LINE_COUNT_KEY));
        }
        return -1L;
    }

    public String[] getFileHeader() {
        return fileHeader;
    }

    public void setFileHeader(String[] fileHeader) {
        this.fileHeader = fileHeader;
    }

    public String getVersion() {
        return getProperty(HEADER_VERSION_KEY, "0");
    }

    /**
     * Parse header line.
     *
     * @param line line to parse.
     */
    public void parseLine(String line) {
        if (line == null) return;

        if (line.startsWith("##")) {
            parsePropertyLine(line);
        } else {
            parseHeaderLine(line);
        }
    }

    protected void parsePropertyLine(String line) {
        String[] lineSplit = line.split("[=:]", 2);
        String propName = StringUtils.strip(lineSplit[0], "\t\n #");
        String propValue = lineSplit.length > 1 ? lineSplit[1].trim() : "";

        if (propName.equals(HEADER_COLUMNS_KEY)) {
            // Ignore columns that have non standard characters.  These are errors.
            if (StandardCharsets.UTF_8.newEncoder().canEncode(lineSplit[1])) {
                setColumns(propValue.split("[\t,]", -1));
            }
        } else {
            setProperty(propName, propValue);
        }
    }

    protected void parseHeaderLine(String line) {
        if (containsProperty(HEADER_COLUMNS_KEY)) {
            return;
        }
        String columnsString = getColumnStringFromHeaderLine(line);

        if (columnsString.length() > 0) {
            setFileHeader(columnsString.split("[\t,]", -1));
            setColumns(fileHeader);
        }
    }

    /*
     * Header from column compressed gorz file can contain binary block that begins with 0.  See: GorzSeekableIterator.
     * TODO:  This should be part of the driver framework (FileReader?) and not repeated here and in GorzSeekableIterator.
     */
    private String getColumnStringFromHeaderLine(String header) {
        int idx = 0;
        String columns = StringUtils.strip(header,  "\n #");
        byte[] columnBytes = columns.getBytes();
        while (idx < columnBytes.length && columnBytes[idx++] != 0);
        return idx != columnBytes.length ?  new String(columnBytes, 0, idx - 1) : columns;
    }

    /**
     * Check if the given line is a header line.
     *
     * @param line line to check.
     * @return true if the given line is a table header line.
     */
    public static boolean isHeaderLine(String line) {
        return line != null && line.startsWith("#");
    }

    /**
     * Format the header for outputting into a file.
     *
     * @return the header formatted for outputting into a table file.
     */
    public String formatHeader() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this.headerProps.entrySet()) {
            sb.append(String.format("## %s = %s%n", entry.getKey(), entry.getValue()));
        }

        if (fileHeader != null && saveHeaderLine) {
            sb.append(String.format("#%s%n", String.join("\t", this.fileHeader)));
        }

        return sb.toString();
    }

    private void parseMetaReader(BufferedReader br) throws IOException {
        parseMetaLines(extractMetaReader(br));
    }

    private void parseMetaLines(List<String> metaLines) throws IOException {
        for (String line : metaLines) {
            parseLine(line);
        }
    }

    protected List<String> extractMetaReader(BufferedReader br) throws IOException {
        String line;
        List<String> metaLines = new ArrayList<>();
        boolean isFirstLine = true;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0) {
                if (isHeaderLine(line) || (isFirstLine && metaPathStr != null &&
                        // gorz and norz contain headerline that does not befin with #
                        (DataUtil.isGorz(metaPathStr)
                                || DataUtil.isNorz(metaPathStr)))) {
                    metaLines.add(line);
                } else {
                    // Done reading the header.
                    break;
                }
            }
            isFirstLine = false;
        }
        return metaLines;
    }

    public void loadAndMergeMeta(Path metaPath) {
        loadAndMergeMeta(new DriverBackedFileReader(""), metaPath.toString());
    }

    public String getMetaPath() {
        return this.metaPathStr;
    }

    public void loadAndMergeMeta(FileReader fileReader, String metaPath) {
        this.metaPathStr = metaPath;

        try {
            if (metaPath == null || !fileReader.resolveUrl(new SourceReferenceBuilder(metaPath)
                    .commonRoot(fileReader.getCommonRoot())
                    .securityContext(fileReader.getSecurityContext()).isFallback(false).build()).exists()) {
                return;
            }
        } catch (GorResourceException e) {
            return;
        }
        try (var br = new BufferedReader(new InputStreamReader(fileReader.getInputStream(metaPath)))) {
            parseMetaReader(br);
        } catch (IOException ex) {
            throw new GorResourceException(String.format("Error reading meta. Can not read file '%s' (%s)", metaPath, ex.getMessage()), metaPath, ex);
        }
    }

    public void loadAndMergeMeta(String content) {
        List<String> metaLines = null;
        try {
            metaLines = extractMetaReader(new BufferedReader(new StringReader(content)));
            parseMetaLines(metaLines);
        } catch (IOException ex) {
            throw new GorResourceException(String.format("Error reading meta: (%s%n%s ",
                    ex.getMessage(), metaLines != null ? String.join("\n", metaLines) : "no lines"), null,  ex);
        }
    }

    protected void save(FileReader fileReader) {
        saveAs(fileReader, this.metaPathStr);
    }

    protected void saveAs(FileReader fileReader, String fileName) {
        try(OutputStream os = fileReader.getOutputStream(fileName))  {
            os.write(formatHeader().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            throw new GorSystemException(String.format("Could not save meta file %s", fileName), ioe);
        }
    }
}
