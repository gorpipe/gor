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

package gorsat.external.plink;

import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * This analysis class collects gor or vcf lines and writes them in batches to a temporary vcf files read by plink2 process
 */
public class PlinkAdjustAdaptor extends gorsat.Commands.Analysis {
    private static final String DEFAULT_ADJUST_COLUMNS = "chrom,pos,ref,alt,unadj,gc,qq,bonf,holm,sidakss,sidaksd,fdrbh,fdrby";
    private static final String DEFAULT_PHENO_COLUMN = "PHENO";
    private static final String DEFAULT_TEST_COLUMN = "TEST";
    private static final String DEFAULT_TEST_ENTRY = "ADD";
    private static final String DEFAULT_PHENO_ENTRY = "pheno";

    private ExecutorService es = Executors.newSingleThreadExecutor();//.newFixedThreadPool(4);
    private Map<String, FilenameAndWriter> splitFileMap = new HashMap<>();
    private List<Future<Stream<Row>>> futList = new ArrayList<>();
    private int testIndex;
    private int phenoIndex;
    private String header;
    private String resultHeader;
    private String adjustColumns;
    private boolean sort;
    private String[] plinkExecutable;

    class FilenameAndWriter {
        Path filepath;
        String pheno;
        String test;
        Writer writer;

        public FilenameAndWriter(Path filepath, String pheno, String test, Writer writer) {
            this.filepath = filepath;
            this.pheno = pheno;
            this.test = test;
            this.writer = writer;
        }
    }

    public PlinkAdjustAdaptor(String header, boolean sort) throws IOException {
        this(header, DEFAULT_ADJUST_COLUMNS, sort);
    }

    public PlinkAdjustAdaptor(String header, String adjustColumns, boolean sort) throws IOException {
        String[] hSplit = header.toUpperCase().split("\t");
        List<String> hList = Arrays.asList(hSplit);
        testIndex = hList.indexOf(DEFAULT_TEST_COLUMN);
        phenoIndex = hList.indexOf(DEFAULT_PHENO_COLUMN);
        ArrayList<String> headerList = new ArrayList<>(Arrays.asList(adjustColumns.toUpperCase().split(",")));
        headerList.add(2,"ID");
        headerList.add(DEFAULT_TEST_COLUMN);
        headerList.add(DEFAULT_PHENO_COLUMN);
        resultHeader = String.join("\t",headerList);
        this.adjustColumns = adjustColumns;
        this.header = header;
        this.sort = sort;
        String pExec = System.getProperty("org.gorpipe.gor.driver.plink.executable");
        if( pExec == null ) {
            GorDriverConfig cfg = ConfigManager.createPrefixConfig("gor", GorDriverConfig.class);
            plinkExecutable = cfg.plinkExecutable().split(" ");
        } else plinkExecutable = pExec.split(" ");
    }

    @Override
    public void setup() {
        // Unused
    }

    @SuppressWarnings("squid:S2095") // Unable to close without a major refactoring
    @Override
    public void process(Row row) {
        String test = testIndex != -1 ? row.colAsString(testIndex).toString() : DEFAULT_TEST_ENTRY;
        String pheno = phenoIndex != -1 ? row.colAsString(phenoIndex).toString() : DEFAULT_PHENO_ENTRY;
        String filename = pheno+"-"+test;
        try {
            Writer writer;
            if( !splitFileMap.containsKey(filename) ) {
                Path newSplitFile = Files.createTempFile(filename+"_", ".txt");
                newSplitFile.toFile().deleteOnExit();
                writer = Files.newBufferedWriter(newSplitFile);
                writer.write(header);
                writer.write('\n');
                splitFileMap.put(filename,new FilenameAndWriter(newSplitFile,pheno,test,writer));
            } else writer = splitFileMap.get(filename).writer;
            row.writeRow(writer);
            writer.write('\n');
        } catch(IOException e) {
            throw new GorSystemException("Error in writing temporary file for PlinkAdjustment", e);
        }
    }

    @Override
    public String getHeader() {
        return resultHeader;
    }

    @Override
    public void finish() {
        try {
            splitFileMap.forEach((key, fw) -> {
                try {
                    Path filepath = fw.filepath;
                    Writer w = fw.writer;
                    w.close();
                    String pheno = fw.pheno;
                    String test = fw.test;
                    PlinkAdjustment plinkAdjustmentCall = new PlinkAdjustment(plinkExecutable, filepath, pheno, test, adjustColumns, sort);
                    futList.add(es.<Stream<Row>>submit(plinkAdjustmentCall));
                } catch (IOException e) {
                    throw new GorSystemException("Error closing plink adjust temp file", e);
                }
            });
            splitFileMap.clear();

            PriorityQueue<AdjustedGORLineIterator> pq = new PriorityQueue<>();
            if( sort ) {
                futList.forEach(future -> {
                    try {
                        Stream<Row> file = future.get();
                        AdjustedGORLineIterator gorline = new AdjustedGORLineIterator(file);
                        if (gorline.getRow() != null) pq.add(gorline);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new GorSystemException("Error joining results from plink adjust", e);
                    }
                });

                while (pq.size() > 0) {
                    AdjustedGORLineIterator gl = pq.poll();
                    Row row = gl.getRow();
                    super.process(row);
                    gl.next();
                    if (gl.getRow() != null) pq.add(gl);
                }
            } else {
                futList.forEach(future -> {
                    try {
                        Stream<Row> file = future.get();
                        file.forEach(super::process);
                    } catch (ExecutionException | InterruptedException e) {
                        throw new GorSystemException("Error reading from plink adjust result file", e);
                    }
                });
            }
        } catch(Exception e) {
            throw new GorSystemException("Exception when reading plink adjustment results", e);
        } finally {
            es.shutdown();
            futList.forEach(f -> {
                try {
                    f.get().close();
                } catch (InterruptedException | ExecutionException e) {
                    // Ignore
                }
            });
            splitFileMap.forEach((key, fw) -> {
                try {
                    fw.writer.close();
                } catch (IOException ie) {
                    // Ignore
                }
            });
        }
    }
}