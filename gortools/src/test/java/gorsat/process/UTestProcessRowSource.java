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

import org.gorpipe.gor.GorSessionFactory;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;


public class UTestProcessRowSource {

    @Test
    public void testCheckNotNested() {
        GorSessionFactory factory = new GenericSessionFactory();
        String cmd = "gor ../tests/data/gor/genes.gorz";
        String out = ProcessRowSource.checkNested(cmd, factory.create(), new StringBuilder());
        Assert.assertEquals(cmd, out);
    }

    @Test
    public void testCheckNested() {
        GorSessionFactory factory = new GenericSessionFactory();
        String cmd = "<(gor ../tests/data/dbnsp_test.gor)";
        String out = ProcessRowSource.checkNested(cmd, factory.create(), new StringBuilder());

        String tmpdir = System.getProperty("java.io.tmpdir");
        if( tmpdir == null || tmpdir.length() == 0 ) tmpdir = "/tmp";
        Path tmpath = Paths.get(tmpdir);
        String scmd = cmd.substring(2,cmd.length()-1);
        Path fifopath = tmpath.resolve( Integer.toString(Math.abs(scmd.hashCode())) );
        String pipename = fifopath.toAbsolutePath().toString();
        Assert.assertEquals(pipename, out);
    }

}