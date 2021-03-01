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

package gorsat.parser;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class UTestEvalFunction {

    private static String absoluteResourcePath;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void init() {
        File relativeResourcePath = new File("../tests");
        absoluteResourcePath = relativeResourcePath.getAbsolutePath();
    }

    @Test
    public void testEvalWithGor() {
        String[] args = new String[]{"gorrow chr1,1,2 | calc x eval('gor data/gor/genes.gor')", "-gorroot", absoluteResourcePath};

        StringJoiner joiner = new StringJoiner("\n", "", "\n");
        joiner.add(getDelimitedRow(Arrays.asList("chrom", "bpStart", "bpStop", "x")));
        joiner.add(getDelimitedRow(Arrays.asList("chr1", "1", "2", "14412")));

        String expected = joiner.toString();
        TestUtils.assertGorpipeResults(expected, args);
    }

    @Test
    public void testEvalWithNor() {
        String[] args = new String[]{"gorrow chr1,1,2 | calc x eval('nor data/gor/genes.gor | select gene_end')", "-gorroot", absoluteResourcePath};

        StringJoiner joiner = new StringJoiner("\n", "", "\n");
        joiner.add(getDelimitedRow(Arrays.asList("chrom", "bpStart", "bpStop", "x")));
        joiner.add(getDelimitedRow(Arrays.asList("chr1", "1", "2", "14412")));

        String expected = joiner.toString();
        TestUtils.assertGorpipeResults(expected, args);
    }

    @Test
    public void testEvalFailsWithResourceNotFound_whenProjectRootNotSet() {
        exception.expect(GorDataException.class);
        exception.expectCause(instanceOf(GorResourceException.class));
        exception.expectMessage("Resource not found for iterator");

        String[] args = new String[]{"gorrow chr1,1,2 | calc x eval('nor data/gor/genes.gor | select gene_end')"};

        TestUtils.runGorPipe(args);
    }

    @Test
    public void testEvalFails_whenTopHasInvalidInput() {
        exception.expect(GorDataException.class);
        exception.expectCause(instanceOf(GorParsingException.class));
        exception.expectMessage("Invalid input to TOP: dummy");

        File relativeResourcePath = new File("../tests");
        String absoluteResourcePath = relativeResourcePath.getAbsolutePath();

        String[] args = new String[]{"gorrow chr1,1,2 | calc x eval('nor data/gor/genes.gor | top dummy ')", "-gorroot", absoluteResourcePath};

        TestUtils.runGorPipe(args);
    }

    private String getDelimitedRow(List<String> values) {
        return String.join("\t", values);
    }
}
