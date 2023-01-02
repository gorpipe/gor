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

import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    String waitForProcessPlus(Process p) throws InterruptedException, ExecutionException {

        final long timeoutInS = 30;

        final var out_writer = new ByteArrayOutputStream();
        final var err_writer = new ByteArrayOutputStream();
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

    void startProcessStreamEaters(Process p, OutputStream outWriter, OutputStream errWriter) {
        new Thread(() -> {
            try(var in = p.getInputStream()) {
                in.transferTo(outWriter);
            } catch (IOException e) {
                // Ignore
            }
        }).start();
        new Thread(() -> {
            try(var in = p.getErrorStream()) {
                in.transferTo(errWriter);
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
            // Wait for the temp file to be created (as then the dict has been read)
            try(var pathList = Files.list(Path.of(table.getRootPath()))) {
                long tempFiles  = pathList.filter(f -> f.toString().contains(table.getName() + ".temp.bucketizing.")).count();
                if (tempFiles == 1) {
                    break;
                }
            }
            // Option to wait for the bucketize look to be taken (by someone else) but then we don't control which version of the dict is bucektized.
//            try (TableLock bucketizeLock = TableLock.acquireWrite(TableManager.DEFAULT_LOCK_TYPE, table, "bucketize", Duration.ofMillis(100))) {
//                if (!bucketizeLock.isValid()) {
//                    break;
//                }
//            }

            if (System.currentTimeMillis() - startTime > 20000) {
                log.info(waitForProcessPlus(p));
                Assert.fail("Test not setup correctly, thread did not get bucketize lock, took too long.");
            }
            Thread.sleep(100);
        }
        Thread.sleep(1000);  // Making sure we have read the table after we get the lock.
    }
}