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

package org.gorpipe.gor.cli.index;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(name = "index",
        aliases = {"i"},
        description="Index gor data files",
        header="Index gor data files")
public class IndexCommand extends HelpOptions implements Runnable {

    @CommandLine.Option(names={"-f","--fullindex"},
            description = "Performs a full index of gor file.")
    private boolean performFullIndex;

    @CommandLine.Parameters(arity = "1..*",
            paramLabel = "Files",
            description = "Queries to execute. Queries can be direct gor query, files containing gor script or gor report template.")
    private File[] files;

    private long pos = 0;
    private final byte[] buffer = new byte[10000000];
    private int r = 0;
    private int i = 0;
    private int lastOffset;
    private boolean lastwritten = false;
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private ByteArrayOutputStream lastbaos = null;

    private boolean stringPrefixEquals(String b1, String b2, int len) {
        for( int j = 0; j < len; j++ ) {
            if( b1.charAt(j) != b2.charAt(j) ) return false;
        }
        return true;
    }

    private void writeUntil(ByteArrayOutputStream baos, InputStream is, char charToFind ) throws IOException {
        while (i < r && buffer[i] != charToFind) i += 1;
        while (r > 0 && i == r) {
            baos.write(buffer, lastOffset, i - lastOffset);
            pos = pos + r;
            r = is.read(buffer);
            i = 0;
            lastOffset = 0;
            while (i < r && buffer[i] != charToFind) i += 1;
        }
        i += 1;
        if (r > 0) {
            if( lastOffset < buffer.length ) baos.write(buffer, lastOffset, i - lastOffset);
            else baos.write(charToFind);
        }
    }

    private void skipUntil( InputStream is, char charToFind ) throws IOException {
        while (i < r && buffer[i] != charToFind) i += 1;
        while (r > 0 && i == r) {
            pos = pos + r;
            r = is.read(buffer);
            i = 0;
            while (i < r && buffer[i] != charToFind) i += 1;
        }
        i += 1;
    }

    private ByteArrayOutputStream writeBufferToFile(Writer fos) throws IOException {
        if (performFullIndex) {
            fos.write(baos.toString());
        } else if (lastbaos == null) {
            fos.write(baos.toString());
            lastbaos = baos;
            baos = new ByteArrayOutputStream();
        } else {
            var tbuffer = baos.toString();
            int o = 0;
            while( o < tbuffer.length() && tbuffer.charAt(o) != '\t' ) o++;
            if( o == tbuffer.length() ) o = -1;
            var lastbuffer = lastbaos.toString();
            int k = 0;
            while( k < lastbuffer.length() && lastbuffer.charAt(k) != '\t' ) k++;

            if( k == lastbuffer.length() ) k = -1;
            if (k == -1 || k != o || !stringPrefixEquals(tbuffer, lastbuffer, k)) {
                if (!lastwritten) {
                    fos.write(lastbuffer);
                }
                fos.write(tbuffer);
                lastwritten = true;
            } else lastwritten = false;
            ByteArrayOutputStream nbaos = lastbaos;
            lastbaos = baos;
            baos = nbaos;
        }
        baos.reset();

        return lastbaos;
    }

    private void indexFile(File gorFile) {
        Path gorindex = Paths.get(gorFile.toString() + ".gori");

        try (
                StreamSource ds = (StreamSource) GorDriverFactory.fromConfig().resolveDataSource(new SourceReferenceBuilder(gorFile.toString()).build());
                InputStream is = ds.open();
                Writer fos = Files.newBufferedWriter(gorindex)
        ) {
            fos.write("## fileformat=GORIv2\n");
            while (i == r) {
                pos = pos + r;
                r = is.read(buffer);
                i = 0;
                while (i < r && buffer[i] != '\n') i += 1;
            }
            i += 1;
            lastOffset = i;

            while (r > 0) {
                writeBufferToFile(fos);
                writeUntil(baos, is, '\t');
                lastOffset = i;
                writeUntil(baos, is, '\t');
                skipUntil(is, '\n');
                lastOffset = i;
                long totalOffset = pos + lastOffset;

                if (r > 0) {
                    byte[] bytes = (totalOffset + "\n").getBytes();
                    baos.write(bytes);
                }
            }
            if (!lastwritten && lastbaos != null) {
                fos.write(lastbaos.toString());
            }
            fos.write(baos.toString());
        } catch (IOException e) {
            throw new GorSystemException("gor file index failed",e);
        }
    }

    @Override
    public void run() {
        for (File file : files) {
            indexFile(file);
        }
    }
}
