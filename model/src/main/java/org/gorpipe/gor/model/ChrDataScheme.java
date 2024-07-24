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


import java.util.*;

/**
 * ChrDataScheme defines the naming and ordering scheme of chromosome data. It assigns internal IDs to the chromsome, such that the standard human chromosome are
 * M = 0, 1 = 1, ..., X = 23, XY = 24 and Y = 25. All other chromsomes will get IDs after this range
 * <p>
 * When working with chromsome data, each chromosome name must be converted into this interal ID which is then used to decide input/ouput order and names
 *
 * @version $Id$
 */
public class ChrDataScheme implements ContigDataScheme {
    /**
     * New Chromosome Lexico object
     */
    public static ChrDataScheme newChrLexico() {
        return new ChrDataScheme(
                new String[]{"chrM", "chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr20", "chr21", "chr22", "chrX", "chrXY", "chrY"},
                new int[]{22, 0, 11, 15, 16, 17, 18, 19, 20, 21, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 23, 24, 25}
        );
    }

    /**
     * New Chromosome Numerical object
     */
    public static ChrDataScheme newChrNumerical() {
        return new ChrDataScheme(
                new String[]{"chrM", "chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr20", "chr21", "chr22", "chrX", "chrXY", "chrY"},
                new int[]{25, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24}
        );
    }

    /**
     * Chromosome names are prefixed with chr and all names are in lexicographical order
     */
    public static final ChrDataScheme ChrLexico = newChrLexico();

    /**
     * Chromosome names are prefixed with chr and all names are in numerical order
     */
    public static final ChrDataScheme ChrNumerical = newChrNumerical();

    /**
     * Human chromosomes are named 1,2,...,22,X,XY,Y,MT and are first in that order, all other are in lexicographically order thereafter
     */
    public static final ChrDataScheme HG = new ChrDataScheme(
            new String[]{"MT", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "XY", "Y"},
            new int[]{25, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24}
    );

    /**
     * Lookup the name of a chromosome from the id of it
     *
     * @param id The unique ID of the chromosome, with in the scheme
     * @return The name of the chromosome
     */
    public String getChrName(int id) {
        if (id < 0 || id > id2chr.length) {
            throw new RuntimeException("Unexpected internal chromosome id " + id);
        }
        return id2chr[id];
    }

    // Treat the folowing arrays as their contents are final, even though Java doesn't provide such mechanism, i.e. only read, never modify
    /**
     * Map the internal chromosome ID to the name of each chromosome
     */
    String[] id2chr;
    /**
     * Map the internal chromosome ID to the name of each chromosomes as bytes
     */
    byte[][] id2chrbytes;
    /**
     * Map the internal chromosome ID to the default order of the chromosome in this data scheme
     */
    public int[] id2order;
    /**
     * Map the order of the chromosome in this data scheme to the internal chromosome ID to the default order of the chromosome
     */
    public int[] order2id;
    /**
     * Map the name to id.
     */
    public Map<String,Integer> name2id;
    /**
     * Length of the chromosome array
     */
    int length;

    public ChrDataScheme(String[] names, int[] id2order) {
        assert names.length == id2order.length;

        newOrder(id2order);
        newId2Chr(names);
    }

    public int[] getOrder2id() {
        return order2id;
    }

    @Override
    public String id2chr(int i) {
        return id2chr[i];
    }

    @Override
    public byte[] id2chrbytes(int i) {
        return id2chrbytes[i];
    }

    @Override
    public int id2order(int i) {
        return id2order[i];
    }

    @Override
    public void setId2order(int i, int val) {
        id2order[i] = val;
        order2id[val] = i;
    }

    @Override
    public void setId2chr(int i, String chr) {
        id2chr[i] = chr;
        id2chrbytes[i] = chr.getBytes();
        if (i >= length) length = i + 1;
    }

    @Override
    public void newOrder(int[] neworder) {
        id2order = neworder;
        order2id = new int[id2order.length];
        for (int i = 0; i < id2order.length; i++) { // Map the order of the chromsome back to the internal id of the chromosome
            order2id[id2order[i]] = i;
        }
    }

    @Override
    public void newId2Chr(String[] newid2chr) {
        id2chr = newid2chr;
        id2chrbytes = new byte[id2chr.length][];
        name2id = new HashMap<>();
        int i;
        for (i = 0; i < id2chr.length; i++) {
            String chr = id2chr[i];
            if (chr == null) {
                break;
            }
            id2chrbytes[i] = chr.getBytes();
            name2id.put(chr, i);
        }
        length = i;
    }

    @Override
    public int order2id(int i) {
        return order2id[i];
    }

    public int chr2id(String chr) {
        return name2id.get(chr);
    }

    public int chr2order(String chr) {
        return id2order[name2id.get(chr)];
    }

    @Override
    public int length() {
        return length;
    }

    /**
     * Find the order of the specified new chromosome name in the provided mapping which folows this naming/ordering scheme
     *
     * @param name       The unique name of the chromsome to be added
     * @param idsInOrder The IDs of the chromosomes in the order defined by this scheme
     * @param id2name    The mapping of internal IDs to the unique names of each chromosome
     * @param chrcnt     The number of chromosomes in the provided arrays
     * @return The index to insert of the name at, to ensure ordering as defined by these scheme
     */
    public int findChromosomeOrder(String name, int[] idsInOrder, String[] id2name, int chrcnt) {
        // All names are in lexicographically order
        int pos = chrcnt - 1;
        while (pos >= 0 && id2name[idsInOrder[pos]].compareTo(name) > 0) {
            pos--;
        }
        return pos + 1;
    }

    /**
     * Update the given data scheme with the provided ordered list of chromosomes.
     *
     * @param dataScheme        Data scheme to update.
     * @param orderedContigList The ordered list of chromosomes.
     */
    public static void updateDataScheme(ContigDataScheme dataScheme, List<String> orderedContigList) {
        int id = 0;
        for (String contig : orderedContigList) {
            dataScheme.setId2chr(id++, contig);
        }
        int c = 0;
        for (String contig : orderedContigList.stream().sorted().toList()) {
            dataScheme.setId2order(orderedContigList.indexOf(contig), c++);
        }
    }

    /**
     * Sort the list of contigs using the provided data scheme.
     * @param contigs        contigs to sort.
     * @param sortDataScheme data scheme to use for sorting.
     * @return sorted list of contigs.
     */
    public static List<String> sortUsingChrDataScheme(List<String> contigs, ChrDataScheme sortDataScheme) {
        List<String> orderedContigList;
        orderedContigList = new ArrayList<>(Collections.nCopies(HG.length() + contigs.size(), null));
        int extraIndex = sortDataScheme.length();
        for (String contig : contigs) {
            String hgName = contig.startsWith("chr") ? contig : "chr" + contig;
            if (sortDataScheme.name2id.containsKey(hgName)) {
                orderedContigList.set(sortDataScheme.id2order[sortDataScheme.name2id.get(hgName)], contig);
            } else {
                // Unknown contigs come just in order of appearance after known contigs.
                orderedContigList.set(extraIndex++, contig);
            }
        }
        orderedContigList = orderedContigList.stream().filter(Objects::nonNull).toList();
        return orderedContigList;
    }
}
