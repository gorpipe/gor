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

package org.gorpipe.model.genome.files.gor.genotypeutilities;

import java.util.Arrays;
import java.util.List;

public class ValueColumnParsing {

    private ValueColumnParsing() {}

    public static void parseBiAllelicHardCalls(CharSequence values, byte[] buffer) {
        final int len = values.length();
        for (int i = 0; i < len; ++i) {
            buffer[i] = (byte) (values.charAt(i) - '0');
        }
    }

    public static void parseBiAllelicHardCalls(CharSequence values, int[] gt1, int[] gt2) {
        final int len = values.length();
        for (int i = 0; i < len; ++i) {
            switch (values.charAt(i)) {
                case '0': {
                    gt1[i] = 0;
                    gt2[i] = 0;
                    break;
                }
                case '1': {
                    gt1[i] = 0;
                    gt2[i] = 1;
                    break;
                }
                case '2': {
                    gt1[i] = 1;
                    gt2[i] = 1;
                    break;
                }
                default: {
                    gt1[i] = -1;
                    gt2[i] = -1;
                }
            }
        }
    }

    public static void parseMultiAllelicHardCalls(List<CharSequence> valuesCols, int[] gt1, int[] gt2) {
        Arrays.fill(gt1, 0);
        Arrays.fill(gt2, 0);

        for (int valueColIdx = valuesCols.size() - 1; valueColIdx != -1; --valueColIdx) {
            final CharSequence valueCol = valuesCols.get(valueColIdx);
            final int len = valueCol.length();
            for (int i = 0; i < len; ++i) {
                setGenotypeValue(gt1, gt2, valueColIdx, valueCol, i);
            }
        }
    }

    public static void parseImputedGenotypes(CharSequence values, float[][] pr) {
        for (int i = 0; i < pr.length; ++i) {
            final float[] pri = pr[i];
            final char c1 = values.charAt(2 * i);
            final char c2 = values.charAt(2 * i + 1);
            if (c1 == ' ' && c2 == ' ') {
                pri[0] = 0f;
                pri[1] = 0f;
                pri[2] = 0f;
            } else {
                final float pr1 = getProbFromChar(values.charAt(2 * i));
                final float pr2 = getProbFromChar(values.charAt(2 * i + 1));
                pri[0] = 1 - pr1 - pr2;
                pri[1] = pr1;
                pri[2] = pr2;
            }
        }
    }

    public static void parseImputedGenotypes(CharSequence values, float threshold, byte[] hc, float[] dosages) {
        final int len = values.length();
        float p0, p1, p2;
        for (int valueIdx = 0, vrIdx = 0; valueIdx < len; valueIdx += 2, ++vrIdx) {
            p1 = getProbFromChar(values.charAt(valueIdx));
            p2 = getProbFromChar(values.charAt(valueIdx + 1));
            p0 = 1 - p1 - p2;
            dosages[vrIdx] = p1 + 2 * p2;

            if (p0 > threshold) hc[vrIdx] = 0;
            else if (p1 > threshold) hc[vrIdx] = 1;
            else if (p2 > threshold) hc[vrIdx] = 2;
            else hc[vrIdx] = 3;
        }
    }

    public static void fillHC(int[] gt1, int[] gt2, byte[] hc) {
        for (int i = 0; i < hc.length; ++i) {
            switch (gt1[i]) {
                case 0: {
                    if (gt2[i] == 0) hc[i] = 0;
                    else hc[i] = 1;
                    break;
                }
                case -1: {
                    hc[i] = 3;
                    break;
                }
                default: {
                    hc[i] = 2;
                }
            }
        }
    }

    private static float getProbFromChar(char c) {
        return (126 - c) / 93f;
    }

    private static void setGenotypeValue(int[] gt1, int[] gt2, int valueColIdx, CharSequence valueCol, int i) {
        switch (valueCol.charAt(i)) {
            case '0': break;
            case '1': {
                if (gt2[i] == 0) gt2[i] = valueColIdx + 1;
                else gt1[i] = valueColIdx + 1;
                break;
            }
            case '2': {
                gt1[i] = valueColIdx + 1;
                gt2[i] = valueColIdx + 1;
                break;
            }
            default: {
                gt1[i] = -1;
                gt2[i] = -1;
            }
        }
    }
}
