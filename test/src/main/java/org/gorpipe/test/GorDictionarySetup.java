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

package org.gorpipe.test;

/**
 * Created by gisli on 16/02/16.
 */

import org.apache.commons.lang3.ArrayUtils;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.util.DataUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

import static org.gorpipe.gor.manager.BucketManager.BUCKET_FILE_PREFIX;

/**
 * Helper class to create Gor dictionary (with data files and bucket files).
 * <p>
 * The static methods can also be used standalone to create test data.
 */
public class GorDictionarySetup {
    public String name;
    public Path dictionary;
    public String[] dataFiles;
    public Path[] bucketFiles;

    public static Map<String, List<String>>  createDataFilesMap(String name, Path path, int fileCount, int[] chrs, int rowsPerChr, String sourceColumn,
                                                               boolean oneSourcePerFile, String[] sources) throws IOException {
        return createDataFilesMap(name, path, fileCount, chrs, rowsPerChr, sourceColumn, oneSourcePerFile, sources, false, false, 0, "gor");
    }

    public static Map<String, List<String>> createDataFilesMap(String name, Path path, int fileCount, int[] chrs, int rowsPerChr, String sourceColumn,
                                                               boolean oneSourcePerFile, String[] sources, boolean addSource, boolean writeFileToJoinWith) throws IOException {
        return createDataFilesMap(name, path, fileCount, chrs, rowsPerChr, sourceColumn, oneSourcePerFile, sources, addSource, writeFileToJoinWith, 0, "gor");
    }

    /**
     * Create data files.
     *
     * @param name             name/id for the data file set.
     * @param path             path to where the files should be created.  If null we use default temp folder.
     * @param fileCount        total number of files to create.
     * @param chrs             chromosomes to create per file.
     * @param rowsPerChr       rows per chromosome.
     * @param sourceColumn     name of the source column.
     * @param oneSourcePerFile if true each file contains only one source (round robin per file), otherwise we use round robin per line.
     * @param sources          values to put in the source column (round robin). If no source are given we use integers (each used once)
     * @param addSource        add source column to data row.
     * @param writeFileToJoinWith  ??
     * @param sizeOfLargeData if larger than 0, then a new field (LargeData) is added to file (after RandomData) where each field contains random data of the given size)
     * @param type            file einding for the data.
     * @return map with files per alias.
     * @throws IOException
     */
    public static Map<String, List<String>> createDataFilesMap(String name, Path path, int fileCount, int[] chrs, int rowsPerChr, String sourceColumn,
                                                               boolean oneSourcePerFile, String[] sources, boolean addSource,
                                                               boolean writeFileToJoinWith, int sizeOfLargeData, String type) throws IOException {
        Map<String, List<String>> data = new HashMap<>();
        Random random = new Random(314);   // Want use the same sequence of random data.
        int sourceIndex = 0;
        ArrayList<Integer[]> lines = new ArrayList<>();

        for (int i = 0; i < fileCount; i++) {
            String source = sources != null && sources.length > 0 ? sources[sourceIndex % sources.length] : String.format("PN%d", sourceIndex + 1);
            sourceIndex++;

            String fileNamePrefix = String.format("%s_datafile_%d_%s", name, i + 1, oneSourcePerFile ? source : "");
            Path dataFilePath = path != null ? Files.createFile(path.resolve(fileNamePrefix + "." + type)) : Files.createTempFile(fileNamePrefix, "." + type);
            dataFilePath.toFile().deleteOnExit();

            // If we multiple sources per file, we can't map single source to a list files, so we map all to "All".
            data.computeIfAbsent(oneSourcePerFile ? source : "All", k -> new ArrayList<>()).add(dataFilePath.toString());

            try (PrintWriter out = new PrintWriter(dataFilePath.toFile())) {
                // Header
                out.println(String.format("Chr\tPos\t%s\tChromoInfo\tConstData\tRandomData%s%s",
                        sourceColumn, sizeOfLargeData > 0 ? "\tLargeData" : "" , addSource ? "\tSource" : ""));
                // Lines
                for (int ci = 0; ci < chrs.length; ci++) {
                    int c = chrs[ci];
                    int[] positions = new int[rowsPerChr];
                    for (int j = 0; j < positions.length; j++) {
                        positions[j] = random.nextInt(1000000);
                    }
                    Arrays.sort(positions);
                    for (int p = 1; p <= rowsPerChr; p++) {
                        if (!oneSourcePerFile) {
                            source = sources != null && sources.length > 0 ? sources[sourceIndex % sources.length] : String.valueOf(sourceIndex + 1);
                            sourceIndex++;
                        }

                        String largeData = "";
                        if (sizeOfLargeData > 0) {
                            largeData = generateRandomString(sizeOfLargeData);
                        }

                        out.print(String.format("chr%d\t%d\t%s\tLineData for the chromosome and position line %d %d\tThis line should be long enough for this test purpose\t%d%s%s%n",
                                c, p, source, c, p, positions[p - 1], sizeOfLargeData > 0 ? "\t" + largeData : "", 
                                addSource ? "\t" + source : "")
                        );
                        if (writeFileToJoinWith) lines.add(new Integer[]{c, p});
                    }
                }
            }
            if (writeFileToJoinWith) {
                Path joinFilePath = path != null ? Files.createTempFile(path, "_joinFile", "." + type) : Files.createTempFile("_joinFile", "." + type);
                try (PrintWriter joinFileWriter = new PrintWriter(joinFilePath.toFile())) {
                    lines.sort((line1, line2) -> {
                        int cmp = Integer.toString(line1[0]).compareTo(Integer.toString(line2[0]));
                        if (cmp == 0) return Integer.compare(line1[1], line2[1]);
                        return cmp;
                    });
                    joinFileWriter.write("Chr\tPos");
                    lines.forEach(line -> joinFileWriter.println(String.format("chr%d\t%d", line[0], line[1])));
                }
            }
        }


        return data;
    }

    public static String generateRandomString(int sizeOfLargeData) {
        String alphabet = "1234567890abcdefghijklmnopqrst";
        Random random = new Random();
        byte[] buffer = new byte[sizeOfLargeData];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte)alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(buffer);
    }


    public GorDictionarySetup(String name, int fileCount, int bucketSize, int[] chrs, int rowsPerChr) throws IOException {
        this(name, fileCount, bucketSize, chrs, rowsPerChr, false);
    }

    public GorDictionarySetup(String name, int fileCount, int bucketSize, int[] chrs, int rowsPerChr, boolean addMultiTagLines) throws IOException {
        this(null, name, fileCount, bucketSize, chrs, rowsPerChr, addMultiTagLines);
    }

    /**
     * Constructor
     *
     * @param root       Root folder
     * @param name       Name to identify the files with (without .gord).
     * @param fileCount  Number of data files to create (same as number of pns used)
     * @param bucketSize Number of files in each bucket.
     * @param chrs       Array with the chromosomes to use.
     * @param rowsPerChr Number of rows per chromosome in each file.
     * @throws IOException
     */
    public GorDictionarySetup(Path root, String name, int fileCount, int bucketSize, int[] chrs, int rowsPerChr, boolean addMultiTagLines) throws IOException {
        if (root == null) {
            root = Files.createTempDirectory("tempdict");
            root.toFile().deleteOnExit();
        }

        this.name = name;

        this.bucketFiles = new Path[fileCount / bucketSize];

        String[] sources = IntStream.range(1, fileCount + 1).mapToObj(i -> String.format("PN%d", i)).toArray(size -> new String[size]);

        // Create data file
        Map<String, List<String>> data = GorDictionarySetup.createDataFilesMap(name, root, fileCount, chrs, rowsPerChr, "PN", true, sources);
        this.dataFiles = data.values().stream().flatMap(Collection::stream).toArray(String[]::new);

        // Bucketize
        Path bucketRelPath = Path.of("." + name +"/buckets");
        Path bucketDir = root.resolve(bucketRelPath);
        Files.createDirectories(bucketDir);

        for (int i = 0; i < this.bucketFiles.length; i++) {
            this.bucketFiles[i] = bucketRelPath.resolve(name + "_" + BUCKET_FILE_PREFIX + "_" + i + DataType.GOR.suffix);
            this.bucketFiles[i].toFile().deleteOnExit();
        }

        HashMap<Path, ArrayList<String>> mapBucketToFiles = new HashMap<>();
        HashMap<String, Path> mapFileToBuckets = new HashMap<>();
        HashMap<String, String> mapFileToAlias = new HashMap<>();

        // Assign to buckets.

        int fileIndex = 0;
        for (String alias : data.keySet()) {
            for (String dataFile : data.get(alias)) {
                int bucketIndex = fileIndex / bucketSize;
                if (bucketIndex < this.bucketFiles.length) {
                    mapBucketToFiles.computeIfAbsent(this.bucketFiles[bucketIndex], k -> new ArrayList<>()).add(dataFile);
                    mapFileToBuckets.put(dataFile, this.bucketFiles[bucketIndex]);
                }
                mapFileToAlias.put(dataFile, alias);
                fileIndex++;
            }
        }

        // Create bucket files

        for (Path bucketFile : this.bucketFiles) {
            boolean printHeader = true;
            try (PrintWriter out = new PrintWriter(root.resolve(bucketFile).toFile())) {
                ArrayList<String>[] fileLines = (ArrayList<String>[]) new ArrayList[mapBucketToFiles.get(bucketFile).size()];
                for (int k = 0; k < fileLines.length; k++) fileLines[k] = new ArrayList<>();
                int i = 0;
                for (String dataFile : mapBucketToFiles.get(bucketFile)) {
                    try (BufferedReader br = new BufferedReader(new FileReader(root.resolve(dataFile).toString()))) {
                        String line;

                        // Read the header.
                        line = br.readLine();
                        if (printHeader) {
                            out.println(line + ("\tSource"));
                            printHeader = false;
                        }

                        // Read the lines.
                        while ((line = br.readLine()) != null) {
                            fileLines[i].add(line);
                        }
                        i++;
                    }
                }
                int linesPerFile = fileLines[0].size(); //assuming that the only difference between the datafiles is the tags.
                int a = 0;
                for (int b = 0; b < linesPerFile; ++b) {
                    for (String dataFile : mapBucketToFiles.get(bucketFile)) {
                        out.println(fileLines[a].get(b) + ("\t" + mapFileToAlias.get(dataFile)));
                    }
                }
                out.flush();
            }
        }

        // Create dictionary file.

        this.dictionary = root.resolve(name + DataType.GORD.suffix);
        this.dictionary.toFile().deleteOnExit();
        try (PrintWriter out = new PrintWriter(this.dictionary.toFile())) {
            for (String dataFile : this.dataFiles) {
                String bucketInfo = mapFileToBuckets.containsKey(dataFile) ? "|" + mapFileToBuckets.get(dataFile) : "";
                out.print(root.relativize(Paths.get(dataFile)).toString()  + bucketInfo + "\t" + mapFileToAlias.get(dataFile));
                out.println();
            }
            out.flush();
        }

        if (addMultiTagLines) {
            //  Add one bucket file with multitags.

            String[] sourcesMultiTags = IntStream.range(1, 10 + 1).mapToObj(n -> String.format("PNMA%d", n)).toArray(size -> new String[size]);
            Map<String, List<String>> dataMultiTags = GorDictionarySetup.createDataFilesMap(name, null, 1, chrs, rowsPerChr, "PN", false, sourcesMultiTags);
            this.dataFiles = (String[]) ArrayUtils.addAll(this.dataFiles, data.values().stream().flatMap(Collection::stream).toArray(String[]::new));

            try (PrintWriter out = new PrintWriter(new FileOutputStream(this.dictionary.toFile(), true))) {
                for (String dataFile : dataMultiTags.get("All")) {
                    out.print(dataFile + "\t" + "Many");
                    out.print(String.format("\tchr1\t1\tchrz\t1\t%s", String.join(",", sourcesMultiTags)));
                    out.println();
                }
                out.flush();
            }

            //  Add one bucket file with multitags and source column set.

            String[] sourcesMultiTags2 = IntStream.range(1, 10 + 1).mapToObj(n -> String.format("PNMB%d", n)).toArray(size -> new String[size]);
            Map<String, List<String>> dataMultiTags2 = GorDictionarySetup.createDataFilesMap(name, null, 1, chrs, rowsPerChr, "PN", false, sourcesMultiTags2, true, false);
            this.dataFiles = (String[]) ArrayUtils.addAll(this.dataFiles, data.values().stream().flatMap(Collection::stream).toArray(String[]::new));

            try (PrintWriter out = new PrintWriter(new FileOutputStream(this.dictionary.toFile(), true))) {
                for (String dataFile : dataMultiTags2.get("All")) {
                    out.print(dataFile + "\t" + "Many");
                    out.print(String.format("\tchr1\t1\tchrz\t1\t%s", String.join(",", sourcesMultiTags2)));
                    out.println();
                }
                out.flush();
            }
        }
    }


    /**
     * Creates a dictionary with horizontal data.
     *
     * The data created will be:
     * <root>
     *      <name>
     *          <name>.gord
     *          <name>_buckets.tsv
     *          data
     *              <name>_bucket_1.gorz
     *              <name>_bucket_2.gorz
     *              ...
     *              <name>_bucket_<numberOfBuckets>.gorz
     *    
     *
     * @param root   the root where the data will be creaed
     * @param name   name of the dictionary
     * @param numberOfBuckets number of buckets to use
     * @param bucketSize      number of pn per bucket
     * @param numberOfVariants number of variants used per file.
     */
    public static void createHorizontalDictionary(String root, String name, int numberOfBuckets, int bucketSize, int numberOfVariants) throws IOException {
        Random random = new Random(67);  // Want always the same random vars.
        Path dictDir = Files.createDirectory(Paths.get(root, name));
        dictDir.toFile().deleteOnExit();

        Path dictDataDir = Files.createDirectory(Paths.get(root, name, "data"));
        dictDataDir.toFile().deleteOnExit();

        Path pnBucketMapPath = Files.createFile(Paths.get(root, name, DataUtil.toFile(name + "_buckets", DataType.TSV)));
        pnBucketMapPath.toFile().deleteOnExit();

        Path gordPath = Files.createFile(Paths.get(root, name, name + DataType.GORD.suffix));
        pnBucketMapPath.toFile().deleteOnExit();

        List<String> bucketTemplateList = new ArrayList<>();
        for (int lineIndex = 1; lineIndex <= numberOfVariants; lineIndex++) {
            String ref = randomRef(random);
            String template = String.format("%s\t%d\trs%d\t%s\t%s",
                    "chr1", lineIndex, random.nextInt(100000),
                    ref, randomAlt(random, ref));
            bucketTemplateList.add(template);
        }


        try(PrintWriter pnBucketMapPrintWriter = new PrintWriter(pnBucketMapPath.toFile());
            PrintWriter gordPrintWriter = new PrintWriter(gordPath.toFile())) {

            int pnIndex = 0;
            for (int bucketIndex = 1; bucketIndex <= numberOfBuckets; bucketIndex++) {
                String bucket = String.format("%d", bucketIndex);
                String[] pnArray = new String[bucketSize];
                for (int bucketItemIndex = 0; bucketItemIndex < bucketSize; bucketItemIndex++) {
                    pnIndex++;
                    String pn = "PN" + pnIndex;
                    pnArray[bucketItemIndex] = pn;

                    pnBucketMapPrintWriter.println(pn + "\t" + bucket);
                }
                
                String pnArrayAsString =  String.join(",", pnArray);

                gordPrintWriter.println(String.format("data/%s_bucket_%s.gor\t%s\tchr1\t0\tchrZ\t1000000000\t%s",
                        name, bucket, bucket, pnArrayAsString));

                Path bucketDataPath = Files.createFile(Paths.get(root, name, "data", name + "_bucket_" + bucket + DataType.GOR.suffix));
                try(PrintWriter bucketDataPrintWriter = new PrintWriter(bucketDataPath.toFile())) {
                    bucketDataPrintWriter.println("#Chrom\tPos\tId\tRef\tAlt\tBucket\tValues");
                    for (int lineIndex = 1; lineIndex <= numberOfVariants; lineIndex++) {
                        bucketDataPrintWriter.println(String.format("%s\t%s\t%s",
                                bucketTemplateList.get(lineIndex-1), bucket, generateRandomHorizontalGtValues(random, 2*pnArray.length)));
                    }
                }
            }
        }
    }

    public static String randomRef(Random random) {
        String alphabet = "ACGT";
        int index = random.nextInt(alphabet.length());
        return alphabet.substring(index, index+1);
    }

    public static String randomAlt(Random random, String ref) {
        String alphabet = "ACGT";
        alphabet = alphabet.replace(ref, "");
        int index = random.nextInt(alphabet.length());
        return alphabet.substring(index, index+1);
    }

    public static String generateRandomHorizontalGtValues(Random random, int sizeOfLargeData) {
        String alphabet = "~*[]{}1234567890abcdefghijklmnopqrstABCDEFGHIJKLMOPQRST";
        byte[] buffer = new byte[sizeOfLargeData];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte)alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(buffer);
    }
}
