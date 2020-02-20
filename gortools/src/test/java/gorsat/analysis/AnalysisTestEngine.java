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

import gorsat.Analysis.AnalysisSink;
import gorsat.Commands.Analysis;
import gorsat.Commands.RowHeader;
import gorsat.Iterators.RowArrayIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RowSource;
import org.junit.Assert;
import scala.collection.Iterator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Class for testing analysis modules. Takes as argument analysis module, input rows and output rows. Input can also be
 * files.
 */
public class AnalysisTestEngine {

    public static final RowHeader ROW_HEADER = RowHeader.apply(new String[] {"Chrom", "Pos", "Column"}, new String[] {"S","I", "S"});

    public void run(Analysis processor, String input, String output) {
        run(processor, input.split("\n", -1), output.split("\n", -1));
    }

    public void run(Analysis processor, String[] inputStrings, String[] outputStrings) {
        List<Row> inputRows = new ArrayList<>();
        List<Row> outputRows = new ArrayList<>();

        for (String inputString : inputStrings) {
            if (!StringUtils.isEmpty(inputString)) {
                inputRows.add(RowObj.apply(inputString));
            }
        }

        for (String outputString : outputStrings ) {
            if (!StringUtils.isEmpty(outputString)) {
                outputRows.add(RowObj.apply(outputString));
            }
        }

        run(processor, inputRows.toArray(new Row[0]), outputRows.toArray(new Row[0]));
    }

    public void run(Analysis processor, Row[] inputRows, Row[] outputRows) {
        run(processor,
                new RowArrayIterator(inputRows, inputRows.length),
                new RowArrayIterator(outputRows, outputRows.length));

    }

    public void run(Analysis processor, File inputFile, File outputFile) {
        try {
            run(processor,
                    FileUtils.readLines(inputFile, Charset.defaultCharset()).toArray(new String[0]),
                    FileUtils.readLines(outputFile, Charset.defaultCharset()).toArray(new String[0]));
        } catch (IOException ioe) {
            throw new GorResourceException("Failed to open file.", ioe.getMessage(), ioe);
        }
    }

    public void run(Analysis processor, RowSource inputIterator, RowSource outputIterator) {
        AnalysisSink sink = new AnalysisSink();

        processor.$bar(sink);

        // For test coverage
        processor.isTypeInformationMaintained();

        // For test coverage
        processor.setRowHeader(ROW_HEADER);

        processor.setup();

        while(inputIterator.hasNext()) {
            processor.process(inputIterator.next());
        }

        processor.finish();

        Iterator<Row> resultIterator = sink.rows().iterator();

        try {
            while (outputIterator.hasNext()) {
                Row compareRow = outputIterator.next();
                Row sourceRow = resultIterator.next();
                Assert.assertEquals("Row mismatch", compareRow.toString(), sourceRow.toString());
            }
        } catch (NoSuchElementException nsee) {
            Assert.fail("Result sets are not of same size.");
        }

        Assert.assertEquals("Result sets are not of same size.", resultIterator.hasNext(), outputIterator.hasNext());
    }
}
