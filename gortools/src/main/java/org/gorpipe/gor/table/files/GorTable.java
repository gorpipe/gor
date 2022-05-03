package org.gorpipe.gor.table.files;

import gorsat.process.CLIGorExecutionEngine;
import gorsat.process.PipeOptions;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.model.RowBase;
import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.TableLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Table class representing gor file (gor/gorz)
 *
 * The internal data is stored in temp files.
 *
 */
public class GorTable<T extends Row> extends BaseTable<T> {

    private static final Logger log = LoggerFactory.getLogger(GorTable.class);

    protected Path tempOutFilePath;

    public GorTable(Builder builder) {
        super(builder);
        init();
    }

    public GorTable(URI uri, FileReader inputFileReader) {
        super(uri, inputFileReader);
        init();
    }

    public GorTable(URI uri) {
        this(uri, null);
    }

    private void init() {
        this.header = new TableHeader();
        if (fileReader.exists(getPath().toString())) {
            validateFile(getPath().toString());
        }
        reload();
    }

    @Override
    public Iterator<String> getLines() {
        try {
            return fileReader.getReader(getMainFile().toString()).lines().iterator();
        } catch (IOException e) {
            throw new GorSystemException("Error getting file reader", e);
        }
    }

    @Override
    public void insert(Collection<T> lines) {
        Path tempInputFile;
        try {
            tempInputFile = createInputTempFile(lines);
        } catch (IOException e) {
            throw new GorSystemException("Could not create temp file for inserting data", e);
        }
        insert(tempInputFile.toUri());

        logAfter(TableLog.LogAction.INSERT, "", lines.stream().map(l -> l.otherCols()).toArray(String[]::new));
    }

    @Override
    public void insert(String... lines) {
        List<T> entries = lineStringsToEntries(lines);
        insert(entries);
    }

    private void insert(URI gorFile) {
        // Validate the new file.
        if (isValidateFiles()) {
            validateFile(gorFile.toString());
        }
        String gorPipeCommand = createInsertTempFileCommand(gorFile);
        runMergeCommand(gorPipeCommand);
        // Use folder for the transaction.  Then queries can be run on the new file, within the transl
    }

    @Override
    public void delete(Collection<T> lines) {
        createDeleteTempFile(lines.stream().map(l -> l.getAllCols()).toArray(String[]::new));
    }

    @Override
    public void delete(String... lines) {
        createDeleteTempFile(lines);
    }

    protected void createDeleteTempFile(String... lines) {
        String randomString = RandomStringUtils.random(8, true, true);
        Path localTempPath = getTransactionFolderPath().resolve(
                String.format("result_temp_%s.%s", randomString, FilenameUtils.getExtension(getPath().toString())));

        String[] strippedLines = Arrays.stream(lines).map(line -> line.endsWith("\n") ? line.substring(0, line.length() - 1) : line).toArray(String[]::new);
        try (OutputStream os = fileReader.getOutputStream(localTempPath.toString())) {
            Iterator<String> it = getLines();
            while (it.hasNext()) {
                String orgLine = it.next();
                if (includeLine(orgLine, strippedLines)) {
                    os.write(orgLine.getBytes(StandardCharsets.UTF_8));
                    os.write('\n');
                }
            }
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        tempOutFilePath = localTempPath;
    }

    private boolean includeLine(String line, String[] skipLines) {
        for (String skipLine : skipLines) {
            if (line.equals(skipLine)) {
                return false;
            }
        }
        return true;
    }

    protected List<T> lineStringsToEntries(String[] lines) {
        List<T> entries = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            entries.add(createRow(line));
        }
        return entries;
    }

    protected T createRow(String line) {
        return (T)new RowBase(line.endsWith("\n") ? line.substring(0, line.length() - 1) : line);
    }

    protected String getInputTempFileEnding() {
        return ".gor";
    }

    protected String getGorCommand() {
        return "gor";
    }

    @Override
    public void saveTempMainFile() {
        // Move our temp file to the standard temp file and clean up.
        // or if these are links update the link file to point to the new temp file.
        // Clean up (remove old files and temp files)  s
        log.debug(String.format("Saving temp file (%s)to temp main file (%s) ",  tempOutFilePath, getTempMainFileName()));
        try {
            if (tempOutFilePath != null && getFileReader().exists(tempOutFilePath.toString())) {
                updateFromTempFile(tempOutFilePath.toString(), getTempMainFileName());
                tempOutFilePath = null;
                getFileReader().deleteDirectory(getTransactionFolderPath().toString());
            } else if (!getFileReader().exists(getPath().toString())) {
                writeToFile(Path.of(getTempMainFileName()), new ArrayList<>());
            }
        } catch (IOException e) {
            throw new GorSystemException("Could not save table", e);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    protected Path getTransactionFolderPath() {
        return Path.of(getFolderPath().toString(), "transactions");
    }

    protected Path createInputTempFile(Collection<T> lines) throws IOException {
        String randomString = RandomStringUtils.random(8, true, true);
        Path tempFilePath = getTransactionFolderPath().resolve("insert_temp_" + randomString + getInputTempFileEnding());

        fileReader.createDirectories(tempFilePath.getParent().toString());
        writeToFile(tempFilePath, lines);

        return tempFilePath;
    }

    protected void writeToFile(Path filePath, Collection<T> lines) throws IOException {
        try (OutputStream os = fileReader.getOutputStream(filePath.toString())) {
            os.write('#');
            os.write(Stream.of(getColumns()).collect(Collectors.joining("\t")).getBytes(StandardCharsets.UTF_8));
            os.write('\n');
            for (Row r : lines) {
                writeRowToStream(r, os);
                os.write('\n');
            }
        }
    }

    protected void writeRowToStream(Row r, OutputStream os) throws IOException {
        r.writeRowToStream(os);
    }

    protected String createInsertTempFileCommand(URI insertFile) {
        Path mainFile = getMainFile();
        tempOutFilePath = getNewTempFileName();

        return String.format("%s %s | merge %s | write %s", getGorCommand(), mainFile, insertFile.toString(), tempOutFilePath);
    }

    protected Path getNewTempFileName() {
        String randomString = RandomStringUtils.random(8, true, true);
        return getTransactionFolderPath().resolve(
                String.format("result_temp_%s.%s", randomString, FilenameUtils.getExtension(getPath().toString())));
    }

    protected Path getMainFile() {
        Path mainFile;
        if (tempOutFilePath == null) {
            mainFile = getPath();
        } else {
            mainFile = tempOutFilePath;
        }
        return mainFile;
    }

    protected void runMergeCommand(String gorPipeCommand) {
        String[] args = new String[]{
                gorPipeCommand,
                "-workers", "1"};
        PipeOptions options = new PipeOptions();
        options.parseOptions(args);
        CLIGorExecutionEngine engine = new CLIGorExecutionEngine(options, null, getSecurityContext());
        engine.execute();
    }
}
