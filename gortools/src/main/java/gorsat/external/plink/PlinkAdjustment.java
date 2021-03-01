/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class PlinkAdjustment implements Callable<Stream<Row>> {
    private static final Logger log = LoggerFactory.getLogger(PlinkAdjustment.class);

    private final String pheno;
    private final String test;
    private final String columns;
    private final String filepath;
    private final boolean sort;
    private final String[] plinkExecutable;

    public PlinkAdjustment(String[] plinkExecutable, Path filepath, String pheno, String test, String columns, boolean sort) {
        this.pheno = pheno;
        this.test = test;
        this.columns = columns;
        this.filepath = filepath.toString();
        this.sort = sort;
        this.plinkExecutable = plinkExecutable;

    }

    @SuppressWarnings("squid:S2095") // Unable to close without a major refactoring
    public Stream<Row> call() throws Exception {
        String fileToBeAdjusted = filepath;
        String adjustedFile = Files.createTempFile("plinkadjust-","").toString();
        ArrayList<String> command = new ArrayList<>(Arrays.asList(plinkExecutable));
        String[] plinkArgs = {"--adjust-file", fileToBeAdjusted, "test="+test, "cols="+columns, "--out", adjustedFile};
        command.addAll(Arrays.asList(plinkArgs));
        ProcessBuilder plinkProcessBuilder = new ProcessBuilder(command);
        Process plinkProcess = plinkProcessBuilder.start();
        StringBuilder sb = new StringBuilder();
        StringBuilder esb = new StringBuilder();
        InputStream es = plinkProcess.getErrorStream();
        Thread eThread = new Thread() {
            public void run() {
                try {
                    int r = es.read();
                    while( r != -1 ) {
                        esb.append((char)r);
                        r = es.read();
                    }
                } catch(Exception ignored) {}
            }
        };
        eThread.start();
        InputStream is = plinkProcess.getInputStream();
        int r = is.read();
        while( r != -1 ) {
            sb.append((char)r);
            r = is.read();
        }
        eThread.join();

        log.debug(sb.toString());
        int exitValue = plinkProcess.waitFor();
        if( exitValue != 0 ) throw new GorSystemException("Error running plink2: "+esb.toString(), null);

        Path origPath = Paths.get(fileToBeAdjusted);
        if( Files.exists(origPath) ) Files.delete(origPath);

        Path res = Paths.get(adjustedFile+".adjusted");
        Path log = Paths.get(adjustedFile+".log");
        if( Files.exists(log) ) Files.delete(log);
        BufferedReader br = Files.newBufferedReader(res);

        Stream<AdjustedGORLine> lines = br.lines().skip(1).map(AdjustedGORLine::new);
        if( sort ) lines = lines.sorted();
        Stream<Row> sstr =lines.map(s -> s.toString()+"\t"+test+"\t"+pheno).map(RowObj::apply);
        sstr.onClose(() -> {
            try {
                if( Files.exists(res) ) Files.delete(res);
                br.close();
            } catch (IOException ignored) {}
        });
        return sstr;
    }
}