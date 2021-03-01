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

public class NorDictTestDataGeneratorBuilder {
    private int numberOfDictionaryFiles;
    private int numberOfLinesInDictionaryFile;
    private boolean sourceFileHeader = false;
    private boolean relativePaths = false;
    private boolean addSourceHeader = false;
    private boolean sourceColumn = false;
    private boolean firstFileEmpty = false;
    private boolean firstFileHeaderOnly = false;
    private int emptyFileIndex = -1;
    private int headerOnlyFileIndex = -1;
    private int fileMissingIndex = -1;

    public NorDictTestDataGeneratorBuilder setNumberOfDictionaryFiles(int numberOfDictionaryFiles) {
        this.numberOfDictionaryFiles = numberOfDictionaryFiles;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setNumberOfLinesInDictionaryFile(int numberOfLinesInDictionaryFile) {
        this.numberOfLinesInDictionaryFile = numberOfLinesInDictionaryFile;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setSourceFileHeader(boolean sourceFileHeader) {
        this.sourceFileHeader = sourceFileHeader;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setRelativePaths(boolean relativePaths) {
        this.relativePaths = relativePaths;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setAddSourceHeader(boolean addSourceHeader) {
        this.addSourceHeader = addSourceHeader;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setSourceColumn(boolean sourceColumn) {
        this.sourceColumn = sourceColumn;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setFileEmpty(int i) {
        emptyFileIndex = i;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setFileHeaderOnly(int i) {
        headerOnlyFileIndex = i;
        return this;
    }

    public NorDictTestDataGeneratorBuilder setFileMissing(int i) {
        fileMissingIndex = i;
        return this;
    }

    public NorDictTestDataGenerator createNorDictTestDataGenerator() {
        return new NorDictTestDataGenerator(
                numberOfDictionaryFiles,
                numberOfLinesInDictionaryFile,
                sourceFileHeader,
                relativePaths,
                addSourceHeader,
                sourceColumn,
                emptyFileIndex,
                headerOnlyFileIndex,
                fileMissingIndex
        );
    }
}