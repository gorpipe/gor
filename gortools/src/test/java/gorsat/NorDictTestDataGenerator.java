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

package gorsat;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

class NorDictTestDataGenerator {
    static final String HEADER_DEFAULT = "#Constant\tCounter\tIndex\n";
    static final String HEADER_WITH_SOURCE = "#Constant\tCounter\tIndex\tSource\n";

    private int numberOfDictionaryFiles;
    private int numberOfLinesInDictionaryFile;
    private boolean sourceFileHeader;
    private boolean relativePaths;
    private boolean addSourceHeader;
    private boolean sourceColumn;
    private int emptyFileIndex;
    private int headerOnlyFileIndex;
    private final int fileMissingIndex;


    NorDictTestDataGenerator(
            int numberOfDictionaryFiles,
            int numberOfLinesInDictionaryFile,
            boolean sourceFileHeader,
            boolean relativePaths,
            boolean addSourceHeader,
            boolean sourceColumn,
            int emptyFileIndex,
            int headerOnlyFileIndex,
            int fileMissingIndex
    ) {
        this.numberOfDictionaryFiles = numberOfDictionaryFiles;
        this.numberOfLinesInDictionaryFile = numberOfLinesInDictionaryFile;
        this.sourceFileHeader = sourceFileHeader;
        this.relativePaths = relativePaths;
        this.addSourceHeader = addSourceHeader;
        this.sourceColumn = sourceColumn;
        this.emptyFileIndex = emptyFileIndex;
        this.headerOnlyFileIndex = headerOnlyFileIndex;
        this.fileMissingIndex = fileMissingIndex;
    }

    String invoke() throws IOException {

        File tmpDir = Files.createTempDir();
        tmpDir.deleteOnExit();
        File subTmpDir = new File(tmpDir, "files");
        subTmpDir.mkdir();

        // Create nord file and the associated file entries
        StringBuilder builder = new StringBuilder();

        if (addSourceHeader) {
            builder.append("##Source=phenotype\n");
        }

        builder.append("#Source\tSubject\n");

        String subPath = subTmpDir.getAbsolutePath();

        if (relativePaths) {
            subPath = "files";
        }

        for (int i = 0; i < numberOfDictionaryFiles; i++) {
            builder.append(String.format("%2$s/file_%1$d.tsv\tPatient_%1$d\n", i, subPath));

            if (i != fileMissingIndex) {
                StringBuilder fileBuilder = new StringBuilder();

                if (i != emptyFileIndex) {
                    addHeader(fileBuilder);
                    if(i != headerOnlyFileIndex) {
                        addLines(fileBuilder, i);
                    }
                }

                FileUtils.writeStringToFile(new File(subTmpDir, String.format("file_%1$d.tsv", i)), fileBuilder.toString(), Charset.defaultCharset());
            }
        }

        FileUtils.writeStringToFile(new File(tmpDir, "test.nord"), builder.toString(), Charset.defaultCharset());

        return tmpDir.getAbsolutePath();
    }

    private void addHeader(StringBuilder fileBuilder) {
        if (sourceFileHeader) {
            if (sourceColumn)
                fileBuilder.append(HEADER_WITH_SOURCE);
            else
                fileBuilder.append(HEADER_DEFAULT);
        }
    }

    private void addLines(StringBuilder fileBuilder, int i) {
        for (int j = 0; j < numberOfLinesInDictionaryFile; j++) {
            if (sourceColumn)
                fileBuilder.append(String.format("Foo\t%1$d\t%2$d\tSource_%1$d\n", i, j));
            else
                fileBuilder.append(String.format("Foo\t%1$d\t%2$d\n", i, j));
        }
    }
}
