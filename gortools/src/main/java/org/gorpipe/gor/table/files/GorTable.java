package org.gorpipe.gor.table.files;

import gorsat.process.CLIGorExecutionEngine;
import gorsat.process.PipeOptions;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.table.BaseTable;
import org.gorpipe.gor.table.TableHeader;
import org.gorpipe.gor.table.TableLog;
import org.gorpipe.gor.table.dictionary.DictionaryTableMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Table class representing gor file (gor/gorz)
 */
public class GorTable<T extends Row> extends BaseTable<T> {

    private static final Logger log = LoggerFactory.getLogger(GorTable.class);

    Path tempOutFilePath;

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
    public void insert(Collection<T> lines) {
        Path tempInputFile;
        try {
            tempInputFile = createInputTempFile(lines);
        } catch (IOException e) {
            throw new GorSystemException("Could not create temp file for inserting data", e);
        }
        insert(tempInputFile.toUri());
    }

    public void insert(URI gorFile) {
        // Validate the new file.
        if (isValidateFiles()) {
            validateFile(gorFile.toString());
        }
        String gorPipeCommand = createInsertTempFileCommand(gorFile);
        runMergeCommand(gorPipeCommand);
        // Use folder for the transaction.  Then queries can be run on the new file, within the transl

        // TODO: enable
        //logAfter(TableLog.LogAction.INSERT, "", line);
    }

    @Override
    public void delete(Collection lines) {
        // How to perform efficient delete.
        // Sort the input lines.
        // Read through the file, and filter out unwanted lines, write out the lines.
        throw new GorSystemException("Not implemented", null);

        // TODO: enable
        //logAfter(TableLog.LogAction.DELETE, "", line);
    }

    @Override
    public void saveMainFile() {
        // Move main temp file into the main file.
        // or if these are links update the link file to point to the new temp file.
        // Clean up (remove old files and temp files)  s

        try {
            if (tempOutFilePath != null && Files.exists(tempOutFilePath)) {
                updateFromTempFile(getPathUri(), tempOutFilePath.toUri());
                Files.delete(getTransactionFolderPath());
            } else if (!Files.exists(getPath())) {
                writeToFile(getPath(), new ArrayList<>());
            }
        } catch (IOException e) {
            throw new GorSystemException("Could not save table", e);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    private Path getTransactionFolderPath() {
        return Path.of(getFolderPath().toString(), "transactions");
    }

    private Path createInputTempFile(Collection<T> lines) throws IOException {
        String randomString = RandomStringUtils.random(8, true, true);
        Path tempFilePath = getTransactionFolderPath().resolve("insert_temp_" + randomString + ".gor");

        fileReader.createDirectories(tempFilePath.getParent().toString());
        writeToFile(tempFilePath, lines);

        return tempFilePath;
    }

    private void writeToFile(Path filePath, Collection<T> lines) throws IOException {
        try (OutputStream os = fileReader.getOutputStream(filePath.toString())) {
            os.write('#');
            os.write(Stream.of(getColumns()).collect(Collectors.joining("\t")).getBytes(StandardCharsets.UTF_8));
            os.write('\n');
            for (Row r : lines) {
                os.write(r.getAllCols().toString().getBytes(StandardCharsets.UTF_8));
                os.write('\n');
            }
        }
    }

    private String createInsertTempFileCommand(URI insertFile) {
        Path mainFile;

        if (tempOutFilePath == null) {
            mainFile = getPath();
        } else {
            mainFile = tempOutFilePath;

        }
        String randomString = RandomStringUtils.random(8, true, true);
        tempOutFilePath = getTransactionFolderPath().resolve(
                String.format("result_temp_%s.%s", randomString, FilenameUtils.getExtension(getPath().toString())));

        String command = String.format("gor %s %s | write %s", mainFile, insertFile.toString(), tempOutFilePath);
        return command;
    }

    private void runMergeCommand(String gorPipeCommand) {
        String[] args = new String[]{
                gorPipeCommand,
                "-workers", "1"};
        log.trace("Calling bucketize with command args: \"{}\" {} {}", args);

        PipeOptions options = new PipeOptions();
        options.parseOptions(args);
        CLIGorExecutionEngine engine = new CLIGorExecutionEngine(options, null, getSecurityContext());
        engine.execute();
    }
}
