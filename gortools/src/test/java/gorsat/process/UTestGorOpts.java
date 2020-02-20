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

package gorsat.process;

import org.gorpipe.test.utils.FileTestUtils;
import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class UTestGorOpts {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private File gorAliasFile;
    private File gorScriptFile;

    @Before
    public void setUpTest() throws IOException {
        String genesPath = Paths.get("../tests/data/gor/genes.gorz").toFile().getCanonicalPath();
        gorAliasFile = FileTestUtils.createTempFile(workDir.getRoot(), "gor_alias_file.txt", "#genes#\t" + genesPath + "\tEnsembl 68 gene list with only one entry per GENE_SYMBOL,chrom\n");
        gorScriptFile = FileTestUtils.createTempFile(workDir.getRoot(), "gor_script.txt", "/* test file for script and aliases opts */\n" + "gor #genes#");
    }

    @Test
    public void testAliasScriptOpts() {
        String[] args = new String[]{"-aliases", gorAliasFile.getPath(), "-script", gorScriptFile.getPath()};
        String results = TestUtils.runGorPipe(args);
        Assert.assertTrue(results.startsWith(
                "Chrom\tgene_start\tgene_end\tGene_Symbol\n" +
                        "chr1\t11868\t14412\tDDX11L1\n" +
                        "chr1\t14362\t29806\tWASH7P"));
    }
}