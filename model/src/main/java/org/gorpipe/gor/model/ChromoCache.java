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

package org.gorpipe.gor.model;

import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorSystemException;

import java.util.HashMap;

/**
 * Chromosome Cache, i.e. class that allows converting chromosome name into a single integer id and comparing
 * the ids in lexicogrpahical order. The cache provides support for standard human chromosomes by default and
 * allows callers to add new chromosome names to be included. Note that when adding names, the lexicographical ordering
 * can change.
 *
 * @version $Id$
 */
public final class ChromoCache {
    private final HashMap<CharSequence, Integer> chr2id = new HashMap(); // Map name as char sequence to the ID
    private final HashMap<CharSequence, Integer> chr2len = new HashMap(); // Map name as char sequence to the ID
    private int chrcnt = 0;
    ContigDataScheme dataScheme;

    /**
     * Construct the ChromoCache
     */
    public ChromoCache(ContigDataScheme dataScheme) {
        init(dataScheme.length() == 0 ? ChrDataScheme.newChrLexico() : dataScheme);
    }

    /**
     * Construct the ChromoCache
     */
    public ChromoCache() {
        init(ChrDataScheme.newChrLexico());
    }

    public void init(ContigDataScheme dataScheme) {
        // Standard Human chromosomes have preallocated ids
        this.dataScheme = dataScheme;

        for (int i = 0; i < dataScheme.length(); i++) {
            String chrname = dataScheme.id2chr(i);
            set(i, chrname);
            if (chrname != null) chr2id.put(chrname.startsWith("chr") ? chrname.substring(3) : chrname, i);
        }

        if (chr2id.containsKey("M") && !chr2id.containsKey("MT")) chr2id.put("MT", chr2id.get("M"));
    }

    /**
     * @return Number of chromosome names that are cached
     */
    public int getChromoCnt() {
        return chrcnt;
    }

    /**
     * @param id The internal id allocated for a chromosome
     * @return Human genome reference consortium standard name for the specified chromosome id, or empty string if not standard human chromosome
     */
    public static String getHgName(int id) {
        return 0 <= id && id < ChrDataScheme.HG.id2chr.length ? ChrDataScheme.HG.id2chr[id] : "";
    }

    /**
     * @param id The internal id allocated for a chromosome
     * @return standard name for the specified chromosome id, or empty string if not standard human chromosome
     */
    public String getChrName(int id) {
        return 0 <= id && id < dataScheme.length() ? dataScheme.id2chr(id) : "";
    }

    /**
     * @param id The internal id allocated for a chromosome
     * @return standard name for the specified chromosome id, or empty string if not standard human chromosome
     */
    public static String getStdChrName(int id) {
        return id < ChrDataScheme.ChrLexico.id2chr.length ? ChrDataScheme.ChrLexico.id2chr[id] : "";
    }

    /**
     * @param id The internal id allocated for a chromosome
     * @return standard name for the specified chromosome id, or empty string if not standard human chromosome
     */
    public static String getStdChrName(ContigDataScheme dataScheme, int id) {
        return 0 <= id && id < dataScheme.length() ? dataScheme.id2chr(id) : "";
    }

    /**
     * @param chr The chromosome id
     * @return The name of the chromosome
     */
    public String toName(int chr) {
        return dataScheme.id2chr(chr);
    }

    /**
     * @param scheme The ChrDataScheme to assume
     * @param chr    The chromosome id
     * @return The name of the chromosome (in HG naming system)
     */
    public String toName(ContigDataScheme scheme, int chr) {
        return chr < 26 ? scheme.id2chr(chr) : dataScheme.id2chr(chr);
    }

    /**
     * @param chr The chromosome id
     * @return True if there is a registered name for this id
     */
    public boolean hasName(int chr) {
        return chr >= 0 && chr < dataScheme.length();
    }

    /**
     * @param chr chromosome name
     * @return The id of the chromosome
     */
    public Integer toId(CharSequence chr) {
        return chr2id.get(chr);
    }

    /**
     * @param chr chromosome name
     * @return The length of the chromosome
     */
    public int toLen(String chr) {
        return chr2len.containsKey(chr) ? chr2len.get(chr) : -1;
    }

    public void setLen(String chr, int len) {
        chr2len.put(chr, len);
    }

    /**
     * @param chr           chromosome name
     * @param addIfNotFound Add the chromosome if not found
     * @return The id of the chromosome, or -1 if chr is not a known chromosome
     */
    public int toIdOrUnknown(CharSequence chr, boolean addIfNotFound) {
        final Integer id = chr2id.get(chr);
        return id != null ? id : addIfNotFound ? addChromosome(chr.toString()) : -1;
    }

    /**
     * @param scheme The ChrDataScheme to assume
     * @param chr    The chromosome id
     * @return The name of the chromosome in bytes
     */
    public byte[] toNameBytes(ContigDataScheme scheme, int chr) {
        return chr < 26 ? scheme.id2chrbytes(chr) : dataScheme.id2chrbytes(chr);
    }


    /**
     * @param chr      Chromosome ID to check for priority
     * @param otherChr Chromosome ID to check with
     * @return True if chr is lexciographically prior to otherChr
     */
    public boolean isChrLexicoPrior(int chr, int otherChr) {
        return dataScheme.id2order(chr) < dataScheme.id2order(otherChr);
    }

    /**
     * @param chrName
     * @return The id of the next chromosome in map or a new higher id if no such is found
     */
    public int findNextInLexicoOrder(String chrName) {
        String next = null;
        for (CharSequence c : chr2id.keySet()) {
            String chr = c.toString();
            if (chr.compareTo(chrName) > 0 && (next == null || next.compareTo(chr) > 0)) { // chr between chrName and next
                next = chr;
            }
        }
        return next != null ? chr2id.get(next) : chrcnt;
    }

    /**
     * Compare two sets of coordinates using lexicographically ordering of chromosome names, i.e.
     * chr1, chr10, chr11, ..., chr19, chr2, chr20, chr21, chr22, chr3, ... ,chr9, chrM, chrX, chrXY, chrY for standard human chromosomes.
     * Any added chromosome is fitted into this lexicographically.
     *
     * @param leftChr  left coordinate chromosome nr
     * @param leftPos  left coordinate position
     * @param rightChr right coordinate chromosome nr
     * @param rightPos right coordinate position
     * @return negative number if left is lower, 0 if equals, and positive number if left is higher
     */
    public int compareLexicoStable(int leftChr, int leftPos, int rightChr, int rightPos) {
        return leftChr == rightChr ? (leftPos - rightPos) : dataScheme.id2order(leftChr) - dataScheme.id2order(rightChr);
    }

    /**
     * Compare to another Source Object using lexicographically ordering of chromosome names, i.e.
     * chr1, chr10, chr11, ..., chr19, chr2, chr20, chr21, chr22, chr3, ... ,chr9, chrM, chrX, chrXY, chrY for standard human chromosomes.
     * Any added chromosome is fitted into this lexicographically.
     * Additionally if the position are equals, then order on extra info field
     *
     * @param leftChr    left coordinate chromosome nr
     * @param leftPos    left coordinate position
     * @param leftExtra  left extra info
     * @param rightChr   right coordinate chromosome nr
     * @param rightPos   right coordinate position
     * @param rightExtra right extra info
     * @return negative number if this is lower, 0 if equals, and positive number if this is higher
     */
    public final int compareLexicoStable(int leftChr, int leftPos, int leftExtra, int rightChr, int rightPos, int rightExtra) {
        return leftChr == rightChr ? (leftPos == rightPos ? leftExtra - rightExtra : leftPos - rightPos) : dataScheme.id2order(leftChr) - dataScheme.id2order(rightChr);
    }

    /**
     * Assume the incoming string is prefixed with chromosome information that end with a tab char.
     *
     * @param str The string to read
     * @return The id of the chromosome information part in the string.
     */
    public int prefixToChromosomeId(CharSequence str) {
        final int idx = prefixToChromosomeIdOrUnknown(str, false);
        if (idx < 0) {
            throw new GorParsingException("Unknown Chromosome. " + str.toString(),"");
        }
        return idx;
    }

    /**
     * Check that the three specified characters form the phrase chr, assuming all lowercase
     *
     * @param ch1 The first letter, assumed to be c
     * @param ch2 The second letter, assumed to be h
     * @param ch3 The third letter, assumed to be r
     * @return True if the letters form the phrase chr
     */
    public static boolean isChr(char ch1, char ch2, char ch3) {
        return ch1 == 'c' && ch2 == 'h' && ch3 == 'r';
    }

    /**
     * Check that the three specified characters form the phrase chr, regardless of high or low case letters
     *
     * @param ch1 The first letter, assumed to be c
     * @param ch2 The second letter, assumed to be h
     * @param ch3 The third letter, assumed to be r
     * @return True if the letters form the phrase chr
     */
    public static boolean isChrIgnoreCase(char ch1, char ch2, char ch3) {
        return (ch1 == 'c' || ch1 == 'C') && (ch2 == 'h' || ch2 == 'H') && (ch3 == 'r' || ch3 == 'R');
    }

    /**
     * Assume the incoming string is prefixed with chromosome information that end with a tab char.
     *
     * @param str           The string to read
     * @param addIfNotFound True to add the chromosome name if not found, else return -1 if chromosme is not found
     * @return The id of the chromosome information part in the string, or -1 if str doesn't start with a known chromosome
     */
    public int prefixToChromosomeIdOrUnknown(CharSequence str, boolean addIfNotFound) {
        final int strlen = str.length();
        if (strlen < 3 || !isChr(str.charAt(0), str.charAt(1), str.charAt(2))) {
            return -1;
        }
        return toIdOrUnknown(str, strlen, addIfNotFound);
    }

    /**
     * Given a string that starts with chr and is at least 4 char long, find the chr id of it
     *
     * @param str           The string, assumed to start with chr and be at least 4 chars long
     * @param strlen        The total length of the string
     * @param addIfNotFound True to add the chromosome name if not found, else return -1 if chromosme is not found
     * @return The id of the chromosome or -1 if this is not a valid chromosome id.
     */
    public int toIdOrUnknown(CharSequence str, int strlen, boolean addIfNotFound) {
        final char ch3 = str.charAt(3);
        if (strlen == 4 || str.charAt(4) == '\t') {
            switch (ch3) {
                case 'Y':
                    return 25;
                case 'X':
                    return 23;
                case 'M':
                    return 0;
                default:
                    return ch3 > '0' && ch3 <= '9' ? ch3 - '0' : lookupNonHumanChromo(str, strlen, addIfNotFound);
            }
        } else if (strlen == 5 || str.charAt(5) == '\t') {
            final char ch4 = str.charAt(4);
            if (ch3 == 'X' && ch4 == 'Y') return 24;
            final int id = (ch3 - '0') * 10 + (ch4 - '0');
            return id >= 10 && id <= 22 ? id : lookupNonHumanChromo(str, strlen, addIfNotFound);
        } else {
            return lookupNonHumanChromo(str, strlen, addIfNotFound);
        }
    }

    /**
     * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
     *
     * @param buf           The buffer to read
     * @param offset        The offset into the buffer to start reading from
     * @param addIfNotFound True to add the chromosome name if not found, else return -1 if chromosme is not found
     * @return The id of the chromosome information part in the buffer, or -1 if buffer doesn't start with a known chromosome
     */
    public int prefixedChrToIdOrUnknown(byte[] buf, int offset, boolean addIfNotFound) {
        return prefixedChrToIdOrUnknown(buf, offset, buf.length, addIfNotFound);
    }

    /**
     * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
     *
     * @param buf           The buffer to read
     * @param offset        The offset into the buffer to start reading from
     * @param buflen        The length of the buffer
     * @param addIfNotFound True to add the chromosome name if not found, else return -1 if chromosme is not found
     * @return The id of the chromosome information part in the buffer, or -1 if buffer doesn't start with a known chromosome
     */
    public int prefixedChrToIdOrUnknown(byte[] buf, int offset, int buflen, boolean addIfNotFound) {
        int extraOffset = buflen > 3 && isChr((char) buf[offset], (char) buf[offset + 1], (char) buf[offset + 2]) ? 3 : 0;
        final byte b3 = buf[offset + extraOffset];
        if (buflen == 4 || (buflen > offset+extraOffset+1 && buf[offset + extraOffset + 1] == '\t')) {
            switch (b3) {
                case 'Y':
                    return 25;
                case 'X':
                    return 23;
                case 'M':
                    return 0;
                default:
                    return b3 > '0' && b3 <= '9' ? b3 - '0' : lookupNonHumanChromo(buf, offset, buflen, addIfNotFound);
            }
        } else if (buflen == 5 || (buflen > offset+extraOffset+2 && buf[offset + extraOffset + 2] == '\t')) {
            if (b3 == 'X' && buf[offset + extraOffset + 1] == 'Y')
                return 24;
            final int id = (b3 - '0') * 10 + (buf[offset + extraOffset + 1] - '0');
            return id >= 10 && id <= 22 ? id : lookupNonHumanChromo(buf, offset, buflen, addIfNotFound);
        }
        return lookupNonHumanChromo(buf, offset, buflen, addIfNotFound);
    }


    /**
     * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
     *
     * @param buf The buffer to read
     * @return The id of the chromosome information part in the string.
     */
    public int prefixToChromosomeId(byte[] buf) {
        final int id = prefixedChrToIdOrUnknown(buf, 0, false);
        if (id == -1) {
            throw new GorSystemException("Invalid chromosome information", null);
        }
        return id;
    }

    /**
     * Add a new chromosome to the cache.
     *
     * @param name The name to be added. It will be inserted into lexicographically ordering
     *             of already added chromosome names
     * @return The id of the newly added chromosome
     */
    public int addChromosome(String name) {
        // Allocate ID and resize data structures as needed
        final int id = chrcnt;
        if (dataScheme.length() <= id) { // Resize arrays by doubling in size if we have outgrown the supported number of chromosome names
            final int len = dataScheme.length();
            final int size = len * 2;
            String[] id2chrNew = new String[size];
            for (int i = 0; i < len; i++) {
                id2chrNew[i] = dataScheme.id2chr(i);
            }
            dataScheme.newId2Chr(id2chrNew);

            final int[] orderingNew = new int[size];
            for (int i = 0; i < len; i++) {
                orderingNew[i] = dataScheme.id2order(i);
                //System.arraycopy(chromosomeOrdering, 0, orderingNew, 0, chromosomeOrdering.length);
            }
            dataScheme.newOrder(orderingNew);
        }

        // Figure out ordering of this name, lexicographically in relevance to all other cached chromosome names
        // 1. Find the position to insert the new chromosome into the array at
        final int[] ordered = getChrInLexicoOrder();
        int pos = chrcnt - 1;
        while (pos >= 0 && toName(ordered[pos]).compareTo(name) > 0) {
            pos--;
        }
        final int orderingIdx = pos + 1;
        // 2. Update ordering of all entries at or after the new entry
        for (int i = 0; i < chrcnt; i++) {
            if (dataScheme.id2order(i) >= orderingIdx) { // All previous entry with ordering at or after the new entry
                dataScheme.setId2order(i, dataScheme.id2order(i) + 1); // Move lexicographical ordering 1 to the right
            }
        }

        // Set the new entry
        dataScheme.setId2order(id, orderingIdx);
        set(id, name);
        return id;
    }

    /**
     * @return An array with the chromosome Ids in lexico order
     */
    public int[] getChrInLexicoOrder() {
        // Create an array with the lexico ordered elements,
        final int[] ordered = new int[chrcnt];
        for (int i = 0; i < chrcnt; i++) {
            ordered[dataScheme.id2order(i)] = i;
        }
        return ordered;
    }

// Private implementation methods

    private int lookupNonHumanChromo(CharSequence str, int strlen, boolean addIfNotFound) {
        int i = 1;
        while (i < strlen && str.charAt(i) != '\t') {
            i++;
        }
        return toIdOrUnknown(str.subSequence(0, i), addIfNotFound);
    }

    private int lookupNonHumanChromo(byte[] buf, int offset, int buflen, boolean addIfNotFound) {
        int i = 1;
        while (i < buflen && buf[offset + i] != '\t') {
            i++;
        }
        return toIdOrUnknown(new String(buf, offset, i), addIfNotFound);
    }

    private void set(int i, String name) {
        chrcnt++;
        dataScheme.setId2chr(i, name);
        chr2id.put(name, i);
    }
}
