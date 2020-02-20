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

package gorsat;

import org.gorpipe.test.SlowTests;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class UTestBenchmarkSparseDenseJoin {

    static private final Logger log = LoggerFactory.getLogger(UTestBenchmarkSparseDenseJoin.class);

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    @Ignore
    @Category(SlowTests.class)
    public void runQuery() { //Double fraction) {
        try {
            String fake_table = createFakeTable(1000000);

            String sparseToDenseJoin = "def sparse = " + fake_table + "| where random() < %f ;\n"
                    + "def dense = " + fake_table + ";\n"
                    + "gor sparse\n"
                    + "| varjoin -i <(gor dense )\n"
                    + "| group genome -count\n"
                    + "| calc t time()";

            String denseToSparseJoin = "def sparse = " + fake_table + "| where random() < %f ;\n"
                    + "def dense = " + fake_table + ";\n"
                    + "gor dense\n"
                    + "| varjoin -i <(gor sparse )\n"
                    + "| group genome -count\n"
                    + "| calc t time()";


            Double[] fractions = new Double[]{0.1, 0.01, 0.001, 0.0001, 0.00001};

            log.info(sparseToDenseJoin);
            log.info("Fraction\truntime_milliseconds\titerations");
            for (Double fraction : fractions) {
                execute(sparseToDenseJoin, fraction, 10);
            }

            log.info(denseToSparseJoin);
            log.info("Fraction\truntime_milliseconds\titerations");
            for (Double fraction : fractions) {
                execute(denseToSparseJoin, fraction, 10);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Should also test tables that have heavier rows
    public String createFakeTable(int rowCount) throws IOException {
        File tableFile = workDir.newFile("SNP_TABLE.gor");
        FileWriter writer = new FileWriter(tableFile);
        BufferedWriter buf = new BufferedWriter(writer);
        buf.write("#chrom\tpos\treference\tcall\n");
        for (int i = 1; i <= rowCount; i++) {
            buf.write("chr1\t" + i + "\tA\tT\n");
        }
        buf.close();
        writer.close();
        return tableFile.getAbsolutePath();
    }

    public void execute(String query, Double fraction, int iterations) {
        ArrayList<Integer> runtimes = new ArrayList<Integer>();
        //Create list of runtimes
        for (int i = 0; i < iterations; i++) {
            Arrays.stream(TestUtils.runGorPipe(String.format(query, fraction)).split("\n"))
                    .map(x -> x.split("\\t")[4])
                    .skip(1) //header
                    .forEach(x -> runtimes.add(Integer.valueOf(x)));
        }

        //Average of runtimes
        runtimes.stream()
                .map(x -> new Tuple2<Integer, Integer>(x, 1))
                .reduce(
                        (acc, y) -> new Tuple2<Integer, Integer>(acc._1 + y._1, acc._2 + y._2)
                )
                .map(x -> x._1 / x._2.floatValue())
                .ifPresent(x -> log.info(fraction + "\t" + x + "\t" + iterations));
    }
}



