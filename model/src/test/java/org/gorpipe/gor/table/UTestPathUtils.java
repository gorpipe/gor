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

package org.gorpipe.gor.table;

import org.gorpipe.gor.table.util.PathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Unit tests for gor table logs.
 * <p>
 */
public class UTestPathUtils {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testRelativizePath() {
        Assert.assertEquals(null, PathUtils.relativize(Paths.get("a/b/c"), null));

        Assert.assertEquals(Paths.get("d"), PathUtils.relativize(Paths.get("a/b/c"), Paths.get("a/b/c/d")));
        Assert.assertEquals(Paths.get("d"), PathUtils.relativize(Paths.get("/a/b/c"), Paths.get("/a/b/c/d")));
        Assert.assertEquals(Paths.get("d"), PathUtils.relativize(Paths.get("/a/b/c"), Paths.get("d")));

        Assert.assertEquals(Paths.get("/e/f/g"), PathUtils.relativize(Paths.get("/a/b/c"), Paths.get("/e/f/g")));
        Assert.assertEquals(Paths.get("b/c/f"), PathUtils.relativize(Paths.get("/a/b/c"), Paths.get("b/c/f")));

        Assert.assertEquals(Paths.get("d/x.gor"), PathUtils.relativize(Paths.get("a/b/c/y.gord").getParent(), Paths.get("a/b/c/d/x.gor")));
    }

    @Test
    public void testRelativizeURI() {
        Assert.assertEquals(null, PathUtils.relativize(URI.create("a/b/c"), (URI)null));
        
        Assert.assertEquals("d", PathUtils.relativize(URI.create("a/b/c"), "a/b/c/d"));
        Assert.assertEquals("d", PathUtils.relativize(URI.create("/a/b/c"), "/a/b/c/d"));
        Assert.assertEquals("d", PathUtils.relativize(URI.create("/a/b/c"), "d"));

        Assert.assertEquals("/e/f/g", PathUtils.relativize(URI.create("/a/b/c"), "/e/f/g"));
        Assert.assertEquals("b/c/f", PathUtils.relativize(URI.create("/a/b/c"), "b/c/f"));

        Assert.assertEquals("d/x.gor", PathUtils.relativize(URI.create(Paths.get("a/b/c/y.gord").getParent().toString()), "a/b/c/d/x.gor"));
    }

    @Test
    public void testRelativizeString() {
        Assert.assertEquals(null, PathUtils.relativize("a/b/c", null));

        Assert.assertEquals("a/b/c", PathUtils.relativize((String)null, "a/b/c"));

        Assert.assertEquals("/a/b/c", PathUtils.relativize("", "/a/b/c"));

        Assert.assertEquals("d", PathUtils.relativize("a/b/c", "a/b/c/d"));
        Assert.assertEquals("d", PathUtils.relativize("/a/b/c", "/a/b/c/d"));
        Assert.assertEquals("d", PathUtils.relativize("/a/b/c", "d"));

        Assert.assertEquals("d", PathUtils.relativize("a/b/c/", "a/b/c/d"));
        Assert.assertEquals("d", PathUtils.relativize("/a/b/c/", "/a/b/c/d"));
        Assert.assertEquals("d", PathUtils.relativize("/a/b/c/", "d"));

        Assert.assertEquals("a/b/c/d", PathUtils.relativize("/", "/a/b/c/d"));

        Assert.assertEquals("/e/f/g", PathUtils.relativize("/a/b/c", "/e/f/g"));
        Assert.assertEquals("b/c/f", PathUtils.relativize("/a/b/c", "b/c/f"));

        Assert.assertEquals("d/x.gor", PathUtils.relativize(Path.of("a/b/c/y.gord").getParent().toString(), "a/b/c/d/x.gor"));
    }
}
