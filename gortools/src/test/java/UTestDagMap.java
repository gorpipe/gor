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

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class UTestDagMap {
    private void assertDagmapQuery(String[] pns, String[] dag, String dagmapOptions, String[] expected) {
        File file1 = TestUtils.createTsvFile("UTestDagMap", pns);
        File file2 = TestUtils.createTsvFile("UTestDagMap", dag);
        String query = String.format("nor %s | dagmap %s %s", file1.getAbsolutePath(), dagmapOptions, file2.getAbsolutePath());
        String[] lines = TestUtils.runGorPipeWithCleanup(query, new File[]{file1, file2});

        Assert.assertArrayEquals(expected, lines);
    }

    @Test
    public void dagmapAllUpperCase() {
        String[] pns = {"CHILD"};
        String[] dag = {
                "CHILD\tFATHER",
                "CHILD\tMOTHER"
        };
        String dagmapOptions = "-c col1";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n",
                "chrN\t0\tCHILD\tCHILD\t0\n",
                "chrN\t0\tCHILD\tMOTHER\t1\n",
                "chrN\t0\tCHILD\tFATHER\t1\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapAllLowerCase() {
        String[] pns = {"child"};
        String[] dag = {
                "child\tfather",
                "child\tmother"
        };
        String dagmapOptions = "-c col1";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n",
                "chrN\t0\tchild\tchild\t0\n",
                "chrN\t0\tchild\tmother\t1\n",
                "chrN\t0\tchild\tfather\t1\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapChildLowerCaseParentUpperCase() {
        String[] pns = {"child"};
        String[] dag = {
                "child\tFATHER",
                "child\tMOTHER"
        };
        String dagmapOptions = "-c col1";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n",
                "chrN\t0\tchild\tchild\t0\n",
                "chrN\t0\tchild\tMOTHER\t1\n",
                "chrN\t0\tchild\tFATHER\t1\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapInconsistentCase() {
        String[] pns = {"Child"};
        String[] dag = {
                "child\tFATHER",
                "CHILD\tmother"
        };
        String dagmapOptions = "-c col1";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapInconsistentCaseWithCisOption() {
        String[] pns = {"Child"};
        String[] dag = {
                "child\tFATHER",
                "CHILD\tmother"
        };
        String dagmapOptions = "-c col1 -cis";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n",
                "chrN\t0\tChild\tCHILD\t0\n",
                "chrN\t0\tChild\tmother\t1\n",
                "chrN\t0\tChild\tFATHER\t1\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapUnsuccessfulLookup() {
        String[] pns = {"child", "sibling"};
        String[] dag = {
                "child\tfather",
                "child\tmother"
        };
        String dagmapOptions = "-c col1";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n",
                "chrN\t0\tchild\tchild\t0\n",
                "chrN\t0\tchild\tmother\t1\n",
                "chrN\t0\tchild\tfather\t1\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapUnsuccessfulLookupWithMissing() {
        String[] pns = {"child", "sibling"};
        String[] dag = {
                "child\tfather",
                "child\tmother"
        };
        String dagmapOptions = "-c col1 -m missing";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\n",
                "chrN\t0\tchild\tchild\t0\n",
                "chrN\t0\tchild\tmother\t1\n",
                "chrN\t0\tchild\tfather\t1\n",
                "chrN\t0\tsibling\tmissing\t-1\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapDagPath() {
        String[] pns = {"child"};
        String[] dag = {
                "child\tfather",
                "child\tmother"
        };
        String dagmapOptions = "-c col1 -dp";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\tDAG_path\n",
                "chrN\t0\tchild\tchild\t0\tchild\n",
                "chrN\t0\tchild\tmother\t1\tchild->mother\n",
                "chrN\t0\tchild\tfather\t1\tchild->father\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapDagPathWithCustomSeparator() {
        String[] pns = {"child"};
        String[] dag = {
                "child\tfather",
                "child\tmother"
        };
        String dagmapOptions = "-c col1 -dp -ps _";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\tDAG_path\n",
                "chrN\t0\tchild\tchild\t0\tchild\n",
                "chrN\t0\tchild\tmother\t1\tchild_mother\n",
                "chrN\t0\tchild\tfather\t1\tchild_father\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }

    @Test
    public void dagmapUnsuccessfulLookupWithMissingAndDagPath() {
        String[] pns = {"child", "sibling"};
        String[] dag = {
                "child\tfather",
                "child\tmother"
        };
        String dagmapOptions = "-c col1 -m missing -dp";
        String[] expected = {
                "ChromNOR\tPosNOR\tcol1\tDAG_node\tDAG_dist\tDAG_path\n",
                "chrN\t0\tchild\tchild\t0\tchild\n",
                "chrN\t0\tchild\tmother\t1\tchild->mother\n",
                "chrN\t0\tchild\tfather\t1\tchild->father\n",
                "chrN\t0\tsibling\tmissing\t-1\tmissing\n"
        };

        assertDagmapQuery(pns, dag, dagmapOptions, expected);
    }
}
