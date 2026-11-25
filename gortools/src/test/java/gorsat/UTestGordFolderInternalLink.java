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

import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ENGKNOW-4199: a folder-GORD whose dictionary is a link file ({@code data.gord/thedict.gord.link})
 * must read the same through the bare folder path as through the link file.
 */
public class UTestGordFolderInternalLink {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setupTest() throws IOException {
        workDirPath = workDir.getRoot().toPath();
        Files.copy(Paths.get("../tests/data/gor/dbsnp_test.gor"), workDirPath.resolve("dbsnp.gor"));
    }

    private String run(String query) {
        return TestUtils.runGorPipe(query, "-gorroot", workDirPath.toString());
    }

    @Test
    public void testBareFolderPathFollowsLinkEntryWrittenByWriteLink() {
        Path folder = workDirPath.resolve("data.gord");
        String link = folder.resolve("thedict.gord.link").toString();
        run("pgor dbsnp.gor | write -link " + link + " -linkmeta 'DATA_LOCATION=" + folder
                + "/,DATA_LIFECYCLE_MANAGED=true,ENTRIES_COUNT_MIN=2,ENTRIES_COUNT_MAX=10,ENTRIES_AGE_MIN=86400000'");

        String expected = run("gor dbsnp.gor | top 1000");
        Assert.assertEquals("link file", expected, run("gor " + link + " | top 1000"));
        Assert.assertEquals("bare folder path", expected, run("gor " + folder + " | top 1000"));
        Assert.assertEquals("pgor bare folder path", expected, run("pgor " + folder + " | top 1000"));
    }

    @Test
    public void testBareFolderPathResolvesDataRelativeToLinkedDictionary() throws IOException {
        Path folder = workDirPath.resolve("sub.gord");
        Files.createDirectories(folder.resolve("v1"));
        run("gor dbsnp.gor | write " + folder.resolve("v1").resolve("b1.gorz"));
        Files.writeString(folder.resolve("v1").resolve("thedict.gord"),
                "b1.gorz\t1\tchr1\t0\tchrZ\t1000000000\n");

        var reader = new DriverBackedFileReader("");
        LinkFile.createV1(new FileSource(folder.resolve("thedict.gord.link").toString()), "")
                .appendEntry("v1/thedict.gord", "", null, reader)
                .save(reader);

        String expected = run("gor " + folder.resolve("v1/thedict.gord") + " | top 1000");
        Assert.assertEquals("target dictionary", run("gor dbsnp.gor | calc Source '1' | top 1000"), expected);
        Assert.assertEquals("link file", expected, run("gor " + folder.resolve("thedict.gord.link") + " | top 1000"));
        Assert.assertEquals("bare folder path", expected, run("gor " + folder + " | top 1000"));
    }
}
