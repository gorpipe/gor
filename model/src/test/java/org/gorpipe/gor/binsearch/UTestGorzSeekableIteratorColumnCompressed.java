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

package org.gorpipe.gor.binsearch;

import org.gorpipe.gor.binsearch.GorZipLexOutputStream;
import org.gorpipe.gor.binsearch.GorzSeekableIterator;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.model.gor.RowObj;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class UTestGorzSeekableIteratorColumnCompressed {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void test_columnCompressed() throws IOException {
        final String columnCompressed = tf.newFile("genes.gorz").getAbsolutePath();
        final String genes = "../tests/data/gor/genes.gor";
        final GorZipLexOutputStream os = new GorZipLexOutputStream(columnCompressed, true);
        final BufferedReader br = new BufferedReader(new FileReader(genes));
        os.setHeader(br.readLine());
        br.lines().forEach(l -> {
            try {
                os.write(RowObj.apply(l));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        os.close();
        br.close();

        final BufferedReader br2 = new BufferedReader(new FileReader(genes));
        final GorzSeekableIterator gsi = new GorzSeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(columnCompressed))));

        Assert.assertEquals(br2.readLine(), String.join("\t", gsi.getHeader()));

        br2.lines().forEach(line -> {
            Assert.assertTrue(gsi.hasNext());
            Assert.assertEquals(line, gsi.next().toString());
        });

        Assert.assertFalse(gsi.hasNext());
    }
}
