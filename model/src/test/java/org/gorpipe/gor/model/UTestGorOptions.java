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

package org.gorpipe.gor.model;

import gorsat.TestUtils;
import org.gorpipe.gor.model.GorOptions;
import org.gorpipe.gor.model.SourceRef;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Test parsing argument list into GorOptions
 *
 * @version $Id$
 */
public class UTestGorOptions {

    static {
        System.setProperty("filemod.cache.max.entry.cnt", "200");
        System.setProperty("filemod.cache.sleep.time", "10 seconds");
        System.setProperty("filemod.cache.refresh.threshold", "1 second");
    }

    /**
     * Test that ../ paths do not reference above the assumed root
     */
    @Test
    public void testDotDotConstraint() {
        Assert.assertTrue(GorOptions.isPathConstraintWithInRoot("kalli/palli"));
        Assert.assertTrue(GorOptions.isPathConstraintWithInRoot("kalli/../kalli/some.gor"));
        Assert.assertTrue(GorOptions.isPathConstraintWithInRoot("kalli/.././kalli/some.gor"));

        Assert.assertFalse(GorOptions.isPathConstraintWithInRoot("../kalli"));
        Assert.assertFalse(GorOptions.isPathConstraintWithInRoot("kalli/../../kalli"));
    }

    /**
     * Test that quoted file names can be used
     */
    @Test
    public void testQuotedFileNames() {
        String files[] = {"my filename.gor"};
        GorOptions opt = GorOptions.createGorOptions("\'" + files[0] + "\'");
        Assert.assertEquals(files[0], opt.files.get(0).file);

        opt = GorOptions.createGorOptions("\"" + files[0] + "\"");
        Assert.assertEquals(files[0], opt.files.get(0).file);
    }

    /**
     * Test that valid command lines are parsed correctly into options
     *
     * @throws Exception
     */
    @Test
    public void testValidCommandLines() throws Exception {
        checkValidResults("1.mem", false, null, filelist("1.mem"));
        checkValidResults("1.mem 2.mem 3.mem", false, null, filelist("1.mem", "2.mem", "3.mem"));
        checkValidResults("-p 0:2000-10000 1.mem", 0, 2000, 10000, false, null, filelist("1.mem"));
        checkValidResults("-p 0:-10000 1.mem", 0, 0, 10000, false, null, filelist("1.mem"));
        checkValidResults("-p 0:2000 1.mem", 0, 2000, 2000,  false, null, filelist("1.mem"));
        checkValidResults("-p 0:2000- 1.mem", 0, 2000, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p 0:2000-1,000,000,00 1.mem", 0, 2000, 100000000, false, null, filelist("1.mem"));
        checkValidResults("-p 0 1.mem", 0, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p chr1 1.mem", 1, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p chrM 1.mem", 0, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p chrXY 1.mem", 24, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p chrY 1.mem", 25, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p chrM:1,000,00-1,000,000 1.mem", 0, 100000, 1000000, false, null, filelist("1.mem"));
        checkValidResults("-p chrM:1,000,00-1m 1.mem", 0, 100000, 1000000, false, null, filelist("1.mem"));
        checkValidResults("-p 14 1.mem", 14, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));
        checkValidResults("-p 14:- 1.mem", 14, 0, Integer.MAX_VALUE, false, null, filelist("1.mem"));

        File file = File.createTempFile("sometempfile", ".tempfile");
        File afile = new File(file.getParentFile(), "a.gor");
        Files.write(afile.toPath(), "chromo\tpos\tdata\n".getBytes());
        Files.copy(afile.toPath(), afile.getParentFile().toPath().resolve("bucket1.gor"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(afile.toPath(), afile.getParentFile().toPath().resolve("bucket2.gor"), StandardCopyOption.REPLACE_EXISTING);
        final Path bfile = Files.copy(afile.toPath(), afile.getParentFile().toPath().resolve("b.gor"), StandardCopyOption.REPLACE_EXISTING);
        bfile.toFile().setLastModified(System.currentTimeMillis());
        Thread.sleep(200);
        final Path cfile = Files.copy(afile.toPath(), afile.getParentFile().toPath().resolve("c.gor"), StandardCopyOption.REPLACE_EXISTING);
        cfile.toFile().setLastModified(System.currentTimeMillis() + 10000);


        // Ensure a system wide common root is used if no other is specified
        System.setProperty("gor.common.root", "kalli");
        checkValidResults("1.mem", false, null, filelist("kalli/1.mem"));
        // Ensure a system wide common root is NOT used if other is specified
        checkValidResults("1.mem -r Palli", false, null, filelist("Palli/1.mem"));
        System.setProperty("gor.common.root", "");
        checkValidResults("file://1.mem", false, null, filelist("1.mem"));
        checkValidResults("1.mem", false, null, filelist("1.mem"));
        checkValidResults("file://1.mem", false, null, filelist("1.mem"));
    }

    private SourceRef[] filelist(String... files) {
        final SourceRef[] refs = new SourceRef[files.length];
        for (int i = 0; i < refs.length; i++) {
            final String[] parts = files[i].split("=");
            refs[i] = new SourceRef(parts[0], parts.length > 1 ? parts[1] : null, null);
        }
        return refs;
    }

    private void checkValidResults(String args, boolean insertSource,
                                    String sourceColName, SourceRef... files) {
        checkValidResults(args, -1, 0, Integer.MAX_VALUE,
                insertSource, sourceColName, files);
    }

    private void checkValidResults(String args, int chromo, int begin, int end,
                                   boolean insertSource, String sourceColName, SourceRef... files) {
        // Check the passed in commandline
        final GorOptions opt = GorOptions.createGorOptions(args);
        checkValidOptionsParse(chromo, begin, end, insertSource, sourceColName, opt, files);

        // Export the command line from the earlier options and parse again into new options object for checking
        final GorOptions opt2 = GorOptions.createGorOptions(opt.toString());
        checkValidOptionsParse(chromo, begin, end, insertSource, sourceColName, opt2, files);
    }

    /**
     * @param chromo
     * @param begin
     * @param end
     * @param insertSource
     * @param sourceColName
     * @param opt
     * @param files
     */
    private void checkValidOptionsParse(int chromo, int begin, int end, boolean insertSource,
                                        String sourceColName,
                                        final GorOptions opt, SourceRef... files) {
        Assert.assertEquals(chromo, opt.chromo);
        Assert.assertEquals(begin, opt.begin);
        Assert.assertEquals(end, opt.end);
        Assert.assertEquals(insertSource, opt.insertSource);
        Assert.assertEquals(files.length, opt.files.size());
        Assert.assertEquals(sourceColName, opt.sourceColName);

        for (int i = 0; i < files.length; i++) {
            Assert.assertEquals(files[i].file, opt.files.get(i).file);
            Assert.assertEquals(files[i].alias, opt.files.get(i).alias);
        }
    }

    @Test
    public void testGorrowChrPrefix() {
        String[] prefSuf = {"create p = ",""," gor [p];",""}; List res = new ArrayList();
        for (int i=0;i<2;i++) { String[] arg = new String[]{prefSuf[i]+"gorrow 8,145584264,145584264 | calc Reference 'T' | calc Call 'C' | rename #2 Pos;"+prefSuf[i+2]};
            res.add(TestUtils.runGorPipe(arg)); }
        Assert.assertTrue(res.toArray()[0].toString().split("\n")[1].equals(res.toArray()[1].toString().split("\n")[1]));
    }

}
