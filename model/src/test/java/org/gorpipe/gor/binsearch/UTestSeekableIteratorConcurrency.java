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

import org.gorpipe.gor.binsearch.SeekableIterator;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class UTestSeekableIteratorConcurrency {

    @Test
    public void readSameFileInManyThreads() throws IOException, ExecutionException, InterruptedException {
        final int threadCount = 10;

        final String path = "../tests/data/gor/genes.gor";
        final Random r = new Random(13);
        final BufferedReader br = new BufferedReader(new FileReader(path));
        br.readLine();
        final List<StringIntKey> keys = br.lines().filter(line -> r.nextFloat() < 0.02).map(line -> {
            final String[] cols = line.split("\t");
            return new StringIntKey(cols[0], Integer.parseInt(cols[1]));
        }).collect(Collectors.toList());

        Collections.shuffle(keys, r);

        final ExecutorService es = Executors.newFixedThreadPool(threadCount);
        final Future[] futures = new Future[threadCount];
        for (int i = 0; i < threadCount; ++i) {
            final Future future = es.submit(() -> {
                final SeekableIterator si;
                try {
                    si = new SeekableIterator(new StreamSourceSeekableFile(new FileSource(new SourceReference(path))), true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                keys.forEach(key -> {
                    try {
                        si.seek(key);
                        Assert.assertTrue(si.hasNext());
                        Assert.assertTrue(si.getNextAsString().startsWith(key.chr + "\t" + key.bpair));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                try {
                    si.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            futures[i] = future;
        }

        for (Future future : futures) {
            future.get();
        }
    }
}
