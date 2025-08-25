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

package org.gorpipe.gorshell;

import org.gorpipe.gorshell.HelpCmd;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class UTestHelpCmd {

    private static String[] INPUT = { "->COMMANDS",
            "Try HELP COMMANDS, HELP FUNCTIONS or HELP followed by some of these commands:",
            "",
            "ADJUST, ANNO, ATMAX, ATMIN, BAMFLAG, BASES, BINARYWRITE, BUCKETSPLIT, BUG, CALC, CALCIFMISSING, CIGARSEGS, CMD, COLNUM, COLS2LIST,",
            "COLSPLIT, COLTYPE, COLUMNSORT, CREATE, CSVCC, CSVSEL, DAGMAP, DEF, DISTINCT, DISTLOC, FORMULAS, GAVA, GOR, GORCMD, GORIF, GORROW,",
            "GORROWS, GORSQL, GRANNO, GREP, GROUP, GTGEN, GTLD, HIDE, INSET, JOIN, LDANNO, LEFTJOIN, LEFTWHERE, LIFTOVER, LOG, MAP, MERGE,",
            "MULTIMAP, NOR, NORCMD, NORIF, NORROWS, NORSQL, PARALLEL, PARTGOR, PEDPIVOT, PGOR, PILEUP, PIPESTEPS, PIVOT, PLINKREGRESSION, PREFIX,",
            "RANK, REGRESSION, REGSEL, RENAME, REPLACE, ROWNUM, SDL, SED, SEGHIST, SEGPROJ, SEGSPAN, SELECT, SEQ, SETCOLTYPE, SIGNATURE, SKIP,",
            "SORT, SPLIT, SQL, TEE, THROWIF, TOGOR, TOP, TRYHIDE, TRYSELECT, TRYWHERE, TSVAPPEND, UNPIVOT, UNTIL, VARGROUP, VARIANTS, VARJOIN, VARMERGE,",
            "VARNORM, VERIFYCOLTYPE, VERIFYORDER, WAIT, WHERE, WRITE",
            "",
            "->ADJUST",
            "",
            "The **ADJUST** command is used to get adjusted p-values for the",
            "results of multiple hypothesis tests.",
            "",
            "",
            "Usage",
            "",
            "   gor ... | ADJUST  [-pc p-value-col] [-zc z-stats-col] [-cc chi-stats-col] [-gc grouping cols] [-gcc] [-bonf] [-holm] [-ss] [-sd] [-bh] [-by]",
            "",
            "",
            "Options",
            "",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-pc p-value-col\"       | The column containing the p-values.                                                                  |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-gc grouping cols\"     | The columns to group on.                                                                             |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-gcc\"                  | To run genomic control correction.                                                                   |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-qq\"                   | To get the x-coordinates for a qq plot.                                                              |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-bonf\"                 | To run Bonferroni correction.                                                                        |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-holm\"                 | To run Bonferroni-Holm correction.                                                                   |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-ss\"                   | To run Sidak single step correction.                                                                 |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-sd\"                   | To run Sidak step down correction.                                                                   |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-bh\"                   | To run Benjamini-Hochberg correction.                                                                |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "| \"-by\"                   | To run Benjamini-Yekutieli correction.                                                               |",
            "+-------------------------+------------------------------------------------------------------------------------------------------+",
            "",
            "",
            "->ANNO",
            "",
            "The ANNO command left-joins the stream with all the annotation files",
            "based on chrom and position. ANNO is a deprecated command that is",
            "equivalent to a JOIN with \"-snpsnp\", \"-l\" and \"-t\" options. That",
            "method should be used instead.",
            "",
            "The ANNO command was created to make annotations based on multiple",
            "files with SNP data easy. When the join is not only based on a single",
            "nucleotide location it is less useful, e.g. for Ref/Alt based",
            "variations.",
            "",
            "",
            "Usage",
            "",
            "   gor ... | ANNO file1.gor [ ... [ fileN.gor ] ... ]  [ attributes ]",
            "",
            "",
            "Options",
            "",
            "+-------------------+----------------------------------------------------------------------------------------------+",
            "| \"-h\"              | Eliminate the header from the output.                                                        |",
            "+-------------------+----------------------------------------------------------------------------------------------+",
            "| \"-e char\"         | Character to denote empty field.  Defaults to an empty string, i.e. string of length 0.      |",
            "+-------------------+----------------------------------------------------------------------------------------------+",
            "",
            "",
            "Examples",
            "",
            "These two queries are equivalent:",
            "",
            "   gor left.gorz | anno right1.gorz right2.gorz",
            "",
            "and the following",
            "",
            "   gor left.gorz | join -snpsnp -l -t right1.gorz | join -snpsnp -l -t right2.gorz",
            "",
            ""
    };


    @Test
    public void listToMap() {
        Map<String, String> map = new HashMap<>();
        HelpCmd.listToMap(INPUT, map);

        Assert.assertEquals(3, map.size());
    }
}