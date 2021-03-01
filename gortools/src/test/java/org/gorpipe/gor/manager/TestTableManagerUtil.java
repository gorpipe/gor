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

package org.gorpipe.gor.manager;

import org.apache.commons.io.IOUtils;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.lock.TableLock;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestTableManagerUtil {
    private static final Logger log = LoggerFactory.getLogger(TestTableManagerUtil.class);

    public TestTableManagerUtil() {
    }

    Process startGorManagerCommand(String table, String[] optionsArgs, String command, String[] commandOptionsArgs, String workingDir)
            throws IOException, InterruptedException, ExecutionException {

        List<String> arguments = new ArrayList<String>();
        arguments.add("java");
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add("-Dlogback.configurationFile=" + Paths.get("..").toFile().getAbsolutePath() + "/tests/config/logback-test.xml");
        arguments.add("org.gorpipe.gor.manager.TableManagerCLI");
        if (optionsArgs != null) {
            arguments.addAll(Arrays.asList(optionsArgs));
        }
        arguments.add(table);
        arguments.add(command);
        if (commandOptionsArgs != null) {
            arguments.addAll(Arrays.asList(commandOptionsArgs));
        }

        log.trace("Running: {}", String.join(" ", arguments));

        ProcessBuilder pb = new ProcessBuilder(arguments);
        if (workingDir != null) {
            pb.directory(new File(workingDir));
        }

        return pb.start();
    }

    String waitForProcessPlus(Process p) throws InterruptedException, IOException, ExecutionException {

        final long timeoutInS = 30;

        final StringWriter out_writer = new StringWriter();
        final StringWriter err_writer = new StringWriter();
        startProcessStreamEaters(p, out_writer, err_writer);
        boolean noTimeout = p.waitFor(timeoutInS, TimeUnit.SECONDS);

        final String processOutput = out_writer.toString();
        final String errorOutput = err_writer.toString();
        if (errorOutput != null && errorOutput.length() > 0) {
            log.warn("Process error output - ==================================== start ====================================");
            log.warn(errorOutput);
            log.warn("Process error output - ==================================== stop  ====================================");
        }

        if (noTimeout) {
            // Process did finish
            int errCode = p.exitValue();
            if (errCode != 0) {
                log.warn("Process output - ==================================== start ====================================");
                log.warn(processOutput);
                log.warn("Process output - ==================================== stop  ====================================");
                throw new ExecutionException("BaseTable manager command failed with exit code " + errCode, null);
            }
        } else {
            log.warn("Process output - ==================================== start ====================================");
            log.warn(processOutput);
            log.warn("Process output - ==================================== stop  ====================================");
            throw new ExecutionException("BaseTable manager command timed out in " + timeoutInS + " seconds", null);
        }

        return processOutput;
    }

    void startProcessStreamEaters(Process p, Writer outWriter, Writer errWriter) {
        new Thread(() -> {
            try {
                IOUtils.copy(p.getInputStream(), outWriter);
            } catch (IOException e) {
                // Ignore
            }
        }).start();
        new Thread(() -> {
            try {
                IOUtils.copy(p.getErrorStream(), errWriter);
            } catch (IOException e) {
                // Ignore
            }
        }).start();
    }

    public String executeGorManagerCommand(String table, String[] optionsArgs, String command, String[] commandOptionsArgs, String workingDir, boolean sync)
            throws IOException, InterruptedException, ExecutionException {
        Process p = startGorManagerCommand(table, optionsArgs, command, commandOptionsArgs, workingDir);
        if (sync) {
            return waitForProcessPlus(p);
        } else {
            return "";
        }
    }

    void waitForBucketizeToStart(DictionaryTable table, Process p) throws InterruptedException, IOException, ExecutionException {
        long startTime = System.currentTimeMillis();
        while (true) {
            try (TableLock bucketizeLock = TableLock.acquireWrite(TableManager.DEFAULT_LOCK_TYPE, table, "bucketize", Duration.ofMillis(100))) {
                if (!bucketizeLock.isValid()) {
                    break;
                }
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                log.info(waitForProcessPlus(p));
                Assert.assertTrue("Test not setup correctly, thread did not get bucketize lock, took too long.", false);
            }
            Thread.sleep(100);
        }
    }
}