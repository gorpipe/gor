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

package org.gorpipe.gor.cli.cache;

import org.gorpipe.gor.driver.meta.DataType;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "purge",
        aliases = {"p"},
        description="Purge cache based on age of cached files.")
public class PurgeCommand extends FilterOptions implements  Runnable{

    @CommandLine.Option(names = {"-y", "--nopropmpt"},
            description = "No verification showed before deleting files")
    private boolean noVerification;

    @CommandLine.Parameters(index = "1",
            arity = "0..1",
            defaultValue = "10",
            paramLabel = "AgeOfFile",
            description = "The age of files to keep in the cache. Age can is defined in number of days, defaults to 10 days.")
    private int ageInDays;

    @Override
    public void run() {

        if (!cachePath.exists() || !cachePath.isDirectory()){
            System.err.printf("Cache path %1$s does not exist!", cachePath.toString());
            System.exit(-1);
        }

        analyseCache(cachePath, ageInDays);
        displayResult();
        verifyAndDeleteFromCache();
    }

    private int fileCounter;
    private AnalysisResult result;
    private long ageTimeCutoff;

    private void analyseCache(File cachePath, int age) {

        result = new AnalysisResult(this.cachePath.toPath());
        fileCounter = 0;
        ageTimeCutoff = System.currentTimeMillis() - Duration.ofDays(age).toMillis();

        listFiles(cachePath);
    }

    private void listFiles(File parentFile) {
        // get all the files from a directory
        File[] fileArray = parentFile.listFiles();

        if (fileArray == null) return;

        for (File file : fileArray) {
            if (file.isFile()) {
                long lastAccessTime = -1;

                try {
                    BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    FileTime time = attrs.lastAccessTime();
                    lastAccessTime = time.toMillis();
                } catch (Exception e) {
                    System.err.println("!!Failed to get last access time for: " + file.toString());
                }

                this.fileCounter++;

                if (lastAccessTime > 0 && lastAccessTime < ageTimeCutoff) {
                    DataType type = DataType.fromFileName(file.getName());

                    if (type != null) {
                        result.process(file.toPath());
                    }
                }
                System.err.print("Processing files: " + this.fileCounter + "\r");
            } else if (file.isDirectory()) {
                listFiles(file);
            }
        }
    }

    private void verifyAndDeleteFromCache() {

        if (this.result.getFileList().size() == 0) {
            System.err.println("No files to remove. Exiting.");
            System.exit(0);
        }

        if (!noVerification) {
            Scanner user_input = new Scanner(System.in);
            System.err.print("Remove files from cache? [y/n]:");
            while (true) {
                String answer = user_input.next().trim().toLowerCase();

                if (answer.equals("y")) {
                    break;
                } else if (answer.equals("n")) {
                    System.exit(0);
                }
            }
            System.err.println();
        }

        System.err.println("Removing files from cache at " + this.cachePath);

        List<String> fileNames = this.result.getFileList();
        int fileRemovalCounter = 0;
        String extra = dryRun ? " (dryrun)" : "";

        for (String filename : fileNames) {
            File fileToDelete = new File(cachePath, filename);

            if (verbose) {
                System.err.println("Removing: " + filename + extra );
            }

            if (!dryRun) {
                try {
                    if (!fileToDelete.delete()) {
                        System.err.println("Failed to delete file: " + filename);
                    }
                    fileRemovalCounter++;
                } catch (Exception e) {
                    System.err.println("An error occurred when deleting file: " + filename);
                    System.err.println(e.getMessage());
                }
            } else {
                fileRemovalCounter++;
            }
        }

        System.err.printf("Removed %1$d from cache%2$s%n", fileRemovalCounter, extra);

    }

    private void displayResult() {
        System.err.println("Processed files: " + this.fileCounter + "       ");
        System.err.println("Files to remove: " + result.getFileList().size());

        if (result.getFileList().size() > 0) {
            System.err.println("Summary by extension:");
            Map<String, Integer> extensionMap = result.getExtensionCountMap();
            for (Map.Entry<String, Integer> entry : extensionMap.entrySet()) {
                System.err.printf("\t%1$s\t%2$d%n", entry.getKey(), entry.getValue());
            }
            System.err.println();
        }
    }
}
