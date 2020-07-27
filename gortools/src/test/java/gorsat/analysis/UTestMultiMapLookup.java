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

package gorsat.analysis;

import gorsat.Analysis.MultiMapLookup;
import org.apache.commons.io.FileUtils;
import org.gorpipe.model.gor.iterators.LineIterator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class UTestMultiMapLookup {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    public File createDoubleKeyTestFiles() throws IOException {
        LineIterator iterator = UTestMapLookup.getDoubleKeyLineIterator();
        StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            builder.append(iterator.nextLine()).append("\n");
        }

        File testFile = workDir.newFile("testData.txt");
        FileUtils.writeStringToFile(testFile, builder.toString(), Charset.defaultCharset());
        return testFile;
    }

    @Test
    public void multiMapLookupWithSingleOutColumn() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\tFooFoo",
                "chr2\t2\tB\tFooBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {2};

        performTest(input, output, lookupColumns, outColumns, "1", UTestMapLookup.getDoubleKeyLineIterator(), null, "", false, false);
    }

    @Test
    public void multiMapLookupWithTwoOutColumns() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\t1\tFooFoo",
                "chr2\t2\tB\t2\tFooBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {1, 2};

        performTest(input, output, lookupColumns, outColumns, "1", UTestMapLookup.getDoubleKeyLineIterator(), null, "", false, false);
    }

    @Test
    public void doubleKeyLookupFromLineIteratorTable() {
        String[] input = {
                "chr1\t1\tA\t1",
                "chr2\t2\tB\t2",
                "chr2\t2\tB\t3"};
        String[] output = {
                "chr1\t1\tA\t1\t1\tFooFoo",
                "chr2\t2\tB\t2\t2\tFooBar"};
        int[] lookupColumns = {2, 3};
        int[] outColumns = {1, 2};

        performTest(input, output, lookupColumns, outColumns, "1", UTestMapLookup.getDoubleKeyLineIterator(), null, "", false, false);
    }

    @Test
    public void singleKeyLookupFromFileTable() throws IOException {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\t1\tFooFoo",
                "chr2\t2\tB\t2\tFooBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {1, 2};

        performTest(input, output, lookupColumns, outColumns, "", null, createDoubleKeyTestFiles().toString(), "", false, false);
    }

    @Test
    public void singleKeyLookupFromSmallTableTwoOutColumnsWithExtraColumns() {
        String[] input = {
                "chr1\t1\tA\tFoo\tBar",
                "chr2\t2\tB\tBar\tFoo"};
        String[] output = {
                "chr1\t1\tA\tFoo\tBar\t1\tFooFoo",
                "chr2\t2\tB\tBar\tFoo\t2\tFooBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {1, 2};

        performTest(input, output, lookupColumns, outColumns, "1", UTestMapLookup.getDoubleKeyLineIterator(), null, "", false, false);
    }

    @Test
    public void singleKeyLookupWithMissingLookupValue() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB",
                "chr3\t3\tC",
                "chr4\t4\tAAA"};
        String[] output = {
                "chr1\t1\tA\t1\tFooFoo",
                "chr2\t2\tB\t2\tFooBar",
                "chr3\t3\tC\t3\tBarFoo",
                "chr4\t4\tAAA\tmissing"}; // This might be an issue. Gummis 17.5.2019
        int[] lookupColumns = {2};
        int[] outColumns = {1, 2};

        performTest(input, output, lookupColumns, outColumns, "1", UTestMapLookup.getDoubleKeyLineIterator(), null, "missing", true, false);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableCartesianJoinTwoOutputs() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\t1\tFooFoo",
                "chr1\t1\tA\t2\tFooBar",
                "chr1\t1\tA\t3\tBarFoo",
                "chr1\t1\tA\t4\tBarBar",
                "chr2\t2\tB\t1\tFooFoo",
                "chr2\t2\tB\t2\tFooBar",
                "chr2\t2\tB\t3\tBarFoo",
                "chr2\t2\tB\t4\tBarBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {1,2};

        performTest(input, output, lookupColumns, outColumns, "1", UTestMapLookup.getDoubleKeyLineIterator(), null, "missing", true, true);
    }

    private void performTest(String[] input, String[] output, int[] lookupColumns, int[] outColumns, String s, LineIterator doubleKeyLineIterator, String fileName, String missingValue, boolean returnMissing, boolean cartesian) {
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        MultiMapLookup analysis = new MultiMapLookup(sessionFactory.create(), s, doubleKeyLineIterator,
                fileName, lookupColumns, true, outColumns, missingValue, returnMissing, cartesian);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
