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

import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.DataType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static gorsat.external.plink.PlinkProcessAdaptor.PGEN_ENDING;
import static gorsat.external.plink.PlinkProcessAdaptor.PVAR_ENDING;

class PlinkThread implements Callable<Boolean> {
    private final List<String> plinkArgList = new ArrayList<>();
    private final File projectRoot;
    private final Path tmpPath;
    private final boolean first;
    private final PlinkProcessAdaptor ppa;
    private final String pgenPath;
    private final Instant processStart;
    private final PlinkArguments plinkArguments;

    PlinkThread(File projectRoot, Path writeDir, String[] plinkExecutable, String pgenPath, String sampleFile, boolean first, PlinkProcessAdaptor ppa, PlinkArguments args, boolean vcf) {
        processStart = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.plinkArguments = args;
        this.tmpPath = writeDir;
        this.projectRoot = projectRoot;
        this.first = first;
        this.ppa = ppa;
        plinkArgList.addAll(Arrays.asList(plinkExecutable));
        plinkArgList.add("--threads");
        plinkArgList.add("2");
        plinkArgList.add("--memory");
        plinkArgList.add("1600");
        if( vcf ) {
            plinkArgList.add("--vcf");
            plinkArgList.add(pgenPath + DataType.VCF.suffix);
            plinkArgList.add("--pheno");
            plinkArgList.add(args.pheno);
        } else {
            plinkArgList.add("--pgen");
            plinkArgList.add(pgenPath + PGEN_ENDING);
            plinkArgList.add("--pvar");
            plinkArgList.add(pgenPath + PVAR_ENDING);
            plinkArgList.add("--psam");
            plinkArgList.add(sampleFile);
        }
        if (args.controlZero) {
            plinkArgList.add("--1");
        }
        if (args.mafThreshold != -1) {
            plinkArgList.add("--maf");
            plinkArgList.add(String.valueOf(args.mafThreshold));
        }
        if (args.hweThreshold != -1) {
            plinkArgList.add("--hwe");
            plinkArgList.add(String.valueOf(args.hweThreshold));
        }
        if (args.genoThreshold != -1) {
            plinkArgList.add("--geno");
            plinkArgList.add(String.valueOf(args.genoThreshold));
        }
        plinkArgList.add("--glm");
        if (args.residualize!=null&&args.residualize.length()>0) {
            if(args.residualize.equalsIgnoreCase("cc")) {
                plinkArgList.add("cc-residualize");
            } else if(args.residualize.equalsIgnoreCase("firth")) {
                plinkArgList.add("firth-residualize");
            } else {
                plinkArgList.add("intercept");
            }
        }
        if (args.firth) {
            plinkArgList.add("firth-fallback");
        }
        if (args.hideCovar) {
            plinkArgList.add("hide-covar");
        }
        if (args.dom) {
            plinkArgList.add("dominant");
        }
        if (args.rec) {
            plinkArgList.add("recessive");
        }
        if (args.covar != null) {
            plinkArgList.add("--covar");
            plinkArgList.add(args.covar);
            if (args.cvs) plinkArgList.add("--covar-variance-standardize");
        } else {
            plinkArgList.add("allow-no-covars");
        }
        if (args.vs) {
            plinkArgList.add("--variance-standardize");
        }
        if (args.qn) {
            plinkArgList.add("--quantile-normalize");
        }
        plinkArgList.add("--out");
        plinkArgList.add(pgenPath);
        this.pgenPath = pgenPath;
    }

    private Thread sendProcessErrorStreamToStdErr(InputStream es, StringWriter stringWriter) {
        Thread t = new Thread(() -> {
            try {
                int r = es.read();
                while (r != -1) {
                    stringWriter.append((char) r);
                    r = es.read();
                }
                es.close();
            } catch (IOException e) {
                PrintWriter pw = new PrintWriter(stringWriter);
                e.printStackTrace(pw);
            }
        });
        t.start();
        return t;
    }

    private void cleanupCoreDump() {
        try {
            Files.list(projectRoot.toPath()).filter(path -> path.getFileName().toString().startsWith("core.")).filter(path -> {
                try {
                    BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                    int cmp = attr.creationTime().toInstant().compareTo(processStart);
                    return cmp >= 0;
                } catch (IOException ie) {
                    // core dump cleanup exception ignored
                }
                return false;
            }).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ie) {
                    // core dump cleanup exception ignored
                }
            });
        } catch (IOException ie) {
            // core dump cleanup exception ignored
        }
    }

    @Override
    public Boolean call() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(plinkArgList);
        pb.directory(projectRoot);
        Process p = pb.start();
        List<String> reslist = new ArrayList<>();
        StringWriter processError = new StringWriter();
        try (InputStream is = p.getInputStream()) {
            Thread esThread = sendProcessErrorStreamToStdErr(p.getErrorStream(), processError);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String res = br.lines().dropWhile(l -> !l.contains("Results written to")).takeWhile(l -> !l.contains("End time")).collect(Collectors.joining(" "));
            String[] resplit = res.split("Results written to");
            for(String r : resplit) {
                if(r.length() > 0) {
                    String[] split = CommandParseUtilities.quoteAngleBracketsSafeSplit(r.trim(), ' ');
                    String resultFileTemplate = split[0].trim();
                    if (resultFileTemplate.contains("<phenotype name>")) {
                        reslist.addAll(Arrays.stream(plinkArguments.phenonames).map(pn -> resultFileTemplate.replace("<phenotype name>", pn)).collect(Collectors.toList()));
                    } else {
                        reslist.add(resultFileTemplate);
                    }
                }
            }
            esThread.join(20000);
        }
        String[] reslines = reslist.toArray(String[]::new);
        boolean exited = p.waitFor(60, TimeUnit.SECONDS);
        if (exited && p.exitValue() != 0) {
            cleanupCoreDump();
            String errorString = processError.getBuffer().toString();
            if (!errorString.contains("No variants"))
                throw new GorResourceException(errorString, "plink2 exited with value " + p.exitValue());
        }
        return writeResult(reslines, this.pgenPath);
    }

    private boolean writeResult(String[] reslines, String pgenPath) throws IOException {
        final File pgenFile = new File(pgenPath + PGEN_ENDING);
        final File pvarFile = new File(pgenPath + PVAR_ENDING);
        pgenFile.delete();
        pvarFile.delete();
        if (reslines.length > 0) {
            PriorityQueue<GORLine> pql = new PriorityQueue<>();
            for (String result : reslines) {
                Path p = Paths.get(result);
                String filename = p.getFileName().toString();
                int u = filename.indexOf('.');
                String phenotype = filename.substring(u + 1, filename.indexOf('.', u + 1));
                GORLine gl = new GORLine(phenotype, p);
                pql.add(gl);
            }
            for (String result : reslines) {
                Path path = Paths.get(result);
                if (Files.exists(path))
                    Files.delete(path);
                path = Paths.get(result + ".id");
                if (Files.exists(path))
                    Files.delete(path);
            }
            ppa.sendLine(pql);
        }
        Path logpath = tmpPath.resolve(this.pgenPath + ".log");
        if (Files.exists(logpath)) Files.delete(logpath);
        return first;
    }
}
