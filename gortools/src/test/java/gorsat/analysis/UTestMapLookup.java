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

import org.gorpipe.model.gor.iterators.LineIterator;
import gorsat.Analysis.MapLookup;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class UTestMapLookup {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    public File createSingleKeyTestFiles() throws IOException {
        LineIterator iterator = getSingleKeyLineIterator();
        StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            builder.append(iterator.nextLine()).append("\n");
        }

        File testFile = workDir.newFile("testData.txt");
        FileUtils.writeStringToFile(testFile, builder.toString(), Charset.defaultCharset());
        return testFile;
    }


    static final LineIterator getSingleKeyLineIterator() {
        return new ArrayLineIterator(new String[] {"A\tFooFoo\t1", "B\tFooBar\t2", "C\tBarFoo\t3", "D\tBarBar\t4",});
    }

    static final LineIterator getDoubleKeyLineIterator() {
        return new ArrayLineIterator(new String[] {"A\t1\tFooFoo\t1", "B\t2\tFooBar\t2", "C\t3\tBarFoo\t3", "D\t4\tBarBar\t4",});
    }

    private static final LineIterator getSetLineIterator() {
        return new ArrayLineIterator(new String[] {"A", "B", "C3", "D",});
    }

    @Test
    public void singleKeyLookupFromLineIteratorTable() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\tFooFoo",
                "chr2\t2\tB\tFooBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "1", getSingleKeyLineIterator(), "", false, "e", false, false, false, false);
    }

    @Test
    public void doubleKeyLookupFromLineIteratorTable() {
        String[] input = {
                "chr1\t1\tA\t1",
                "chr2\t2\tB\t2",
                "chr2\t2\tB\t3"};
        String[] output = {
                "chr1\t1\tA\t1\tFooFoo",
                "chr2\t2\tB\t2\tFooBar"};
        int[] lookupColumns = {2,3};
        int[] outColumns = {2};

        performTest(input, output, lookupColumns, outColumns, "1", getDoubleKeyLineIterator(), "", false, "e", false, false, false, false);
    }

    @Test
    public void singleKeyLookupFromFileTable() throws IOException {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\tFooFoo",
                "chr2\t2\tB\tFooBar"};
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "", null, createSingleKeyTestFiles().toString(), false, "e", false, false, false, false);
    }

    @Test
    public void singleKeyLookupFromSmallTableTwoOutColumns() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\tFooFoo\t1",
                "chr2\t2\tB\tFooBar\t2"};
        int[] lookupColumns = {2};
        int[] outColumns = {1,2};

        performTest(input, output, lookupColumns, outColumns, "2", getSingleKeyLineIterator(), "", false, "e", false, false, false, false);
    }

    @Test
    public void singleKeyLookupFromSmallTableTwoOutColumnsWithExtraColumns() {
        String[] input = {
                "chr1\t1\tA\tFoo\tBar",
                "chr2\t2\tB\tBar\tFoo"};
        String[] output = {
                "chr1\t1\tA\tFoo\tBar\tFooFoo\t1",
                "chr2\t2\tB\tBar\tFoo\tFooBar\t2"};
        int[] lookupColumns = {2};
        int[] outColumns = {1,2};

        performTest(input, output, lookupColumns, outColumns, "3", getSingleKeyLineIterator(), "", false, "e", false, false, false, false);
    }

    @Test
    public void singleKeyLookupWithMissingLookupValue() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB",
                "chr3\t3\tC",
                "chr4\t4\tAAA"};
        String[] output = {
                "chr1\t1\tA\tFooFoo\t1",
                "chr2\t2\tB\tFooBar\t2",
                "chr3\t3\tC\tBarFoo\t3",
                "chr4\t4\tAAA\tmissing"}; // This might be an issue. Gummis 17.5.2019
        int[] lookupColumns = {2};
        int[] outColumns = {1,2};

        performTest(input, output, lookupColumns, outColumns, "4", getSingleKeyLineIterator(), "", false, "missing", true, false, false, false);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableCartesianJoinSingleOutput() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\tA",
                "chr1\t1\tA\tB",
                "chr1\t1\tA\tC",
                "chr1\t1\tA\tD",
                "chr2\t2\tB\tA",
                "chr2\t2\tB\tB",
                "chr2\t2\tB\tC",
                "chr2\t2\tB\tD"};
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "1", getSingleKeyLineIterator(), "", false, "e", false, false, false, true);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableCartesianJoinTwoOutputs() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        String[] output = {
                "chr1\t1\tA\tA\tFooFoo\t1",
                "chr1\t1\tA\tB\tFooBar\t2",
                "chr1\t1\tA\tC\tBarFoo\t3",
                "chr1\t1\tA\tD\tBarBar\t4",
                "chr2\t2\tB\tA\tFooFoo\t1",
                "chr2\t2\tB\tB\tFooBar\t2",
                "chr2\t2\tB\tC\tBarFoo\t3",
                "chr2\t2\tB\tD\tBarBar\t4"};
        int[] lookupColumns = {2};
        int[] outColumns = {1,2};

        performTest(input, output, lookupColumns, outColumns, "1", getSingleKeyLineIterator(), "", false, "e", false, false, false, true);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableInSet() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB",
                "chr2\t2\tAAA"};
        String[] output = {
                "chr1\t1\tA",
                "chr2\t2\tB"};
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "1", getSetLineIterator(), "", false, "e", false, true, false, false);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableInSetNegated() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB",
                "chr3\t3\tAAA"};
        String[] output = {
                "chr3\t3\tAAA"};
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "1", getSetLineIterator(), "", true, "e", false, true, false, false);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableInSetAndInsetColumn() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB",
                "chr3\t3\tAAA"};
        String[] output = {
                "chr1\t1\tA\t1",
                "chr2\t2\tB\t1",
                "chr3\t3\tAAA\tmissing"}; // Is this correct?
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "1", getSetLineIterator(), "", false, "missing", false, true, true, false);
    }

    @Test
    public void singleKeyLookupFromLineIteratorTableInSetNegatedAndInsetColumn() {
        String[] input = {
                "chr1\t1\tA",
                "chr2\t2\tB",
                "chr3\t3\tAAA"};
        String[] output = {
                "chr1\t1\tA\t0",
                "chr2\t2\tB\t0",
                "chr3\t3\tAAA\tmissing"}; // Is this correct
        int[] lookupColumns = {2};
        int[] outColumns = {1};

        performTest(input, output, lookupColumns, outColumns, "1", getSetLineIterator(), "", true, "missing", false, true, true, false);
    }

    private void performTest(String[] input, String[] output, int[] lookupColumns, int[] outColumns, String iteratorCommand, LineIterator lineIterator, String fileName, boolean negate, String missingValue, boolean returnMissing, boolean inSet, boolean inSetCol, boolean cartesian) {
        AnalysisTestSessionFactory sessionFactory = new AnalysisTestSessionFactory();
        MapLookup analysis = new MapLookup(sessionFactory.create(), iteratorCommand, lineIterator, fileName,
                lookupColumns, negate, true, outColumns, missingValue, returnMissing, inSet,
                inSetCol, cartesian, false);
        AnalysisTestEngine engine = new AnalysisTestEngine();
        engine.run(analysis, input, output);
    }
}
