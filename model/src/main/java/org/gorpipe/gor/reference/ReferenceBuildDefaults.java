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

package org.gorpipe.gor.reference;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReferenceBuildDefaults {

    private static final String CHR_1 = "chr1";
    private static final String CHR_2 = "chr2";
    private static final String CHR_3 = "chr3";
    private static final String CHR_4 = "chr4";
    private static final String CHR_5 = "chr5";
    private static final String CHR_6 = "chr6";
    private static final String CHR_7 = "chr7";
    private static final String CHR_8 = "chr8";
    private static final String CHR_9 = "chr9";
    private static final String CHR_10 = "chr10";
    private static final String CHR_11 = "chr11";
    private static final String CHR_12 = "chr12";
    private static final String CHR_13 = "chr13";
    private static final String CHR_14 = "chr14";
    private static final String CHR_15 = "chr15";
    private static final String CHR_16 = "chr16";
    private static final String CHR_17 = "chr17";
    private static final String CHR_18 = "chr18";
    private static final String CHR_19 = "chr19";
    private static final String CHR_20 = "chr20";
    private static final String CHR_21 = "chr21";
    private static final String CHR_22 = "chr22";
    private static final String CHR_X = "chrX";
    private static final String CHR_Y = "chrY";
    private static final String CHR_M = "chrM";
    private static final String CHR_XY = "chrXY";

    private static final Map<String, Integer> buildSize_generic;
    private static final Map<String, Integer> buildSplit_generic;
    private static final Map<String, Integer> buildSize_hg18;
    private static final Map<String, Integer> buildSplit_hg18;
    private static final Map<String, Integer> buildSize_hg19;
    private static final Map<String, Integer> buildSplit_hg19;
    private static final Map<String, Integer> buildSize_hg38;
    private static final Map<String, Integer> buildSplit_hg38;

    private ReferenceBuildDefaults(){}

    public static Map<String, Integer> buildSizeGeneric() {
        return buildSize_generic;
    }

    public static Map<String, Integer> buildSplitGeneric() {
        return buildSplit_generic;
    }

    public static Map<String, Integer> buildSizeHg18() {
        return buildSize_hg18;
    }

    public static Map<String, Integer> buildSplitHg18() {
        return buildSplit_hg18;
    }

    public static Map<String, Integer> buildSizeHg19() {
        return buildSize_hg19;
    }

    public static Map<String, Integer> buildSplitHg19() {
        return buildSplit_hg19;
    }

    public static Map<String, Integer> buildSizeHg38() {
        return buildSize_hg38;
    }

    public static Map<String, Integer> buildSplitHg38() {
        return buildSplit_hg38;
    }

    static {
        // Generic build/split
        Map<String, Integer> buildSizeMapGeneric = new HashMap<>();
        buildSizeMapGeneric.put(CHR_1, 250000000);
        buildSizeMapGeneric.put(CHR_2, 250000000);
        buildSizeMapGeneric.put(CHR_3, 200000000);
        buildSizeMapGeneric.put(CHR_4, 200000000);
        buildSizeMapGeneric.put(CHR_5, 200000000);
        buildSizeMapGeneric.put(CHR_6, 200000000);
        buildSizeMapGeneric.put(CHR_7, 200000000);
        buildSizeMapGeneric.put(CHR_8, 150000000);
        buildSizeMapGeneric.put(CHR_9, 150000000);
        buildSizeMapGeneric.put(CHR_10, 150000000);
        buildSizeMapGeneric.put(CHR_11, 150000000);
        buildSizeMapGeneric.put(CHR_12, 150000000);
        buildSizeMapGeneric.put(CHR_13, 150000000);
        buildSizeMapGeneric.put(CHR_14, 150000000);
        buildSizeMapGeneric.put(CHR_15, 150000000);
        buildSizeMapGeneric.put(CHR_16, 100000000);
        buildSizeMapGeneric.put(CHR_17, 100000000);
        buildSizeMapGeneric.put(CHR_18, 100000000);
        buildSizeMapGeneric.put(CHR_19, 100000000);
        buildSizeMapGeneric.put(CHR_20, 100000000);
        buildSizeMapGeneric.put(CHR_21, 100000000);
        buildSizeMapGeneric.put(CHR_22, 100000000);
        buildSizeMapGeneric.put(CHR_X, 200000000);
        buildSizeMapGeneric.put(CHR_Y, 100000000);
        buildSizeMapGeneric.put(CHR_M, 20000);
        buildSizeMapGeneric.put(CHR_XY, 1);
        buildSize_generic = Collections.unmodifiableMap(buildSizeMapGeneric);
        buildSplit_generic = Collections.unmodifiableMap(new HashMap<>());

        // Hg18 build/split
        Map<String, Integer> buildSizeMapHg18 = new HashMap<>();
        buildSizeMapHg18.put(CHR_1, 247249719);
        buildSizeMapHg18.put(CHR_2, 242951149);
        buildSizeMapHg18.put(CHR_3, 199501827);
        buildSizeMapHg18.put(CHR_4, 191273063);
        buildSizeMapHg18.put(CHR_5, 180857866);
        buildSizeMapHg18.put(CHR_6, 170899992);
        buildSizeMapHg18.put(CHR_7, 158821424);
        buildSizeMapHg18.put(CHR_8, 146274826);
        buildSizeMapHg18.put(CHR_9, 140273252);
        buildSizeMapHg18.put(CHR_10, 135374737);
        buildSizeMapHg18.put(CHR_11, 134452384);
        buildSizeMapHg18.put(CHR_12, 132349534);
        buildSizeMapHg18.put(CHR_13, 114142980);
        buildSizeMapHg18.put(CHR_14, 106368585);
        buildSizeMapHg18.put(CHR_15, 100338915);
        buildSizeMapHg18.put(CHR_16, 88827254);
        buildSizeMapHg18.put(CHR_17, 78774742);
        buildSizeMapHg18.put(CHR_18, 76117153);
        buildSizeMapHg18.put(CHR_19, 63811651);
        buildSizeMapHg18.put(CHR_20, 62435964);
        buildSizeMapHg18.put(CHR_21, 46944323);
        buildSizeMapHg18.put(CHR_22, 49691432);
        buildSizeMapHg18.put(CHR_X, 154913754);
        buildSizeMapHg18.put(CHR_Y, 57772954);
        buildSizeMapHg18.put(CHR_M, 16571);
        buildSizeMapHg18.put(CHR_XY, 1);
        buildSize_hg18 = Collections.unmodifiableMap(buildSizeMapHg18);

        Map<String, Integer> buildSplitMapHg18 = new HashMap<>();
        buildSplitMapHg18.put(CHR_1, 124000000);
        buildSplitMapHg18.put(CHR_10, 40350000);
        buildSplitMapHg18.put(CHR_11, 52750000);
        buildSplitMapHg18.put(CHR_12, 35000000);
        buildSplitMapHg18.put(CHR_2, 93100000);
        buildSplitMapHg18.put(CHR_3, 91350000);
        buildSplitMapHg18.put(CHR_4, 50750000);
        buildSplitMapHg18.put(CHR_5, 47650000);
        buildSplitMapHg18.put(CHR_6, 60125000);
        buildSplitMapHg18.put(CHR_7, 59330000);
        buildSplitMapHg18.put(CHR_8, 45500000);
        buildSplitMapHg18.put(CHR_9, 53700000);
        buildSplitMapHg18.put(CHR_X, 60000000);
        buildSplit_hg18 = Collections.unmodifiableMap(buildSplitMapHg18);

        // Hg19 build/split
        Map<String, Integer> buildSizeMapHg19 = new HashMap<>();
        buildSizeMapHg19.put(CHR_1, 249250621);
        buildSizeMapHg19.put(CHR_2, 243199373);
        buildSizeMapHg19.put(CHR_3, 198022430);
        buildSizeMapHg19.put(CHR_4, 191154276);
        buildSizeMapHg19.put(CHR_5, 180915260);
        buildSizeMapHg19.put(CHR_6, 171115067);
        buildSizeMapHg19.put(CHR_7, 159138663);
        buildSizeMapHg19.put(CHR_8, 146364022);
        buildSizeMapHg19.put(CHR_9, 141213431);
        buildSizeMapHg19.put(CHR_10, 135534747);
        buildSizeMapHg19.put(CHR_11, 135006516);
        buildSizeMapHg19.put(CHR_12, 133851895);
        buildSizeMapHg19.put(CHR_13, 115169878);
        buildSizeMapHg19.put(CHR_14, 107349540);
        buildSizeMapHg19.put(CHR_15, 102531392);
        buildSizeMapHg19.put(CHR_16, 90354753);
        buildSizeMapHg19.put(CHR_17, 81195210);
        buildSizeMapHg19.put(CHR_18, 78077248);
        buildSizeMapHg19.put(CHR_19, 59128983);
        buildSizeMapHg19.put(CHR_20, 63025520);
        buildSizeMapHg19.put(CHR_21, 48129895);
        buildSizeMapHg19.put(CHR_22, 51304566);
        buildSizeMapHg19.put(CHR_X, 155270560);
        buildSizeMapHg19.put(CHR_Y, 59373566);
        buildSizeMapHg19.put(CHR_M, 16571);
        buildSizeMapHg19.put(CHR_XY, 1);
        buildSize_hg19 = Collections.unmodifiableMap(buildSizeMapHg19);
        buildSplit_hg19 = buildSplit_hg18;

        // Hg38 build/split
        Map<String, Integer> buildSizeMapHg38 = new HashMap<>();
        buildSizeMapHg38.put(CHR_1, 248956422);
        buildSizeMapHg38.put(CHR_2, 242193529);
        buildSizeMapHg38.put(CHR_3, 198295559);
        buildSizeMapHg38.put(CHR_4, 190214555);
        buildSizeMapHg38.put(CHR_5, 181538259);
        buildSizeMapHg38.put(CHR_6, 170805979);
        buildSizeMapHg38.put(CHR_7, 159345973);
        buildSizeMapHg38.put(CHR_8, 145138636);
        buildSizeMapHg38.put(CHR_9, 138394717);
        buildSizeMapHg38.put(CHR_10, 133797422);
        buildSizeMapHg38.put(CHR_11, 135086622);
        buildSizeMapHg38.put(CHR_12, 133275309);
        buildSizeMapHg38.put(CHR_13, 114364328);
        buildSizeMapHg38.put(CHR_14, 107043718);
        buildSizeMapHg38.put(CHR_15, 101991189);
        buildSizeMapHg38.put(CHR_16, 90338345);
        buildSizeMapHg38.put(CHR_17, 83257441);
        buildSizeMapHg38.put(CHR_18, 80373285);
        buildSizeMapHg38.put(CHR_19, 58617616);
        buildSizeMapHg38.put(CHR_20, 64444167);
        buildSizeMapHg38.put(CHR_21, 46709983);
        buildSizeMapHg38.put(CHR_22, 50818468);
        buildSizeMapHg38.put(CHR_X, 156040895);
        buildSizeMapHg38.put(CHR_Y, 57227415);
        buildSizeMapHg38.put(CHR_M, 16569);
        buildSizeMapHg38.put(CHR_XY, 1);
        buildSize_hg38 = Collections.unmodifiableMap(buildSizeMapHg38);

        Map<String, Integer> buildSplitMapHg38 = new HashMap<>();
        buildSplitMapHg38.put(CHR_1, 123400000);
        buildSplitMapHg38.put(CHR_10, 39800000);
        buildSplitMapHg38.put(CHR_11, 53400000);
        buildSplitMapHg38.put(CHR_12, 35500000);
        buildSplitMapHg38.put(CHR_2, 93900000);
        buildSplitMapHg38.put(CHR_3, 90900000);
        buildSplitMapHg38.put(CHR_4, 50000000);
        buildSplitMapHg38.put(CHR_5, 48800000);
        buildSplitMapHg38.put(CHR_6, 59800000);
        buildSplitMapHg38.put(CHR_7, 60100000);
        buildSplitMapHg38.put(CHR_8, 45200000);
        buildSplitMapHg38.put(CHR_9, 43000000);
        buildSplitMapHg38.put(CHR_X, 61000000);
        buildSplit_hg38 = Collections.unmodifiableMap(buildSplitMapHg38);
    }
}
