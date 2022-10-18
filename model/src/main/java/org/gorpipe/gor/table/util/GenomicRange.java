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

package org.gorpipe.gor.table.util;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorSystemException;

import java.util.Arrays;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Helper class to map genomic range to string and back.
 * <p>
 * Created by gisli on 22/08/16.
 *
 * Notes:
 * 1. Empty stop range implies same range as the start.
 * 2. "", NULL and -1 imply not specified values.
 */
public class GenomicRange {

    public static final GenomicRange EMPTY_RANGE = new GenomicRange();

    // How to treat empty values in the string rep.   These values are picked for backward compatibility with existing code.
    public static final String MIN_START_CHR = "";
    public static final int MIN_START_POS = 0;
    public static final String MAX_STOP_CHR = "~";
    public static final int MAX_STOP_POS = Integer.MAX_VALUE;

    static final String NOTSET_CHR = "?";
    static final int NOTSET_POS = -1;


    private final String startChr;
    private final int startPos;
    private final String stopChr;
    private final int stopPos;
    boolean isEmpty;


    public GenomicRange() {
        this(null, -1, null, -1);
        this.isEmpty = true;

    }

    public GenomicRange(String startChr, int startPos, String stopChr, int stopPos) {
        // TODO:  Add more validation (if pos then chr, start <= stop)
        this.startChr = !isNullOrEmpty(startChr) ? startChr : NOTSET_CHR;
        this.startPos = startPos >= 0 ? startPos : NOTSET_POS;
        this.stopChr = !isNullOrEmpty(stopChr) ? stopChr : this.NOTSET_CHR;
        this.stopPos = stopPos >= 0 ? stopPos : NOTSET_POS;
        this.isEmpty = isNullOrEmpty(startChr) && startPos < 0 && isNullOrEmpty(stopChr) && stopPos < 0;
    }

    public GenomicRange(String startChr, int startPos) {
        this(startChr, startPos, NOTSET_CHR, NOTSET_POS);
    }

    public String getStartChr() {
        return !NOTSET_CHR.equals(startChr) ? startChr : null;
    }

    public int getStartPos() {
        return NOTSET_POS != startPos ? startPos : MIN_START_POS;
    }

    public String getStopChr() {
        return !NOTSET_CHR.equals(stopChr) ? stopChr : getStartChr();
    }

    public int getStopPos() {
        return NOTSET_POS != stopPos ? stopPos : MAX_STOP_POS;
    }

    @Override
    public String toString() {
        if (isEmpty) return "";
        return String.format("%s%s%s%s%s%s%s",
                !NOTSET_CHR.equals(this.startChr) ? this.startChr : "",
                !NOTSET_CHR.equals(this.startChr) && this.startPos != NOTSET_POS  ? ":" : "",
                !NOTSET_CHR.equals(this.startChr) && this.startPos != NOTSET_POS  ? this.startPos : "",
                !this.startChr.equals(this.stopChr) || this.stopPos != NOTSET_POS ? "-" : "",
                !NOTSET_CHR.equals(this.stopChr) && !this.startChr.equals(this.stopChr) ? this.stopChr : "",
                !NOTSET_CHR.equals(this.stopChr) && !this.startChr.equals(this.stopChr) && this.stopPos != NOTSET_POS ? ":" : "",
                this.stopPos != NOTSET_POS ? this.stopPos : "");
    }

    public String formatAsTabDelimited() {
        if (isEmpty) return "\t\t\t";
        StringBuilder sb =  new StringBuilder();
        formatAsTabDelimited(sb);
        return sb.toString();
    }

    public StringBuilder formatAsTabDelimited(StringBuilder sb) {
        if (isEmpty) {
            sb.append("\t\t\t");
        } else {
            sb.append(!NOTSET_CHR.equals(this.startChr) ? this.startChr : "");
            sb.append('\t');
            sb.append(getStartPos());
            sb.append('\t');
            sb.append(!NOTSET_CHR.equals(stopChr) ? stopChr : (!NOTSET_CHR.equals(this.startChr) ? this.startChr : ""));
            sb.append('\t');
            sb.append(getStopPos());
        }
        return sb;
    }

    /**
     * Parse genomic range string.
     *
     * @param genomicRange string to parse.
     * @return new GenomicRange object constructed from the string.
     */
    public static GenomicRange parseGenomicRange(String genomicRange) {
        if (genomicRange == null) {
            return null;
        }

        if (genomicRange.isEmpty()) {
            return new GenomicRange();
        }

        Boolean testChromosom = false;
        String[] startLoc;
        String[] stopLoc;
        if (genomicRange.contains("\t")) {
            String[] cols = genomicRange.split("\t", -1);
            startLoc = Arrays.copyOfRange(cols, 0, 2);
            stopLoc = Arrays.copyOfRange(cols, 2, 4);
        } else {
            testChromosom = true;
            String[] pos = genomicRange.split("[\\-]");
            startLoc = pos[0].contains(":") ? pos[0].split(":") : new String[]{pos[0], "-1"};
            stopLoc = pos.length > 1 ? pos[1].split(":") : new String[]{"", "-1"};

            if (stopLoc.length == 1) {
                // Have only one item in stopLoc, create correct length two stopLoc.
                stopLoc = stopLoc[0].startsWith("chr") ? new String[]{stopLoc[0], "-1"} : new String[]{"", stopLoc[0]};
            }
        }

        if (startLoc.length != 2 || stopLoc.length != 2) {
            throw new GorSystemException("Could not parse genomic range: " + genomicRange, null);
        }

        if (stopLoc[0].length() == 0 && !stopLoc[0].equals("-1")) {
            stopLoc[0] = startLoc[0];
        }

        // See if the chromosome is available
        String startChromosome = startLoc[0].trim();
        String stopChromosome = stopLoc[0].trim();

        // We only accept a start chromosome so lets test it for validity
        if (testChromosom && startChromosome.isEmpty() && stopChromosome.isEmpty()) {
            throw new GorDataException("Invalid chromosome defined: " + startLoc[0]);
        }

        int startLocation =  startLoc[1].length() > 0 ? parseLocationWithError(startLoc[1]) : -1;
        int stopLocation = stopLoc[1].length() > 0 ? parseLocationWithError(stopLoc[1]) : -1;

        return new GenomicRange(startChromosome, startLocation, stopChromosome, stopLocation);
    }

    private static int parseLocationWithError(String location) {
        try {
            return Integer.parseInt(location.trim());
        } catch (NumberFormatException e) {
            throw new GorDataException("Invalid chromosome location at " + location, e);
        }
    }
}
