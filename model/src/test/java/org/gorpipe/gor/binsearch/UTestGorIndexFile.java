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

package org.gorpipe.gor.binsearch;

import org.apache.commons.io.FileUtils;
import org.gorpipe.gor.binsearch.GorIndexFile;
import org.gorpipe.gor.binsearch.GorIndexType;
import org.gorpipe.gor.binsearch.PositionCache;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class UTestGorIndexFile {
    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Test
    public void empty() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.FULLINDEX)) {
        }

        Assert.assertTrue(path.toFile().exists());

        String contents = FileUtils.readFileToString(path.toFile(), "utf8");
        Assert.assertEquals("## fileformat=GORIv2\n", contents);
    }

    @Test
    public void singlePos() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.FULLINDEX)) {
            file.putFilePosition("chr1", 1, 100);
        }

        String contents = FileUtils.readFileToString(path.toFile(), "utf8");
        String expected = "## fileformat=GORIv2\n" +
                "chr1\t1\t100\n";
        Assert.assertEquals(expected, contents);
    }

    @Test
    public void multiplePosFullIndex() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.FULLINDEX)) {
            file.putFilePosition("chr1", 1, 100);
            file.putFilePosition("chr1", 2, 200);
            file.putFilePosition("chr1", 3, 300);
            file.putFilePosition("chr1", 4, 400);
        }

        String contents = FileUtils.readFileToString(path.toFile(), "utf8");
        String expected = "## fileformat=GORIv2\n" +
                "chr1\t1\t100\n" +
                "chr1\t2\t200\n" +
                "chr1\t3\t300\n" +
                "chr1\t4\t400\n";
        Assert.assertEquals(expected, contents);
    }

    @Test
    public void multiplePosChromIndex() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.CHROMINDEX)) {
            file.putFilePosition("chr1", 1, 100);
            file.putFilePosition("chr1", 2, 200);
            file.putFilePosition("chr1", 3, 300);
            file.putFilePosition("chr1", 4, 400);
        }

        String contents = FileUtils.readFileToString(path.toFile(), "utf8");
        String expected = "## fileformat=GORIv2\n" +
                "chr1\t1\t100\n";
        Assert.assertEquals(expected, contents);
    }

    @Test
    public void load() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.CHROMINDEX)) {
            file.putFilePosition("chr1", 1, 100);
            file.putFilePosition("chr10", 2, 200);
            file.putFilePosition("chr11", 3, 300);
            file.putFilePosition("chr12", 4, 400);
        }
        final PositionCache pc = new PositionCache(0, 5,  Integer.MAX_VALUE);

        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            GorIndexFile.load(inputStream, pc);
        }
        Assert.assertEquals(4, pc.getSize());
    }

    @Test
    public void generateFromGorz() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.CHROMINDEX)) {
            file.generateForGorz("../tests/data/gor/genes.gorz");
        }

        String expected = "## fileformat=GORIv2\n" +
                "chr1\t36921318\t16108\n" +
                "chr1\t249230779\t74190\n" +
                "chr10\t71211228\t89394\n" +
                "chr10\t135515570\t106042\n" +
                "chr11\t48981479\t121618\n" +
                "chr11\t134931674\t151552\n" +
                "chr12\t51454989\t166750\n" +
                "chr12\t133794897\t190993\n" +
                "chr13\t100426278\t206515\n" +
                "chr13\t115095033\t209232\n" +
                "chr14\t63589750\t223703\n" +
                "chr14\t107287769\t239579\n" +
                "chr15\t60973767\t253881\n" +
                "chr15\t102516757\t268032\n" +
                "chr16\t31720433\t283349\n" +
                "chr16\t90252404\t301069\n" +
                "chr17\t28531335\t316154\n" +
                "chr17\t81174665\t341452\n" +
                "chr18\t75334507\t356572\n" +
                "chr18\t77941562\t357193\n" +
                "chr19\t20285821\t372742\n" +
                "chr19\t59105262\t398105\n" +
                "chr2\t74361598\t413982\n" +
                "chr2\t243064437\t451909\n" +
                "chr20\t56063447\t467846\n" +
                "chr20\t62921737\t470705\n" +
                "chr21\t48110675\t480845\n" +
                "chr22\t43912133\t495946\n" +
                "chr22\t51205928\t498329\n" +
                "chr3\t73076958\t514488\n" +
                "chr3\t197955064\t540042\n" +
                "chr4\t77679706\t555706\n" +
                "chr4\t191011859\t575619\n" +
                "chr5\t77296348\t590971\n" +
                "chr5\t180898963\t615152\n" +
                "chr6\t42896937\t630952\n" +
                "chr6\t171054605\t655335\n" +
                "chr7\t65338253\t670959\n" +
                "chr7\t159024124\t694060\n" +
                "chr8\t61544067\t709467\n" +
                "chr8\t146277763\t727269\n" +
                "chr9\t78194584\t742357\n" +
                "chr9\t141130158\t760644\n" +
                "chrM\t15955\t761039\n" +
                "chrX\t73352838\t777046\n" +
                "chrX\t155255322\t794741\n" +
                "chrY\t59001390\t801787\n";

        String contents = FileUtils.readFileToString(path.toFile(), "utf8");
        Assert.assertEquals(expected, contents);
    }

    @Test
    public void generateForGorzFullIndex() throws IOException {
        Path path = workDir.getRoot().toPath().resolve("test.gori");
        try (GorIndexFile file = new GorIndexFile(path.toFile(), GorIndexType.FULLINDEX)) {
            file.generateForGorz("../tests/data/gor/genes.gorz");
        }

        String expected = "## fileformat=GORIv2\n" +
                "chr1\t36921318\t16108\n" +
                "chr1\t96719624\t31818\n" +
                "chr1\t156030950\t46614\n" +
                "chr1\t205594610\t61670\n" +
                "chr1\t249230779\t74190\n" +
                "chr10\t71211228\t89394\n" +
                "chr10\t128798540\t104385\n" +
                "chr10\t135515570\t106042\n" +
                "chr11\t48981479\t121618\n" +
                "chr11\t75921999\t136242\n" +
                "chr11\t130981829\t150776\n" +
                "chr11\t134931674\t151552\n" +
                "chr12\t51454989\t166750\n" +
                "chr12\t104324202\t181650\n" +
                "chr12\t133794897\t190993\n" +
                "chr13\t100426278\t206515\n" +
                "chr13\t115095033\t209232\n" +
                "chr14\t63589750\t223703\n" +
                "chr14\t106573232\t238027\n" +
                "chr14\t107287769\t239579\n" +
                "chr15\t60973767\t253881\n" +
                "chr15\t102516757\t268032\n" +
                "chr16\t31720433\t283349\n" +
                "chr16\t82068608\t297680\n" +
                "chr16\t90252404\t301069\n" +
                "chr17\t28531335\t316154\n" +
                "chr17\t56634038\t330919\n" +
                "chr17\t81174665\t341452\n" +
                "chr18\t75334507\t356572\n" +
                "chr18\t77941562\t357193\n" +
                "chr19\t20285821\t372742\n" +
                "chr19\t49133286\t387641\n" +
                "chr19\t59105262\t398105\n" +
                "chr2\t74361598\t413982\n" +
                "chr2\t132480825\t428750\n" +
                "chr2\t213775726\t443625\n" +
                "chr2\t243064437\t451909\n" +
                "chr20\t56063447\t467846\n" +
                "chr20\t62921737\t470705\n" +
                "chr21\t48110675\t480845\n" +
                "chr22\t43912133\t495946\n" +
                "chr22\t51205928\t498329\n" +
                "chr3\t73076958\t514488\n" +
                "chr3\t151451703\t529605\n" +
                "chr3\t197955064\t540042\n" +
                "chr4\t77679706\t555706\n" +
                "chr4\t165864441\t570952\n" +
                "chr4\t191011859\t575619\n" +
                "chr5\t77296348\t590971\n" +
                "chr5\t145965661\t605818\n" +
                "chr5\t180898963\t615152\n" +
                "chr6\t42896937\t630952\n" +
                "chr6\t132880162\t646584\n" +
                "chr6\t171054605\t655335\n" +
                "chr7\t65338253\t670959\n" +
                "chr7\t131537438\t686312\n" +
                "chr7\t159024124\t694060\n" +
                "chr8\t61544067\t709467\n" +
                "chr8\t142469174\t724721\n" +
                "chr8\t146277763\t727269\n" +
                "chr9\t78194584\t742357\n" +
                "chr9\t135973106\t757419\n" +
                "chr9\t141130158\t760644\n" +
                "chrM\t15955\t761039\n" +
                "chrX\t73352838\t777046\n" +
                "chrX\t151889194\t792370\n" +
                "chrX\t155255322\t794741\n" +
                "chrY\t59001390\t801787\n";

        String contents = FileUtils.readFileToString(path.toFile(), "utf8");
        Assert.assertEquals(expected, contents);
    }
}