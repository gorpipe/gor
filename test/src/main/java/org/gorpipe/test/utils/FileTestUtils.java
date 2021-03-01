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

package org.gorpipe.test.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


public class FileTestUtils {

    private FileTestUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a temp directory
     *
     * @param prefix
     * @return
     * @throws IOException
     */

    public static File createTempDirectory(String prefix) throws IOException {
        File tempDirectory = Files.createTempDirectory(prefix).toFile();
        tempDirectory.deleteOnExit();
        return tempDirectory;
    }

    /**
     * Creates a temp file in a supplied directory with a supplied file name and data
     *
     * @param directory
     * @param fileName
     * @param data
     * @return
     * @throws IOException
     */

    public static File createTempFile(File directory, String fileName, String data) throws IOException {
        File tempFile = new File(directory, fileName);
        handleTempFile(tempFile, data);
        return tempFile;
    }

    private static void handleTempFile(File tempFile, String data) throws IOException {
        tempFile.deleteOnExit();
        FileUtils.writeStringToFile(tempFile, data, Charset.defaultCharset());
    }

    /**
     * Creates a generic file with a supplied number of lines
     *
     * @param directory
     * @param lines
     * @return
     * @throws IOException
     */

    public static File createLinesFile(File directory, int lines) throws IOException {
        return FileTestUtils.createTempFile(directory, "lines_" + lines + ".txt", FileTestUtils.getLinesString(lines));
    }

    /**
     * Creates a dummy gor file
     *
     * @param directory
     * @return
     * @throws IOException
     */

    public static File createDummyGorFile(File directory) throws IOException {
        return FileTestUtils.createTempFile(directory, "dummy.gor",
                "chrom\tpos\ta\n" +
                        "chr1\t0\tb");
    }

    /**
     * Create an empty file
     *
     * @param directory
     * @return
     * @throws IOException
     */

    public static File createEmptyFile(File directory) throws IOException {
        return FileTestUtils.createTempFile(directory, "empty_file.txt", "");
    }

    /**
     * Creates a generic small gor file which can be used in many tests
     *
     * @param directory
     * @return
     * @throws IOException
     */

    public static File createGenericSmallGorFile(File directory) throws IOException {
        return FileTestUtils.createTempFile(directory, "generic.gor",
                "#Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                        "chr1\t11868\t14412\tDDX11L1\n" +
                        "chr1\t14362\t29806\tWASH7P\n" +
                        "chr1\t29553\t31109\tMIR1302-11\n" +
                        "chr1\t34553\t36081\tFAM138A\n" +
                        "chr1\t52472\t54936\tOR4G4P\n" +
                        "chr1\t62947\t63887\tOR4G11P\n" +
                        "chr1\t69090\t70008\tOR4F5\n" +
                        "chr1\t89294\t133566\tRP11-34P13.7\n" +
                        "chr1\t89550\t91105\tRP11-34P13.8\n"
        );
    }

    // Bucket for the small gorfile
    public static File createGenericSmallGorFileBucket(File directory, String source) throws IOException {
        return FileTestUtils.createTempFile(directory, "generic_bucket.gor",
                "#Chrom\tgene_start\tgene_end\tGene_Symbol\tSource\n" +
                        "chr1\t11868\t14412\tDDX11L1\t" + source + "\n" +
                        "chr1\t14362\t29806\tWASH7P\t" + source + "\n" +
                        "chr1\t29553\t31109\tMIR1302-11\t" + source + "\n" +
                        "chr1\t34553\t36081\tFAM138A\t" + source + "\n" +
                        "chr1\t52472\t54936\tOR4G4P\t" + source + "\n" +
                        "chr1\t62947\t63887\tOR4G11P\t" + source + "\n" +
                        "chr1\t69090\t70008\tOR4F5\t" + source + "\n" +
                        "chr1\t89294\t133566\tRP11-34P13.7\t" + source + "\n" +
                        "chr1\t89550\t91105\tRP11-34P13.8\t" + source + "\n"
        );
    }

    /**
     * Create a generic dictionary file with a supplied path to a gor file along with with pns "a" and "b"
     *
     * @param directory
     * @param gorFilePath
     * @param fileName
     * @return
     * @throws IOException
     */

    public static File createGenericDictionaryFile(File directory, String gorFilePath, String fileName) throws IOException {
        return FileTestUtils.createTempFile(directory, fileName,
                gorFilePath + "\ta\n" +
                        gorFilePath + "\tb\n"
        );
    }

    /**
     * Create a generic PN file without a header with PNs a and b
     *
     * @param directory
     * @return
     * @throws IOException
     */

    public static File createPNTxtFile(File directory) throws IOException {
        return FileTestUtils.createTempFile(directory, "pns.txt",
                "a\n" +
                        "b\n"
        );
    }

    /**
     * Create a generic PN file with a header and PNs a and b
     *
     * @param directory
     * @return
     * @throws IOException
     */

    public static File createPNTsvFile(File directory) throws IOException {
        return FileTestUtils.createTempFile(directory, "pns.tsv",
                "#PN\n" +
                        "a\n" +
                        "b\n"
        );
    }

    // ---------------------------------------------
    // Delete stuff
    // ---------------------------------------------

    /**
     * Delete a specified folder and all its contents.
     *
     * @param path The path to the folder.
     * @throws IOException on I/O error
     */
    public static void deleteFolder(Path path) throws IOException {
        java.nio.file.Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                java.nio.file.Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    java.nio.file.Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                throw exc;
            }
        });
    }

    /**
     * Delete a specified folder and all its contents.
     *
     * @param src The File descriptior for the folder.
     * @return true if folder is deleted, else false.
     */
    public static boolean deleteFolder(File src) {
        if (src != null) {
            deleteFolderContents(src);
            src.delete();
            return true;
        }

        return false;
    }

    /**
     * Delete the content of a specified folder.
     *
     * @param src The file descriptor for the folder.
     */
    private static void deleteFolderContents(File src) {
        if (src != null) {
            File[] files = src.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory())
                        deleteFolder(file);
                    else
                        file.delete();
                }
            }
        }
    }

    private static String getLinesString(int lines) {
        int digits = (int) Math.floor(Math.log10(lines));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            String format = "%0" + digits + "d";
            sb.append(String.format(format, i)).append("\n");
        }
        return sb.toString();
    }

}
