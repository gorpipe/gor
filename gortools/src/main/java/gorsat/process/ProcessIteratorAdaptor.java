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

package gorsat.process;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.GorContext;
import org.gorpipe.gor.GorSession;
import org.gorpipe.model.genome.files.gor.Row;
import gorsat.Commands.Analysis;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RowSource;
import gorsat.Commands.CommandParseUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sigmar on 03/11/15.
 */
public class ProcessIteratorAdaptor extends RowSource {
    private static final Logger log = LoggerFactory.getLogger(ProcessIteratorAdaptor.class);
    private static Pattern pattern = Pattern.compile("'(?:[^']|'')+'|[^ ]+");

    static String norprefix = "chrN\t0\t";
    static String norheaderprefix = "ChromNOR\tPosNOR\t";
    static int norprefixlength = norprefix.length();
    static int norheaderprefixlength = norheaderprefix.length();

    private boolean mustReCheck = true;
    private boolean myHasNext = true;
    private BufferedReader breader;

    private final Process proc;
    final InputStream is;

    private boolean skipheader;
    private boolean nor;

    private String processName = "";
    private StringBuilder errorStr = new StringBuilder();
    private boolean allowerror;
    private RowSource rowSource;
    private OutThread outThread;

    class ProcessAdaptor extends Analysis {
        OutputStream os;
        Analysis pps;

        ProcessAdaptor(OutputStream os) {
            this.os = os;
        }

        void setProcessPipeStep(Analysis pps) {
            this.pps = pps;
        }

        @Override
        public void process(Row row) {
            try {
                if (!wantsNoMore()) {
                    String rowstr = row.toString();
                    os.write(rowstr.getBytes());
                    os.write('\n');
                } else {
                    pps.wantsNoMore_$eq(true);
                }
            } catch (IOException e) {
                // ignore, external process already closed the output stream
            }
        }

        @Override
        public void finish() {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class NorProcessAdaptor extends ProcessAdaptor {
        NorProcessAdaptor(OutputStream os) {
            super(os);
        }

        @Override
        public void process(Row row) {
            try {
                if (!wantsNoMore()) {
                    String rowstr = row.toString();
                    rowstr = rowstr.substring(ProcessIteratorAdaptor.norprefixlength);
                    os.write(rowstr.getBytes());
                    os.write('\n');
                } else {
                    pps.wantsNoMore_$eq(true);
                }
            } catch (IOException e) {
                // ignore, external process already closed the output stream
            }
        }
    }

    private class OutThread extends Thread {
        private RowSource rs;
        private Analysis processPipeStep;
        private OutputStream os;
        private Throwable th = null;
        private String header;

        OutThread(RowSource rs, Analysis processPipeStep, OutputStream os, String header) {
            this.rs = rs;
            this.processPipeStep = processPipeStep;
            this.os = os;
            this.header = header;
        }

        boolean hasError() {
            return th != null;
        }

        public Throwable getException() {
            return th;
        }

        void writeOutput() {
            try {
                while (rs.hasNext() && !processPipeStep.wantsNoMore()) {
                    Row row = rs.next();
                    processPipeStep.process(row);
                }
            } finally {
                rs.close();
            }
        }

        public void run() {
            try {
                if (!skipheader) {
                    String hdr = nor ? "#" + header.substring(ProcessIteratorAdaptor.norheaderprefixlength) : header;
                    os.write(hdr.getBytes());
                    os.write('\n');
                    os.flush();
                }
                writeOutput();
            } catch (IOException ie) {
                if (!ie.getMessage().contains("Stream closed")) th = ie;
            } catch (Exception e) {
                th = e;
            } finally {
                processPipeStep.securedFinish(th);
            }

            if (!proc.isAlive() && proc.exitValue() != 0) {
                th = new GorSystemException("Non zero exit value from OutThread " + proc.exitValue(), th);
            }
        }
    }

    public ProcessIteratorAdaptor(GorContext context, String cmd, String alias, RowSource rs, Analysis an, String header, boolean skipheader, Optional<String> skip, boolean allowerror, boolean nor) throws IOException {
        this.skipheader = skipheader;
        this.nor = nor;
        this.allowerror = allowerror;
        this.rowSource = rs;
        setHeader(header);

        List<String> commands = new ArrayList<>();
        Path fileRoot = ProcessIteratorAdaptor.getFileRoot(context.getSession());
        List<String> splitcmd = ProcessIteratorAdaptor.commandSplit(fileRoot, cmd);

        for (String scmd : splitcmd) {
            String ncmd = ProcessRowSource.checkNested(scmd, context.getSession(), errorStr);
            commands.add(ncmd);
        }
        processName = alias + ": [" + String.join(" ", commands) + "]";

        ProcessBuilder pb = new ProcessBuilder(commands);
        if (fileRoot != null) pb.directory(fileRoot.toFile());
        proc = pb.start();

        final InputStream es = proc.getErrorStream();
        startReadStdErrThread(es);

        is = proc.getInputStream();
        breader = new BufferedReader(new InputStreamReader(is));

        final OutputStream os = proc.getOutputStream();
        ProcessAdaptor processAdaptor = nor ? new NorProcessAdaptor(os) : new ProcessAdaptor(os);
        Analysis processPipeStep;
        if (an != null) {
            an = PipeInstance.injectTypeInferral(an, false);
            processPipeStep = an.$bar(processAdaptor);
        } else {
            processPipeStep = processAdaptor;
        }
        processAdaptor.setProcessPipeStep(processPipeStep);
        processPipeStep.securedSetup(null);

        outThread = new OutThread(rs, processPipeStep, os, header);
        outThread.start();

        if (skip.isPresent()) {
            String skipstr = skip.get();
            try {
                int skipnum = Integer.parseInt(skipstr);
                for (int i = 0; i < skipnum; i++) {
                    breader.readLine();
                }
            } catch (NumberFormatException e) {
                String line = breader.readLine();
                while (line != null && line.startsWith(skipstr)) {
                    line = breader.readLine();
                }

                mustReCheck = false;
                myHasNext = line != null;
            }
        }

        if (!skipheader) {
            processHeader(nor, os);
        }
    }

    private void processHeader(boolean nor, OutputStream os) throws IOException {
        setHeader(line != null ? line : breader.readLine());
        // R in some cases outputs 'WARNING' as first line to stdout
        while (getHeader() != null && (getHeader().length() == 0 || getHeader().startsWith("WARNING"))) {
            setHeader(breader.readLine());
        }

        if (getHeader() == null) {
            String newline = breader.readLine();

            proc.destroy();
            Exception ie = null;
            try {
                breader.close();
                os.close();
                proc.waitFor();
            } catch (Exception e) {
                ie = e;
            }
            String error = "newline " + newline + "; " + errorStr + "; exitValue=" + proc.exitValue();
            if (ie != null) throw new GorSystemException(error, ie);
            else throw new GorSystemException(error, null);
        } else if (nor) {
            setHeader(ProcessIteratorAdaptor.norheaderprefix + getHeader());
        }
    }

    private void startReadStdErrThread(InputStream es) {
        Thread readStdErrThread = new Thread(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                int r = es.read();
                while (r != -1) {
                    baos.write(r);
                    r = es.read();
                }
                es.close();
                baos.close();
                String stderr = baos.toString();
                if (stderr.length() > 0) {
                    errorStr.append(stderr);
                    log.trace("stderr from external process " + processName + ": " + stderr);
                }
            } catch (IOException e) {
                throw new GorSystemException("Error reading stderr from external process", e);
            }
        });

        readStdErrThread.setDaemon(true);
        readStdErrThread.start();
    }

    private String line;
    @Override
    public boolean hasNext() {
        if (!mustReCheck) {
            return myHasNext;
        }
        try {
            line = breader.readLine();
            myHasNext = line != null;
        } catch (IOException e) {
            throw new GorSystemException("unable to read from process", e);
        }
        mustReCheck = false;
        if (outThread.hasError()) {
            throw new GorSystemException("Error in process out thread ", outThread.getException());
        }
        return myHasNext;
    }

    @Override
    public Row next() {
        if (hasNext()) {
            mustReCheck = true;
            Row myNext;
            try {
                if (nor) myNext = RowObj.StoR(ProcessIteratorAdaptor.norprefix + line);
                else myNext = RowObj.StoR(line);
            } catch (ArrayIndexOutOfBoundsException ae) {
                throw new GorDataException("external process " + processName + "returned illegal line.", -1, line, getHeader(), ae);
            }
            return myNext;
        } else {
            return null;
        }
    }

    @Override
    public void setPosition(String seekChr, int seekPos) {
        mustReCheck = true;
        rowSource.setPosition(seekChr, seekPos);
    }

    @Override
    public void close() {
        mustReCheck = true;

        int exitValue = 0;
        if (proc.isAlive()) {
            proc.destroy();
            // no need to waitFor or check exitValue
        } else {
            exitValue = proc.exitValue();
        }

        if (exitValue != 0) {
            String errMsg = errorStr == null || errorStr.length() == 0 ? getHeader() : errorStr.toString();
            if (allowerror) {
                log.trace("Allowed external process " + processName + " with non-zero exit code: " + errMsg);
            } else {
                throw new GorSystemException("External process " + processName + " exited with non-zero exit code (" + exitValue + "): " + errMsg, null);
            }
        }
    }

    @Override
    public boolean isBuffered() {
        return true;
    }

    private static Path getFileRoot(GorSession session) {
        Path fileRoot = null;
        if (session != null) {
            String root = session.getProjectContext().getRoot();
            if (root != null && root.length() > 0) {
                int i = root.indexOf(' ');
                if (i == -1) i = root.length();
                fileRoot = Paths.get(root.substring(0, i));
            }
        }
        return fileRoot;
    }

    static List<String> commandSplit(Path fileRoot, String cmd) {
        String[] cmds = CommandParseUtilities.quoteSafeSplit(cmd, ' ');

        List<String> split = new ArrayList<>();
        for( String scmd : cmds) {
            subCommandSplit(fileRoot, scmd, split);
        }
        return split;
    }

    public static void subCommandSplit(Path fileRoot, String subCommand, List<String> split) {
        if( subCommand.startsWith("<(") ) {
            split.add( subCommand );
        } else {
            Matcher matcher = pattern.matcher(subCommand);

            boolean found = matcher.find();
            while (found) {
                String match = matcher.group();
                if (match.startsWith("'")) split.add(match.substring(1, match.length() - 1));
                else {
                    boolean isFile = match.contains("/") || match.endsWith(".R") || match.endsWith(".py") || match.endsWith(".sh") || match.endsWith(".gor") || match.endsWith("gorz") || match.endsWith(".txt");
                    if (isFile) {
                        Path fmatch;
                        if (fileRoot != null && !match.startsWith("/")) {
                            fmatch = fileRoot.resolve(match);
                        } else fmatch = Paths.get(match);

                        if (split.size() == 0 && Files.exists(fmatch)) fmatch.toFile().setExecutable(true);
                    }
                    split.add(match);
                }
                found = matcher.find();
            }
        }
    }
}
