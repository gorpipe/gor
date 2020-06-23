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

package org.gorpipe.gor;

import org.gorpipe.gor.function.GorRowFilterFunction;
import org.gorpipe.gor.function.GorRowInferFunction;
import org.gorpipe.gor.function.GorRowMapFunction;
import org.gorpipe.gor.function.GorRowQueryFunction;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class UTestGorPredicates {
    @Test
    public void testGorInfer() throws IOException {
        Path p = Paths.get("../tests/data/gor/genes.gor");
        GorRowInferFunction gorInfer = new GorRowInferFunction();
        Row row = Files.lines(p).skip(1).limit(8).map(RowObj::apply).reduce(gorInfer).orElseThrow(() -> new NoSuchElementException("No value present"));
        Assert.assertEquals("Wrong types","S\tI\tI\tS", row.toString());
    }

    @Test
    public void testGorFilter() throws IOException {
        Path p = Paths.get("../tests/data/gor/genes.gor");
        GorRowInferFunction gorInfer = new GorRowInferFunction();
        String[] header = Files.lines(p).findFirst().orElseThrow(() -> new NoSuchElementException("No value present")).split("\t");
        String[] types = Files.lines(p).skip(1).limit(8).map(RowObj::apply).reduce(gorInfer).orElseThrow(() -> new NoSuchElementException("No value present")).toString().split("\t");
        GorRowFilterFunction<Row> gorFilter = new GorRowFilterFunction<>("Gene_Symbol = 'BRCA2'", header, types);
        Row gene = Files.lines(p).skip(1).map(RowObj::apply).filter(gorFilter).findFirst().orElseThrow(() -> new NoSuchElementException("No value present"));
        Assert.assertEquals("Wrong gene locus", "chr13\t32889610\t32973805\tBRCA2", gene.toString());
    }

    @Test
    public void testGorMap() throws IOException {
        Path p = Paths.get("../tests/data/gor/genes.gor");
        GorRowInferFunction gorInfer = new GorRowInferFunction();
        String[] header = Files.lines(p).findFirst().orElseThrow(() -> new NoSuchElementException("No value present")).split("\t");
        String[] types = Files.lines(p).skip(1).limit(8).map(RowObj::apply).reduce(gorInfer).orElseThrow(() -> new NoSuchElementException("No value present")).toString().split("\t");
        GorRowFilterFunction<Row> gorFilter = new GorRowFilterFunction<>("Gene_Symbol = 'BRCA2'", header, types);
        GorRowMapFunction gorMap = new GorRowMapFunction("if(1>0,1,0)", header, types);
        Row gene = Files.lines(p).skip(1).map(RowObj::apply).filter(gorFilter).map(gorMap).findFirst().orElseThrow(() -> new NoSuchElementException("No value present"));
        Assert.assertEquals("Wrong row generated", "chr13\t32889610\t32973805\tBRCA2\t1", gene.toString());
    }

    @Ignore("Sigmar needs to look at this test - fails with 'Analysis step already finished'")
    @Test
    public void testGorQuery() throws IOException {
        Path p = Paths.get("../tests/data/gor/genes.gor");
        GorRowInferFunction gorInfer = new GorRowInferFunction();
        String header = Files.lines(p).findFirst().orElseThrow(() -> new NoSuchElementException("No value present"));

        String[] hsplit = header.split("\t");
        String[] types = Files.lines(p).skip(1).limit(8).map(RowObj::apply).reduce(gorInfer).orElseThrow(() -> new NoSuchElementException("No value present")).toString().split("\t");

        GorRowFilterFunction<Row> gorFilter = new GorRowFilterFunction<>("Gene_Symbol = 'BRCA2'", hsplit, types);
        GorRowQueryFunction gorQuery = new GorRowQueryFunction("group chrom -count", header);
        Row gene = Stream.concat(Files.lines(p).skip(1).map(RowObj::apply).filter(gorFilter),Stream.of(RowObj.apply("chrZ\t-1"))).flatMap(gorQuery).findFirst().orElseThrow(() -> new NoSuchElementException("No value present"));
        Assert.assertEquals("Wrong result generated", "chr13\t0\t150000000\t1", gene.toString());
    }
}
