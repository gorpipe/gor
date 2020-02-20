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

import org.gorpipe.model.genome.files.gor.FileReader;
import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.gor.GorSession;
import gorsat.Commands.Analysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IOTestAnalyser extends Analysis {
    private Map<String, Integer> headerIndexMap = new HashMap<>();
    private Random rnd = new Random();
    String header;
    private boolean parallel;
    private List<Row> rows;
    private FileReader fileReader;
    private String cacheDir;

    public IOTestAnalyser(String header, boolean parallel, GorSession session) {
        this.header = header;
        this.parallel = parallel;
        this.fileReader = session.getProjectContext().getFileReader();
        this.cacheDir = session.getProjectContext().getCacheDir();
        rows = new ArrayList<>();
    }

    @Override
    public void setup() {
        String[] hsplit = header.split("\t");
        for (int i = 0; i < hsplit.length; i++) {
            headerIndexMap.put(hsplit[i].toLowerCase(), i);
        }
    }

    @Override
    public void finish() {
        List<Thread> lt = new ArrayList<>();
        List<Row> res = Collections.synchronizedList(new ArrayList<>());
        rows.forEach(row -> {
            Thread t = new Thread(() -> res.add(testRow(row)));
            lt.add(t);
            t.start();
        });
        lt.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        //List<Row> nlist = rows.stream().parallel().map(this::testRow).collect(Collectors.toList());
        res.forEach(super::process);
    }

    private Row testRow(Row row) {
        int bytebuffersize = 1000000;
        byte[] bb = new byte[bytebuffersize];
        ByteBuffer bbb = ByteBuffer.wrap(bb);

        String filepath = row.colAsString(headerIndexMap.get("filepath")).toString();
        long streamsize = (long) row.colAsDouble(headerIndexMap.get("streamsize"));
        int seeknum = headerIndexMap.containsKey("seeknum") ? row.colAsInt(headerIndexMap.get("seeknum")) : 1;
        String write = headerIndexMap.containsKey("write") ? row.colAsString(headerIndexMap.get("write")).toString() : null;

        Path p;
        try {
            long t = System.nanoTime();
            long total = 0;
            if (write != null && (write.equals("1") || write.equals("true")) && !filepath.contains("..") && !filepath.startsWith("/")) {
                p = fileReader.toPath(cacheDir);
                p = p.resolve(filepath);
                try (OutputStream os = Files.newOutputStream(p)) {
                    while (total < streamsize) {
                        long wsize = Math.min(bb.length, streamsize - total);
                        os.write(bb, 0, (int) wsize);
                        total += wsize;
                    }
                }
                Files.delete(p);
            } else {
                p = fileReader.toPath(filepath);
                if (seeknum > 0) {
                    long filesize = Files.size(p);
                    for (int i = 0; i < seeknum; i++) {
                        long subtotal = 0;
                        try (SeekableByteChannel sbc = Files.newByteChannel(p)) {
                            sbc.position(Math.abs(rnd.nextLong()) % Math.max(1, filesize - streamsize));
                            bbb.limit((int) Math.min(bb.length, streamsize));
                            int r = sbc.read(bbb);
                            while (r > 0) {
                                subtotal += r;
                                if (subtotal == streamsize) break;
                                bbb.rewind();
                                bbb.limit((int) Math.min(bb.length, streamsize - subtotal));
                                r = sbc.read(bbb);
                            }
                            bbb.rewind();
                            if (bb.length > streamsize - subtotal) bbb.limit((int) (streamsize - subtotal));
                            total += subtotal;
                        }
                    }
                } else {
                    try (InputStream is = Files.newInputStream(p)) {
                        int r = is.read(bb, 0, (int) Math.min(bb.length, streamsize));
                        while (r > 0) {
                            total += r;
                            if (total == streamsize) break;
                            r = is.read(bb, 0, (int) Math.min(bb.length, streamsize - total));
                        }
                    }
                }
            }
            long et = System.nanoTime();
            return row.rowWithAddedColumn(total + "\t" + (et - t));
        } catch (IOException e) {
            throw new RuntimeException("Unable to access file " + filepath, e);
        }
    }

    @Override
    public void process(Row row) {
        if (parallel) {
            rows.add(row);
        } else {
            super.process(testRow(row));
        }
    }

    @Override
    public String getHeader() {
        return header + "\ttotalbytes\ttime";
    }
}

