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

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.ChrDataScheme;
import org.gorpipe.gor.model.ContigDataScheme;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static org.gorpipe.gor.model.ChrDataScheme.*;

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
    public static final String VCF_HEADER_TOKEN = "##";

    /**
     * Find the gor data offset in a .vcf file
     *
     * @param instream The stream to read
     * @param dataScheme inferred dataschema to return.
     * @param fixInternalIndex  should internal index be fixed (ordered)
     * @return The offset found
     * @throws IOException If no offset is found
     */
    public static int[] findVcfGorDataOffset(final InputStream instream, ContigDataScheme dataScheme, boolean fixInternalIndex) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Assume .vcf files start with lines
        long offset = -1;
        byte[] buf = new byte[8 * 1024];
        int read, pos = 0;
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
                            int chrNamingSystem = findVcfChrNamingSystem(buf, i + 1, read, instream);
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
        findContigOrderFromHeader(dataScheme, baos.toByteArray(), ret[1] == 1, fixInternalIndex);

        return ret;
    }

    private static void findContigOrderFromHeader(final ContigDataScheme dataScheme, byte[] byteArray,
                                                  boolean hasPrefix, boolean fixInternalIndex) {
        if (dataScheme != null) {
            final List<String> originalContigList = new ArrayList<>();
            InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(byteArray));
            BufferedReader br = new BufferedReader(isr);

            final Set<String> chrset = new HashSet(Arrays.asList(chromosomes));
            br.lines().peek(new Consumer<>() {
                @Override
                public void accept(String line) {
                    if (line.startsWith("##contig=")) {
                        String contig = line.substring(13, line.indexOf(','));

                        if (hasPrefix) {
                            if (contig.equals("MT")) contig = "M";
                            String prefix = chrset.contains(contig) ? "chr" : "";
                            contig = prefix + contig;
                        }
                        originalContigList.add(contig);
                    }
                }
            }).allMatch(line -> line.startsWith("#")); // short circuiting operation, use takeWhile in jdk9

            if (fixInternalIndex) {
                ChrDataScheme.updateDataScheme(dataScheme, fixContigsOrder(originalContigList));
            } else {
                ChrDataScheme.updateDataScheme(dataScheme, originalContigList);
            }
        }
    }

    static List<String> fixContigsOrder(List<String> oldContigs) {
        if (oldContigs == null || oldContigs.isEmpty()) return oldContigs;

        List<String> orderedContigList;
        if (ChrDataScheme.isMostLikelyLexicalOrder(oldContigs)) {
            orderedContigList = ChrDataScheme.sortUsingChrDataScheme(oldContigs, ChrLexico);
        } else {
            // Else assume numerical ordering, fix the order by using the order from Human Genome (HG) data scheme.
            orderedContigList = ChrDataScheme.sortUsingChrDataScheme(oldContigs, ChrNumerical);
        }

        return orderedContigList;
    }

    public static int findVcfChrNamingSystem(byte[] buf, int cur, int read, final InputStream instream) throws IOException {
        // Find next line and check the value in the first column (i.e. the assumed chromosome column)
        while (read != 0) {
            while (cur < read) {
                if (buf[cur++] == '\n') {
                    if (cur > read) { // Need more data prior to find first letter of first column
                        read = instream.read(buf);
                        cur = 0;
                    }
                    return (cur >= read || buf[cur] == 'c' || buf[cur] == 'C') ? 0 : 1; // if chromosome starts with c assume it is gor naming system and order, else hg naming system and order
                }
            }

            // Need more data prior to finding the end of line
            read = instream.read(buf);
            cur = 0;
        }

        return 0; // Empty file which we treat as gor naming system
    }

    public static List<String> loadVcfHeader(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        List<String> header = new ArrayList<>();

        while(reader.ready()) {
            var line = reader.readLine();

            if (line.isEmpty()) continue;

            if (!line.startsWith(VCF_HEADER_TOKEN)) {
                break;
            } else {
                header.add(line);
            }
        }

        return header;
    }
}
