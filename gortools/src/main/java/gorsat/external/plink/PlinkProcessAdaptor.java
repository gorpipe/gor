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

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.model.genome.files.gor.pgen.PGenWriter;
import org.gorpipe.model.genome.files.gor.pgen.PGenWriterFactory;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.GorSession;
import org.gorpipe.model.gor.RowObj;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * This analysis class collects gor or vcf lines and writes them in batches to a temporary vcf files read by plink2 process
 */
public class PlinkProcessAdaptor extends gorsat.Commands.Analysis {
    private final static Logger log = LoggerFactory.getLogger(PlinkProcessAdaptor.class);

    final static String PGEN_ENDING = ".pgen";
    final static String PVAR_ENDING = ".pvar";
    final static String PSAM_ENDING = ".psam";
    final static int MAXIMUM_NUMBER_OF_LINES = 100;

    private final String[] pgenFiles;
    private PGenWriter writer;
    final String psamFile;
    int pfnIdx = 0;
    GorSession session;

    int linesWrittenToCurrentFile = 0;
    String[] plinkExecutable;
    boolean first;
    ExecutorService es;
    Future<Boolean> plinkFuture;
    final Path writeDir;
    final PlinkArguments args;

    private float threshold;
    private final String phenoFile;
    private final int refIdx, altIdx, rsIdIdx, valueIdx;
    private final boolean hardCalls;
    private String lastChr = "";
    private int lastPos = -1;

    private String expectedHeader;
    private boolean checkedHeaderFromPlink = false;

    public PlinkProcessAdaptor(GorSession session, PlinkArguments plinkArguments,
                               int refIdx, int altIdx, int rsIdx, int valueIdx, boolean hc, float th, boolean vcf, String header) throws IOException {
        GorDriverConfig cfg = ConfigManager.createPrefixConfig("gor", GorDriverConfig.class);
        plinkExecutable = cfg.plinkExecutable().split(" ");
        this.session = session;
        this.expectedHeader = header;
        this.es = Executors.newSingleThreadExecutor();
        try {
            this.writeDir = Files.createTempDirectory("plinkregression");
            this.writeDir.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new GorSystemException("Could not create temp directory.", e);
        }
        this.pgenFiles = new String[]{this.writeDir.resolve(UUID.randomUUID().toString()).toString(), this.writeDir.resolve(UUID.randomUUID().toString()).toString()};
        this.psamFile = this.writeDir.resolve(UUID.randomUUID().toString() + PSAM_ENDING).toString();
        this.phenoFile = plinkArguments.pheno;
        this.refIdx = refIdx;
        this.altIdx = altIdx;
        this.rsIdIdx = rsIdx;
        this.valueIdx = valueIdx;
        this.hardCalls = hc;
        this.threshold = th;
        this.args = plinkArguments;
    }

    void nextGorLine(PriorityQueue<GORLine> pq, GORLine gorline) {
        try {
            gorline = gorline.next();
        } catch (IOException e) {
            try {
                gorline.close();
            } catch (IOException e1) {
                // Ignore exception in close
            }
            throw new GorSystemException("unable to read from process", e);
        }
        if (gorline != null) pq.add(gorline);
    }

    void sendLine(PriorityQueue<GORLine> pq) {
        while (pq.size() > 0) {
            GORLine gorline = pq.poll();
            if(!checkedHeaderFromPlink) {
                String header = gorline.getHeader();
                if(header!=null&&header.length()>0) {
                    if(expectedHeader.split("\t").length-1!=header.split("\t").length) {
                        throw new GorDataException("Unexpected number of columns in plink2 result");
                    }
                    checkedHeaderFromPlink = true;
                }
            }
            super.process(RowObj.apply(gorline.toString()));
            nextGorLine(pq, gorline);
        }
    }

    void prepareAndRunPlink(String pgenFilePath) throws ExecutionException, InterruptedException {
        try {
            writer.close();
        } catch (Exception e) {
            throw new GorSystemException(e);
        }
        if (plinkFuture != null) first = plinkFuture.get();
        PlinkThread plinkThread = new PlinkThread(session.getProjectContext().getRealProjectRootPath().toFile(), this.writeDir,
                this.plinkExecutable, pgenFilePath, this.psamFile, this.first,this, this.args, false);
        plinkFuture = es.submit(plinkThread);
    }

    void processRow(Row row) throws IOException, ExecutionException, InterruptedException {
        if (linesWrittenToCurrentFile > MAXIMUM_NUMBER_OF_LINES && (!lastChr.equals(row.chr) || lastPos != row.pos)) {
            prepareAndRunPlink(getCurrentPGenFile());
            setNewPGenStream();
            linesWrittenToCurrentFile = 0;
        }
        this.writer.write(row);
        lastChr = row.chr;
        lastPos = row.pos;
        linesWrittenToCurrentFile++;
    }

    @Override
    public void setup() {
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(this.psamFile));
             final BufferedReader br = new BufferedReader(this.session.getProjectContext().getFileReader().getReader(this.phenoFile))) {
            final String phenoHeader = br.readLine();
            final String phenoHeaderCutOff = phenoHeader.substring(phenoHeader.indexOf('\t') + 1);
            bw.write("#IID\tSID\tPAT\tMAT\tSEX\t" + phenoHeaderCutOff + "\n");
            String currLine;
            while ((currLine = br.readLine()) != null) {
                final int tabIdx = currLine.indexOf('\t');
                final String pn = currLine.substring(0, tabIdx);
                final String phenos = currLine.substring(tabIdx + 1);
                bw.write(pn + "\t" + pn + "\t0\t0\tNA\t" + phenos + "\n");
            }
            setNewTmpFileStream();
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    @Override
    public void process(Row row) {
        try {
            processRow(row);
        } catch (IOException | ExecutionException e) {
            isInErrorState_$eq(true);
            throw new GorSystemException("Error when running plink2", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isInErrorState_$eq(true);
            throw new GorSystemException("plink2 interrupted", e);
        }
    }

    void setNewTmpFileStream() throws IOException {
        setNewPGenStream();
    }

    private void setNewPGenStream() throws IOException {
        this.pfnIdx = (this.pfnIdx + 1) & 1;
        this.writer = PGenWriterFactory.getPGenWriter(getCurrentPGenFile() + PGEN_ENDING, this.refIdx, this.altIdx, this.rsIdIdx, this.valueIdx, this.hardCalls, !this.hardCalls, this.threshold);
    }

    String getCurrentPGenFile() {
        return this.pgenFiles[this.pfnIdx];
    }

    @Override
    public void finish() {
        try {
            prepareAndRunPlink(this.getCurrentPGenFile());
            if (plinkFuture != null) plinkFuture.get();
        } catch (ExecutionException e) {
            isInErrorState_$eq(true);
            throw new GorSystemException("Error when running plink2", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isInErrorState_$eq(true);
            throw new GorSystemException("Error plink2 interrupted", e);
        } finally {
            es.shutdown();
            try {
                FileUtils.deleteDirectory(this.writeDir.toFile());
            } catch (IOException e) {
                log.warn("Could not delete working directory {}", this.writeDir);
            }
        }
    }
}