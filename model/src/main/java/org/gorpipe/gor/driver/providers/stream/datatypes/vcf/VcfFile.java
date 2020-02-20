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

package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.gorpipe.model.genome.files.gor.ContigDataScheme;
import org.gorpipe.model.genome.files.gor.SourceRef;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by sigmar on 28/10/15.
 */
public class VcfFile extends StreamSourceFile {
    public VcfFile(StreamSource str) {
        super(str);
    }

    @Override
    public DataType getType() {
        return DataType.VCF;
    }

    static final String[] chromosomes = new String[]{"M", "MT", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "XY", "Y"};

    /**
     * Find the gor data offset in a .vcf file
     *
     * @param instream The stream to read
     * @return The offset found
     * @throws IOException If no offset is found
     */
    public static int[] findVcfGorDataOffset(final InputStream instream, ContigDataScheme dataScheme) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Assume .vcf files start with lines
        long offset = -1;
        byte[] buf = new byte[8 * 1024];
        int read = 0, pos = 0;
        int[] ret = null;
        int state = 1; // assume newline already encountered at the beginning
        while ((read = instream.read(buf)) != -1 && offset == -1) {
            baos.write(buf, 0, read);

            for (int i = 0; i < read && ret == null; i++) {
                // looking for the pattern \n# not followed by another #
                switch (buf[i]) {
                    case '\n':
                        state = 1;
                        break;
                    case '#':
                        state = state == 1 ? 2 : 0;
                        break;
                    default:
                        if (state == 2) {
                            // This is the offset into .vcf file that corresponds to the start of gor compatible data in the file
                            int chrNamingSystem = SourceRef.findVcfChrNamingSystem(buf, i + 1, read, instream);
                            ret = new int[]{pos + i, chrNamingSystem};
                        }
                        state = 0;
                        break;
                }
            }
            if (ret != null) break;
            pos += read;
        }

        if (ret == null) {
            throw new RuntimeException("Could not find header line for vcf file");
        }
        findContigOrderFromHeader(dataScheme, baos.toByteArray(), ret[1] == 1);

        return ret;
    }

    private static void findContigOrderFromHeader(final ContigDataScheme dataScheme, byte[] byteArray, boolean hasPrefix) {
        if (dataScheme != null) {
            Map<String, Integer> chr2id = new TreeMap();
            InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(byteArray));
            BufferedReader br = new BufferedReader(isr);

            final Set<String> chrset = new HashSet(Arrays.asList(chromosomes));
            br.lines().peek(new Consumer<String>() {
                int counter = 0;

                @Override
                public void accept(String line) {
                    if (line.startsWith("##contig=")) {
                        String contig = line.substring(13, line.indexOf(','));

                        if (hasPrefix) {
                            if (contig.equals("MT")) contig = "M";
                            String prefix = chrset.contains(contig) ? "chr" : "";
                            dataScheme.setId2chr(counter, prefix + contig);
                            chr2id.put(prefix + contig, counter);
                        } else {
                            dataScheme.setId2chr(counter, contig);
                            chr2id.put(contig, counter);
                        }
                        counter++;
                    }
                }
            }).allMatch(line -> line.startsWith("#")); // short circuiting operation, use takeWhile in jdk9

            int c = 0;
            for (String cont : chr2id.keySet()) {
                dataScheme.setId2order(chr2id.get(cont), c++);
            }
        }
    }
}
