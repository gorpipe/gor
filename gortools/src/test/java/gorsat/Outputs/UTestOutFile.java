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

package gorsat.Outputs;

import gorsat.Commands.Output;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.model.RowBase;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class UTestOutFile {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void emptyGorFile() {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        Output output = OutFile.apply(outputFile.getAbsolutePath());
        output.setup();
        output.finish();

        Assert.assertEquals(true, outputFile.exists());
        Assert.assertEquals(0, outputFile.length());
    }

    @Test
    public void emptyTsvFile() {
        File outputFile = new File(workDir.getRoot(), "output.tsv");
        Output output = OutFile.apply(outputFile.getAbsolutePath());
        output.setup();
        output.finish();

        Assert.assertEquals(true, outputFile.exists());
        Assert.assertEquals(0, outputFile.length());
    }

    @Test
    public void emptyGorFileMd5() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        File md5File = new File(workDir.getRoot(), "output.gor.md5");
        Output output = OutFile.apply(outputFile.getAbsolutePath(), null, false, false, true);
        output.setup();
        output.finish();

        Assert.assertEquals(true, outputFile.exists());
        Assert.assertEquals(0, outputFile.length());

        List<String> md5Lines = Files.readAllLines(md5File.toPath());
        Assert.assertEquals(1, md5Lines.size());
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5Lines.get(0));
    }

    @Test
    public void emptyTsvFileMd5() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.tsv");
        File md5File = new File(workDir.getRoot(), "output.tsv.md5");
        Output output = OutFile.apply(outputFile.getAbsolutePath(), null, false, false, true);
        output.setup();
        output.finish();

        Assert.assertEquals(true, outputFile.exists());
        Assert.assertEquals(0, outputFile.length());

        List<String> md5Lines = Files.readAllLines(md5File.toPath());
        Assert.assertEquals(1, md5Lines.size());
        Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5Lines.get(0));
    }

    @Test
    public void emptyGorFileWithHeader() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        final String header = "Chrom\tPos";
        Output output = OutFile.apply(outputFile.getAbsolutePath(), header);
        output.setup();
        output.finish();

        List<String> allLines = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(1, allLines.size());
        Assert.assertEquals(header, allLines.get(0));
    }

    @Test
    public void emptyGorFileWithHeaderMd5() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        File md5File = new File(workDir.getRoot(), "output.gor.md5");
        final String header = "Chrom\tPos";
        Output output = OutFile.apply(outputFile.getAbsolutePath(), header, false, false, true);
        output.setup();
        output.finish();

        List<String> allLines = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(1, allLines.size());
        Assert.assertEquals(header, allLines.get(0));

        List<String> md5Lines = Files.readAllLines(md5File.toPath());
        Assert.assertEquals(1, md5Lines.size());
        Assert.assertEquals("eeada059f302e61b9be88237bc393101", md5Lines.get(0));
    }

    @Test
    public void gorFileNoHeader() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        String rowContents = "chr1\t1\tbingo";
        Output output = OutFile.apply(outputFile.getAbsolutePath());
        output.setup();
        output.process(new RowBase(rowContents));
        output.finish();

        List<String> allLines = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(1, allLines.size());
        Assert.assertEquals(rowContents, allLines.get(0));
    }

    @Test
    public void gorFile() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        final String header = "Chrom\tPos";
        String rowContents = "chr1\t1\tbingo";
        Output output = OutFile.apply(outputFile.getAbsolutePath(), header);
        output.setup();
        output.process(new RowBase(rowContents));
        output.finish();

        List<String> allLines = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(2, allLines.size());
        Assert.assertEquals(header, allLines.get(0));
        Assert.assertEquals(rowContents, allLines.get(1));
    }

    @Test
    public void gorFileMd5() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.gor");
        File md5File = new File(workDir.getRoot(), "output.gor.md5");
        final String header = "Chrom\tPos";
        String rowContents = "chr1\t1\tbingo";
        Output output = OutFile.apply(outputFile.getAbsolutePath(), header, false, false, true);
        output.setup();
        output.process(new RowBase(rowContents));
        output.finish();

        List<String> allLines = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(2, allLines.size());
        Assert.assertEquals(header, allLines.get(0));
        Assert.assertEquals(rowContents, allLines.get(1));

        List<String> md5Lines = Files.readAllLines(md5File.toPath());
        Assert.assertEquals(1, md5Lines.size());
        Assert.assertEquals("8f4475a78f0736b3fe9ab4316b92a4b7", md5Lines.get(0));
    }

    @Test
    public void tsvFileMd5() throws IOException {
        File outputFile = new File(workDir.getRoot(), "output.tsv");
        File md5File = new File(workDir.getRoot(), "output.tsv.md5");
        final String header = "ChromNOR\tPosNOR\tData1\tData2";
        String rowContents = "ChrN\t1\tbingo\tbongo";
        Output output = OutFile.apply(outputFile.getAbsolutePath(), header, false, true, true);
        output.setup();
        output.process(new RowBase(rowContents));
        output.finish();

        List<String> allLines = Files.readAllLines(outputFile.toPath());
        Assert.assertEquals(2, allLines.size());
        Assert.assertEquals("#Data1\tData2", allLines.get(0));
        Assert.assertEquals("bingo\tbongo", allLines.get(1));

        List<String> md5Lines = Files.readAllLines(md5File.toPath());
        Assert.assertEquals(1, md5Lines.size());
        Assert.assertEquals("08750340346460d464415a56abc6cda7", md5Lines.get(0));
    }

    @Test
    public void invalidPathTsvThrowsResourceException() {
        thrown.expect(GorResourceException.class);
        OutFile.apply("/this/path/is/invalid.tsv", "", false, true, true);
    }

    @Test
    public void invalidPathGorThrowsResourceException() {
        thrown.expect(GorResourceException.class);
        OutFile.apply("/this/path/is/invalid.gor", "", false, false, true);
    }

    @Test
    public void invalidPathGorzThrowsResourceException() {
        thrown.expect(GorResourceException.class);
        OutFile.apply("/this/path/is/invalid.gorz", "", false, false, true);
    }
}