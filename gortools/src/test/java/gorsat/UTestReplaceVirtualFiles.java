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

import gorsat.Script.VirtualFileManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 06/01/16.
 */
public class UTestReplaceVirtualFiles {

    @Test
    public void testReplaceVirtualFilesContainingFileNotReferenced() {
        String query = "create dummy = gor ../tests/data/external/samtools/serialization_test.bam; create xxx = gor ../tests/data/external/samtools/serialization_test.bam | signature -timeres 1; gor [xxx]";
        int count = TestUtils.runGorPipeCount(query);
        Assert.assertEquals(1, count);
    }

    @Test
    public void testJavaReplaceVirtualFiles() {
        String cmd = "gor [gorgrid: Query_1]";
        String[] equivTagTempFiles = {"[gorgrid: Query_1]\tcache/object_cache/cache01/e51/e517462dc6d3969f6ca0a14cff03124d.gorz"};
        String[] virtualFileTagEquivTag = {"[gorgrid: Query_1]\t[gorgrid: Query_1]\t"};
        String gorCmdRes = MacroUtilities.javaReplaceVirtualFiles(cmd, equivTagTempFiles, virtualFileTagEquivTag);
        Assert.assertEquals("gor cache/object_cache/cache01/e51/e517462dc6d3969f6ca0a14cff03124d.gorz", gorCmdRes);

        //Test if Virtual Relation Manager gives same results
        VirtualFileManager virtualFileManager = new VirtualFileManager();

        for (String virtualRelationString : equivTagTempFiles) {
            String[] virtualRelation = virtualRelationString.split("\t");
            virtualFileManager.add(virtualRelation[0]);
            virtualFileManager.updateCreatedFile(virtualRelation[0], virtualRelation[1]);
        }
        String gorCmdRes2 = virtualFileManager.replaceVirtualFiles(cmd);

        Assert.assertEquals(gorCmdRes, gorCmdRes2);
    }

    @Test
    public void testJavaReplaceVirtualFilesQuotes() {
        String cmd = "gor [gorgrid: 'Query_1']";
        String[] equivTagTempFiles = {"[gorgrid: 'Query_1']\tcache/object_cache/cache01/e51/e517462dc6d3969f6ca0a14cff03124d.gorz"};
        String[] virtualFileTagEquivTag = {"[gorgrid: 'Query_1']\t[gorgrid: 'Query_1']"};
        String gorCmdRes = MacroUtilities.javaReplaceVirtualFiles(cmd, equivTagTempFiles, virtualFileTagEquivTag);
        Assert.assertEquals("gor cache/object_cache/cache01/e51/e517462dc6d3969f6ca0a14cff03124d.gorz", gorCmdRes);

        //Test if Virtual Relation Manager gives same results
        VirtualFileManager virtualFileManager = new VirtualFileManager();

        for (String virtualRelationString : equivTagTempFiles) {
            String[] virtualRelation = virtualRelationString.split("\t");
            virtualFileManager.add(virtualRelation[0]);
            virtualFileManager.updateCreatedFile(virtualRelation[0], virtualRelation[1]);
        }
        String gorCmdRes2 = virtualFileManager.replaceVirtualFiles(cmd);

        Assert.assertEquals(gorCmdRes, gorCmdRes2);
    }
}
