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

import gorsat.Script.VirtualFileEntry;
import gorsat.Script.VirtualFileManager;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

public class UTestVirtualFileManager {
    @Test
    public void addingAndRetrievingVirtualFileEntries() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("xxx");

        Assert.assertTrue(manager.get("xxx") != null);
        Assert.assertTrue(manager.get("[xxx]") != null);
        Assert.assertTrue(manager.get("[ xxx ]") != null);
    }

    @Test
    public void addingAndRetrievingVirtualFileEntriesWithVirtualPlaceholder() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("[yyy]");

        Assert.assertTrue(manager.get("yyy") != null);
        Assert.assertTrue(manager.get("[yyy]") != null);
        Assert.assertTrue(manager.get("[ yyy ]") != null);
    }

    @Test
    public void addingAndRetrievingVirtualFileEntriesWithVirtualPlaceholderAndWhitespaces() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("[   vvv  ]");

        Assert.assertTrue(manager.get("vvv") != null);
        Assert.assertTrue(manager.get("[vvv]") != null);
        Assert.assertTrue(manager.get("[ vvv ]") != null);
    }

    @Test
    public void addingMultipleEntriesOfSameVirtualEntry() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("xxx");
        manager.add("yyy");

        Assert.assertEquals(2, manager.size());

        manager.add("[xxx]");
        Assert.assertEquals(2, manager.size());

        manager.add("[ xxx   ]");
        Assert.assertEquals(2, manager.size());
    }

    @Test
    public void getUnusedVirtualFileEntries() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("foo");
        manager.add("bar");
        manager.setAllAsOriginal();
        Assert.assertEquals(2, manager.getUnusedVirtualFileEntries().length);

        manager.updateCreatedFile("foo", "foo_file");
        Assert.assertEquals(1, manager.getUnusedVirtualFileEntries().length);

        manager.updateCreatedFile("bar", "bar_file");
        Assert.assertEquals(0, manager.getUnusedVirtualFileEntries().length);
    }

    @Test
    public void addingAndRetrievingEntries() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("foo");

        Assert.assertEquals(1, manager.size());
        VirtualFileEntry entry = manager.get("foo");
        Assert.assertEquals("[foo]", entry.name);
        Assert.assertFalse(entry.isExternal);
        Assert.assertNull(entry.fileName);
        Assert.assertFalse(entry.isOriginal);

        manager.updateCreatedFile("foo", "foo_file");
        manager.setAllAsOriginal();
        entry = manager.get("foo");
        Assert.assertEquals("[foo]", entry.name);
        Assert.assertFalse(entry.isExternal);
        Assert.assertEquals("foo_file", entry.fileName);
        Assert.assertTrue(entry.isOriginal);
    }

    @Test
    public void addExternalVirtualReference() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("file:foo");
        Assert.assertTrue(manager.get("file:foo").isExternal);

        manager.add("grid:bar");
        Assert.assertTrue(manager.get("grid:bar").isExternal);

        manager.add("gorgrid:bar");
        Assert.assertTrue(manager.get("gorgrid:bar").isExternal);
    }

    @Test
    public void urlLikeExternalVirtualReferenceMapping() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("file://foo");
        Assert.assertFalse(manager.get("file://foo").isExternal);

        manager.add("grid:/bar");
        Assert.assertTrue(manager.get("grid:/bar").isExternal);

        manager.add("gorgrid:s3://bar");
        Assert.assertTrue(manager.get("gorgrid:s3://bar").isExternal);
    }

    @Test
    public void updateCreatedVirtualFiles() {
        VirtualFileManager manager = new VirtualFileManager();

        manager.add("foo");
        manager.add("bar");
        manager.add("xxx");
        manager.add("yyy");
        Assert.assertEquals(0, manager.getCreatedFiles().size());

        manager.updateCreatedFile("foo", "foo_file.txt");
        Assert.assertEquals(1, manager.getCreatedFiles().size());
        Assert.assertEquals("foo_file.txt", manager.get("foo").fileName);

        try {
            manager.updateCreatedFile("foo", "");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof GorParsingException);
        }

        try {
            manager.updateCreatedFile("foo", null);
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof GorParsingException);
        }

        manager.updateCreatedFile("foo", "foo_file2.txt");
        Assert.assertEquals(1, manager.getCreatedFiles().size());
        Assert.assertEquals("foo_file2.txt", manager.get("foo").fileName);

        manager.updateCreatedFile("bar", "bar_file.txt");
        manager.updateCreatedFile("xxx", "xxx_file.txt");
        manager.updateCreatedFile("yyy", "yyy_file.txt");
        Assert.assertEquals(4, manager.getCreatedFiles().size());
    }

    @Test
    public void testIfDependenciesAreAvailable() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("[ foo]");
        manager.add("[bar]");
        manager.add("xxx");
        manager.add("yyy");

        String[] dependencies = {"foo", "bar"};
        Assert.assertFalse(manager.areDependenciesReady(dependencies));

        manager.updateCreatedFile("foo", "foo_file.txt");
        Assert.assertFalse(manager.areDependenciesReady(dependencies));

        manager.updateCreatedFile("bar", "bar_file.txt");
        Assert.assertTrue(manager.areDependenciesReady(dependencies));

        String[] dependencies2 = {"[foo]", "[  bar ]"};
        Assert.assertTrue(manager.areDependenciesReady(dependencies2));

        String[] dependencies3 = {"[foo]", "[  bar ]", "hhh"};
        Assert.assertFalse(manager.areDependenciesReady(dependencies3));

        String[] dependencies4 = {"[xxx]", "[  yyy ]"};
        Assert.assertFalse(manager.areDependenciesReady(dependencies4));
    }

    @Test
    public void virtualFileReplacement() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("[ foo]");
        manager.add("[bar]");
        manager.add("xxx");
        manager.add("yyy");

        final String baseQuery = "gor [foo] | join [  bar]";

        manager.updateCreatedFile("foo", "foo_file.txt");
        manager.updateCreatedFile("bar", "bar_file.txt");

        String query = manager.replaceVirtualFiles(baseQuery);
        Assert.assertEquals("gor foo_file.txt | join bar_file.txt", query);

        final String baseQuery2 = "gor [foo] | join [yyy ] | join <(gor [xxx])";

        manager.updateCreatedFile("[ xxx  ]", "xxx_file.foo");
        manager.updateCreatedFile("[yyy]", "yyy_file.foo");

        query = manager.replaceVirtualFiles(baseQuery2);
        Assert.assertEquals("gor foo_file.txt | join yyy_file.foo | join <(gor xxx_file.foo)", query);
    }

    @Test
    public void virtualFileReplacementWithMultiplSameEntries() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.add("[ foo]");
        manager.add("[bar]");

        final String baseQuery = "gor [foo] [  bar] | join [  foo] | join <(gor [bar])";

        manager.updateCreatedFile("foo", "foo_file.txt");
        manager.updateCreatedFile("bar", "bar_file.txt");

        String query = manager.replaceVirtualFiles(baseQuery);
        Assert.assertEquals("gor foo_file.txt bar_file.txt | join foo_file.txt | join <(gor bar_file.txt)", query);
    }

    @Test
    public void virtualFileReplacementMissingData() {
        VirtualFileManager manager = new VirtualFileManager();
        try {
            manager.add("");
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof GorParsingException);
        }
    }

    @Test
    public void externalVirtualFilesFromQuery() {
        VirtualFileManager manager = new VirtualFileManager();
        manager.addQuery("create xxx = gorrows -p chr1:10-1000 | top 100 | join [grid:foo];create yyy = gor [xxx] | join [ file:/tmp/foo.txt];gor [yyy]");
        VirtualFileEntry[] entries = manager.getExternalVirtualFiles();
        Assert.assertEquals(2, entries.length);
        Assert.assertEquals("[file:/tmp/foo.txt]", entries[0].name);
        Assert.assertEquals("[grid:foo]", entries[1].name);
    }

    @Test
    public void externalVirtualFilesFromQueryWithReplacement() {
        VirtualFileManager manager = new VirtualFileManager();
        String query = "create xxx = gorrows -p chr1:10-1000 | top 100 | join [grid:foo];create yyy = gor [xxx] | join [ file:/tmp/foo.txt];gor [yyy]";
        manager.addQuery(query);
        VirtualFileEntry[] entries = manager.getExternalVirtualFiles();
        Assert.assertEquals(2, entries.length);
        Assert.assertEquals("[file:/tmp/foo.txt]", entries[0].name);
        Assert.assertEquals("[grid:foo]", entries[1].name);

        manager.updateCreatedFile("[file:/tmp/foo.txt]", "file1.txt");

        manager.updateCreatedFile("[grid:foo]", "file2.txt");

        manager.updateCreatedFile("xxx", "result1.gorz");
        manager.updateCreatedFile("yyy", "result2.gorz");

        String newQuery = manager.replaceVirtualFiles(query);
        Assert.assertEquals("create xxx = gorrows -p chr1:10-1000 | top 100 | join file2.txt;create yyy = gor result1.gorz | join file1.txt;gor result2.gorz", newQuery);
    }
}
