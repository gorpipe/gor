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

import org.gorpipe.model.gor.Pipes;
import gorsat.Commands.CommandParseUtilities;
import gorsat.Commands.GenomicRange;
import gorsat.DynIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.GorSession;
import org.gorpipe.model.genome.files.gor.*;
import org.gorpipe.model.gor.RowObj;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by sigmar on 12/02/16.
 */
public class ProcessRowSource extends ProcessSource {
    private StringBuilder errorStr = new StringBuilder();
    List<String> commands;
    private GenomicIterator it;
    boolean nor;
    private ProcessBuilder pb;
    private Process p;
    private Path fileroot = null;
    private String filter;

    public ProcessRowSource(String cmd, String type, boolean nor, GorSession session, GenomicRange.Range range, String filter) {
        this(CommandParseUtilities.quoteSafeSplit(cmd, ' '), type, nor, session, range, filter, Pipes.rowsToProcessBuffer());
    }

    public ProcessRowSource(String cmd, String type, boolean nor, GorSession session, GenomicRange.Range range, String filter, int bs) {
        this(CommandParseUtilities.quoteSafeSplit(cmd, ' '), type, nor, session, range, filter, bs);
    }

    public static String checkNested(String cmd, GorSession session, StringBuilder errorStr) {
        String ncmd;
        if( cmd.startsWith("<(") ) {
            String tmpdir = System.getProperty("java.io.tmpdir");
            if( tmpdir == null || tmpdir.length() == 0 ) tmpdir = "/tmp";
            Path tmpath = Paths.get(tmpdir);
            String scmd = cmd.substring(2,cmd.length()-1);
            Path fifopath = tmpath.resolve( Integer.toString(Math.abs(scmd.hashCode())) );
            String pipename = fifopath.toAbsolutePath().toString();
            try {
                if( !Files.exists(fifopath) ) {
                    ProcessBuilder mkfifo = new ProcessBuilder("mkfifo", pipename);
                    Process p = mkfifo.start();
                    p.waitFor();
                }
                Thread t = new Thread(() -> {
                    try (OutputStream os = Files.newOutputStream(fifopath);
                         DynIterator.DynamicRowSource drs = new DynIterator.DynamicRowSource(scmd, session.getGorContext(), false)) {
                        os.write( drs.getHeader().getBytes() );
                        os.write( '\n' );
                        while( drs.hasNext() ) {
                            String rowstr = drs.next().toString();
                            os.write( rowstr.getBytes() );
                            os.write('\n');
                        }
                    } catch (IOException e) {
                        errorStr.append(e.getMessage());
                    } finally {
                        try {
                            Files.delete( fifopath );
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                });
                t.start();
            } catch (InterruptedException | IOException e) {
                throw new GorSystemException("Failed starting fifo thread",e);
            }
            ncmd = pipename;
        } else {
            boolean quotas = cmd.startsWith("'") || cmd.startsWith("\"");
            ncmd = quotas ? cmd.substring(1, cmd.length() - 1) : cmd;
            if (quotas) ncmd = ncmd.replace("\\t", "\t").replace("\\n", "\n");
        }
        return ncmd;
    }

    private ProcessRowSource(String[] cmds, String type, boolean nor, GorSession session, GenomicRange.Range range, String fltr, int bs) {
        this.nor = nor;
        this.setBufferSize(bs);
        this.filter = fltr;
        commands = new ArrayList<>();

        if (session != null) {
            String root = session.getProjectContext().getRoot();
            if (root != null && root.length() > 0) {
                int i = root.indexOf(' ');
                if (i == -1) i = root.length();
                fileroot = Paths.get(root.substring(0, i));
            }
        }

        for (String cmd : cmds) {
            String ncmd = checkNested(cmd, session, errorStr);
            commands.add( ncmd );
        }

        boolean bamvcf = type != null && (type.equals("bam") || type.equals("sam") || type.equals("cram") || type.equals("vcf"));
        List<String> headercommands = bamvcf ? seekCmd(commands, it, null, 0, -1, null) : seekCmd(commands, it, range.chromosome(), range.start(), range.stop(), filter);

        try {
            List<String> rcmd = headercommands.stream().filter(p -> p.length() > 0).collect(Collectors.toList());
            pb = new ProcessBuilder(rcmd);
            if (fileroot != null) pb.directory(fileroot.toFile());
            p = pb.start();
            Thread errorThread = new Thread(() -> {
                try {
                    InputStream es = p.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(es));
                    String line = br.readLine();
                    while (line != null) {
                        errorStr.append(line).append("\n");
                        line = br.readLine();
                    }
                    br.close();
                } catch (IOException e) {
                    // don't care throw new RuntimeException("", e);
                }
            });
            errorThread.start();
            InputStream is = p.getInputStream();

            if (type == null || type.equalsIgnoreCase("gor")) {
                it = gorIterator( is, headercommands, type );
            } else if (type.equalsIgnoreCase("vcf")) {
                it = vcfIterator( is );
                if( range.chromosome() != null ) it.seek(range.chromosome(), range.start(), range.stop());
            } else if (type.equalsIgnoreCase("bam") || type.equalsIgnoreCase("sam") || type.equalsIgnoreCase("cram")) {
                it = bamIterator( is );
            }
            if( range.chromosome() != null ) it.seek(range.chromosome(), range.start(), range.stop());
            String header = it.getHeader();
            String[] headerArray = header.split("\t");
            it.setColnum(headerArray.length-2);
            setHeader(String.join("\t", header));
        } catch (IOException e) {
            throw new GorResourceException("unable to get header from process " + commands.get(0), "", e);
        }
    }

    private GenomicIterator gorIterator( InputStream is, List<String> headercommands, String type ) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        final String header = br.readLine();
        setHeader(header);
        if (getHeader() == null) {
            throw new GorSystemException("Running external process: " + String.join(" ", headercommands) + " with error: " + errorStr, null);
        }
        if (nor) setHeader("ChromNOR\tPosNOR\t" + getHeader().replace(" ", "_").replace(":", "") );
        return new GenomicIterator() {
            BufferedReader reader = br;
            String next = readLine();

            private String readLine() throws IOException {
                String line = reader.readLine();
                if (line == null) return null;
                return nor ? "chrN\t0\t" + line : line;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Row next() {
                Row row = RowObj.StoR(next);
                try {
                    next = readLine();
                } catch (IOException e) {
                    throw new GorSystemException("Error reading next line from external process", e);
                }

                return row;
            }

            @Override
            public String getHeader() {
                return header;
            }

            @Override
            public boolean seek(String seekChr, int seekPos) {
                InputStream is = setRange(seekChr, seekPos, -1);
                reader = new BufferedReader(new InputStreamReader(is));
                try {
                    if (type != null) readLine();
                    next = readLine();
                } catch (IOException e) {
                    throw new GorSystemException("Error reading next line from external process after seek", e);
                }
                return true;
            }

            @Override
            public boolean next(Line line) {
                return false;
            }

            @Override
            public void close() {
                try {
                    reader.close();
                    p.destroy();
                } catch (IOException e) {
                    // don't care if external process stdout stream fails closing, could already have been closed by the process itself
                }
            }
        };
    }

    private GenomicIterator vcfIterator( InputStream is ) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        GenomicIterator.ChromoLookup lookup = createChromoLookup();
        GenomicIterator vcfit;
        try {
            vcfit = new VcfGzGenomicIterator(lookup, "filename", null, br) {
                @Override
                public boolean seek(String seekChr, int seekPos) {
                    return seek(seekChr, seekPos, lookup.chrToLen(seekChr));
                }

                @Override
                public boolean seek(String seekChr, int seekPos, int endPos) {
                    try {
                        reader.close();
                        if (seekChr != null && this.chrNameSystem != VcfGzTabixGenomicIterator.ChrNameSystem.WITH_CHR_PREFIX)
                            seekChr = seekChr.substring(3);
                        InputStream is1 = setRange(seekChr, seekPos, endPos == 0 ? 1 : endPos);
                        reader = new BufferedReader(new InputStreamReader(is1));
                        next = reader.readLine();
                        while (next != null && next.startsWith("##")) {
                            next = reader.readLine();
                        }
                        while (next != null && !next.startsWith("#")) {
                            next = reader.readLine();
                        }
                        while (next != null && next.startsWith("#")) {
                            next = reader.readLine();
                        }
                    } catch (IOException e) {
                                throw new GorSystemException("Error reading next line from external process providing vcf stream", e);
                    }
                    return true;
                }

                @Override
                public void close() {
                    super.close();
                }
            };
        } catch (Exception e) {
            p.destroy();
            int exitValue = 0;
            try {
                boolean didStop = p.waitFor(1, TimeUnit.SECONDS);
                if( !didStop ) {
                    p.destroyForcibly();
                    exitValue = p.waitFor();
                } else exitValue = p.exitValue();
            } catch (InterruptedException ie) {
                errorStr.append( ie.getMessage() );
            }
            throw new GorSystemException("Error initializing vcf reader. Exit value from process: " + exitValue + ". Error from process: " + errorStr, e);
        }
        return vcfit;

    }

    private GenomicIterator bamIterator( InputStream is ) {
        GenomicIterator.ChromoLookup lookup = createChromoLookup();
        SamReaderFactory srf = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
        SamInputResource sir = SamInputResource.of(is);
        SamReader samreader = srf.open(sir);
        BamIterator bamit = new BamIterator() {
            @Override
            public boolean seek(String chr, int pos) {
                return super.seek(chr, pos);
            }

            @Override
            public boolean seek(String chr, int pos, int end) {
                int chrId = lookup.chrToId(chr); // Mark that a single chromosome seek
                if (chrnamesystem == 1) { // BAM data on hg chromsome names, use the hg name for the chromsome for the seek
                    chr = ChromoCache.getHgName(chrId);
                } else if (chrnamesystem == 2) {
                    chr = ChromoCache.getStdChrName(chrId);
                }

                try {
                    this.reader.close();
                } catch (IOException e) {
                    // don't care if external process stream has already been closed
                }
                InputStream nis = setRange(chr, pos, end);
                SamInputResource sir = SamInputResource.of(nis);
                this.reader = srf.open(sir);
                this.pos = pos;

                return true;
            }

            @Override
            public boolean hasNext() {
                initIterator();
                boolean hasNext = it.hasNext();
                while (hasNext && (record = it.next()) != null && (record.getReadUnmappedFlag() || "*".equals(record.getCigarString()) || record.getStart() < pos)) {
                    hasNext = it.hasNext();
                }
                if (!hasNext) {
                    if (hgSeekIndex >= 0) { // Is seeking through differently ordered data
                        while (++hgSeekIndex < ChrDataScheme.ChrLexico.getOrder2id().length) {
                            String name = getChromName();
                            if (samFileHeader.getSequenceIndex(name) > -1) {
                                createIterator(name, 0);
                                return hasNext();
                            }
                        }
                    }
                }
                return hasNext;
            }

            @Override
            public void createIterator(String chr, int pos) {
                if( it == null ) it = reader.iterator();
            }
        };
        bamit.init(lookup, samreader, null, false);
        bamit.chrnamesystem = 0;
        return bamit;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public Row next() {
        return it.next();
    }

    private static void noSeekReplace(String cmd, List<String> seekcmd) {
        int hPos;
        int sPos;
        hPos = cmd.indexOf("#(H:");
        if (hPos != -1) {
            int hEnd = cmd.indexOf(')', hPos + 1);
            cmd = cmd.substring(0, hPos) + cmd.substring(hPos + 4, hEnd) + cmd.substring(hEnd + 1);
        }

        sPos = cmd.indexOf("#(S:");
        if (sPos != -1) {
            int sEnd = cmd.indexOf(')', sPos + 1);
            cmd = cmd.substring(0, sPos) + cmd.substring(sEnd + 1);
        }

        if( sPos != -1 || hPos != -1 ) {
            seekcmd.addAll(Arrays.asList(cmd.split("[ ]+")));
        } else seekcmd.add(cmd);
    }

    private static String posReplace(String seek, GenomicIterator it, String seekChr, int startPos, int endPos) {
        if( seekChr.startsWith("chr") ) seek = seek.replace("chn", seekChr.substring(3));
        else seek = seek.replace("chn", seekChr);
        int pos = seek.indexOf("pos-end");
        if (pos != -1) {
            if (endPos == -1) {
                int len = Integer.MAX_VALUE;
                if( it != null && it.getLookup() != null ) it.getLookup().chrToLen(seekChr);
                seek = seek.replace("pos", (startPos + 1) + "").replace("end", len + "");
            } else {
                seek = seek.replace("pos", (startPos + 1) + "").replace("end", endPos + "");
            }
        } else if (seek.contains("pos")) {
            pos = seek.indexOf("pos-");
            if (endPos == -1) {
                seek = seek.replace("pos", startPos + "");
            } else if (startPos == endPos && pos != -1) {
                seek = seek.replace("pos-", startPos + "");
            } else {
                seek = seek.replace("pos", startPos+"").replace("end", endPos+"");
            }
        }

        return seek;
    }

    private static void seekReplace(String cmd, List<String> seekcmd, GenomicIterator it, String seekChr, int startPos, int endPos) {
        int sPos;
        int hPos = cmd.indexOf("#(H:");
        if (hPos != -1) {
            int hEnd = cmd.indexOf(')', hPos + 1);
            cmd = cmd.substring(0, hPos) + cmd.substring(hEnd + 1);
        }

        sPos = cmd.indexOf("#(S:");
        if (sPos != -1) {
            int sEnd = cmd.indexOf(')', sPos + 1);
            String seek = cmd.substring(sPos + 4, sEnd).replace("chr", seekChr);
            seek = posReplace(seek, it, seekChr, startPos, endPos);
            cmd = cmd.substring(0, sPos) + seek + cmd.substring(sEnd + 1);
        }
        if( sPos != -1 || hPos != -1 ) {
            seekcmd.addAll(Arrays.asList(cmd.split("[ ]+")));
        } else seekcmd.add(cmd);
    }

    public static String filterCmd(String[] commands, String filter) {
        String[] ret = filterCmd(Arrays.asList(commands), filter).toArray(new String[0]);
        return String.join(" ", ret).trim();
    }

    private static List<String> filterCmd(List<String> commands, String filter) {
        List<String> seekcmd = new ArrayList<>();
        for (String cmd : commands) {
            if (filter == null) {
                int fPos = cmd.indexOf("#(F:");
                if (fPos != -1) {
                    int fEnd = CommandParseUtilities.quoteSafeIndexOf(cmd, ")", true, fPos+1);
                    if( fEnd == -1 ) fEnd = cmd.length();
                    cmd = cmd.substring(0, fPos) + cmd.substring(Math.min(cmd.length(),fEnd+1));
                }
            } else {
                int fPos = cmd.indexOf("#(F:");
                if (fPos != -1) {
                    int fEnd = CommandParseUtilities.quoteSafeIndexOf(cmd, ")", true, fPos+1);
                    if( fEnd == -1 ) fEnd = cmd.length()-1;
                    String filt = cmd.substring(fPos + 4, fEnd).replace("filter", filter);
                    cmd = cmd.substring(0, fPos) + filt + cmd.substring(fEnd + 1);
                }
            }
            if( cmd.length() > 0 ) seekcmd.add(cmd);
        }
        return seekcmd;
    }

    static List<String> seekCmd(List<String> commands, GenomicIterator it, String seekChr, int startPos, int endPos, String filter) {
        List<String> filtercmd = filterCmd(commands,filter);
        List<String> seekcmd = new ArrayList<>();
        for (String cmd : filtercmd) {
            if (seekChr == null) {
                noSeekReplace(cmd, seekcmd);
            } else {
                seekReplace(cmd, seekcmd, it, seekChr, startPos, endPos);
            }
        }
        return seekcmd;
    }

    @Override
    public InputStream setRange(String seekChr, int startPos, int endPos) {
        try {
            List<String> seekcmd = seekCmd(commands, it, seekChr, startPos, endPos, filter);
            if (it != null) it.close();

            if (p != null && p.isAlive()) {
                p.destroy();
            }
            List<String> cmdlist = seekcmd.stream().filter(p -> p.length() > 0).collect(Collectors.toList());
            pb = new ProcessBuilder(cmdlist);
            if (fileroot != null) pb.directory(fileroot.toFile());
            p = pb.start();

            Thread errorThread = new Thread(() -> {
                try {
                    InputStream es = p.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(es));
                    String line = br.readLine();
                    while (line != null) {
                        errorStr.append(line).append("\n");
                        line = br.readLine();
                    }
                    br.close();
                } catch (IOException e) {
                    // don't care throw new RuntimeException("Error reading stderr from external process", e);
                }
            });
            errorThread.start();

            return p.getInputStream();
        } catch (IOException e) {
            throw new GorSystemException("Unable to read line from external process in seek: " + commands, e);
        }
    }

    @Override
    public void setPosition(String seekChr, int seekPos) {
        it.seek(seekChr, seekPos);
    }

    @Override
    public void close() {
        if (it != null) it.close();
        if (p != null && p.isAlive()) {
            p.destroy();
        }
    }

    @Override
    public boolean isBuffered() {
        return true;
    }

    public static GenomicIterator.ChromoLookup createChromoLookup() {
        final ChromoCache lookupCache = new ChromoCache();
        final boolean addAnyChrToCache = true;
        final ChrDataScheme dataOutputScheme = ChrDataScheme.ChrLexico;
        GenomicIterator.ChromoLookup lookup = new GenomicIterator.ChromoLookup() {
            @Override
            public final String idToName(int id) {
                return lookupCache.toName(dataOutputScheme, id);
            }

            @Override
            public final int chrToId(String chr) {
                return lookupCache.toIdOrUnknown(chr, addAnyChrToCache);
            }

            @Override
            public final int chrToLen(String chr) {
                return lookupCache.toLen(chr);
            }

            @Override
            public final int chrToId(CharSequence str, int strlen) {
                return lookupCache.toIdOrUnknown(str, strlen, addAnyChrToCache);
            }

            @Override
            public final int prefixedChrToId(byte[] buf, int offset) {
                return lookupCache.prefixedChrToIdOrUnknown(buf, offset, addAnyChrToCache);
            }

            @Override
            public final int prefixedChrToId(byte[] buf, int offset, int buflen) {
                return lookupCache.prefixedChrToIdOrUnknown(buf, offset, buflen, addAnyChrToCache);
            }

            @Override
            public ChromoCache getChromCache() {
                return lookupCache;
            }
        };
        return lookup;
    }
}
