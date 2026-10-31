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

import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Characterization of the LIFTOVER command (ENGKNOW-4117).
 * <p>
 * Task 1 pinned the pre-P4 output, quirks and bugs included. Tests whose expectation the native liftover
 * engine (P4, Task 11) changes carry a {@code // P4-CHANGES: <what>} comment and assert a {@code *_P4}
 * constant. The pre-P4 output stays in the matching {@code *_LEGACY} constant, which
 * {@code UTestGorLiftoverDifferential.oracleMatchesFrozenLegacy} checks the legacy oracle against.
 */
public class UTestGorLiftoverCharacterization {

    private static final String CHAIN_HEADER = "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore";
    private static final String SNP_HEADER = "Chrom\tPos\tRef\tAlt\tId";
    private static final String SEG_HEADER = "Chrom\tStart\tEnd\tName";
    private static final String SEG3_HEADER = "Chrom\tStart\tEnd";

    // Same chain as UTestGorLiftover.mapstr (qScore above Int range).
    private static final String MAPSTR =
            "Chrom\ttStart\ttEnd\tqChrom\tqStart\tqEnd\tqStrand\tqScore\n" +
            "chr12\t154166\t154277\tchr12\t45000\t45111\t+\t12341034620\n" +
            "chr12\t154278\t154325\tchr12\t45112\t45159\t+\t12341034620\n" +
            "chr12\t154326\t154453\tchr12\t45160\t45287\t+\t12341034620\n" +
            "chr12\t154480\t6934516\tchr12\t45314\t6825350\t+\t12341034620\n" +
            "chr12\t6934516\t6938023\tchr12\t6825351\t6828858\t+\t12341034620\n" +
            "chr12\t6938023\t7029175\tchr12\t6828859\t6920011\t+\t12341034620\n" +
            "chr12\t7029175\t7080211\tchr12\t6920012\t6971048\t+\t12341034620\n" +
            "chr12\t7080212\t7090659\tchr12\t6971050\t6981497\t+\t12341034620\n" +
            "chr12\t7090659\t7094372\tchr12\t6981498\t6985211\t+\t12341034620\n" +
            "chr12\t7094372\t7094767\tchr12\t6985212\t6985607\t+\t12341034620\n" +
            "chr12\t7094768\t7094790\tchr12\t6985608\t6985630\t+\t12341034620\n" +
            "chr12\t7094790\t7103556\tchr12\t6986698\t6995464\t+\t12341034620\n" +
            "chr12\t7103563\t7108115\tchr12\t6995471\t7000023\t+\t12341034620\n" +
            "chr12\t7108115\t7118625\tchr12\t7000810\t7011320\t+\t12341034620\n" +
            "chr12\t7118625\t7189876\tchr12\t7011321\t7082572\t+\t12341034620\n" +
            "chr12\t7239876\t9994445\tchr12\t7087280\t9841849\t+\t12341034620\n" +
            "chr12\t9994448\t11331782\tchr12\t9841849\t11179183\t+\t12341034620\n";

    // Fixture paths (set in setUp).
    static String CHAIN;
    static String CHAIN_OVL;
    static String CHAIN_ZERO;
    static String SNP;
    static String VAR;
    static String SEG;
    static String SEG3;
    static String EDGE;
    static String OVLIN;
    static String OVLSEG;
    static String REFOPT;
    static String COLL;
    static String ZSEG;
    static String MA;
    static String BAM;
    static String MAPSTR_CHAIN;
    static String BIGSCORE_IN;

    private static Path tmpdir;

    // ------------------------------------------------------------------------------------------------
    // Expected outputs that P4 changes.
    // ------------------------------------------------------------------------------------------------

    static final String SNP_DEFAULT_LEGACY = lines(
            "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
            "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped",
            "chr1\t1150\tA\tG\ts01\tchr1\t150\t+\tmapped",
            "chr1\t1200\tA\tG\ts02\tchr1\t200\t+\tmapped",
            "chr1\t1201\tA\tG\ts03\tchr1\t201\t+\tmapped",
            "chr1\t1300\tA\tG\ts04\tchr1\t300\t+\tmapped",
            "chr1\t5001\tA\tG\ts09\tchr1\t500\t-\tmapped",
            "chr1\t5100\tA\tG\ts08\tchr1\t401\t-\tmapped",
            "chr1\t8875\tA\tG\ts11\tchr1\t875\t+\tmapped",
            "chr1\t9875\tA\tG\ts11\tchr1\t875\t+\tmapped",
            "chr2\t0\tA\tG\ts12\tchr2\t100\t\tunmapped",
            "chr2\t2150\tA\tG\ts10\tchr1\t650\t+\tmapped",
            "chrM\t998\tA\tG\ts13\tchrM\t1000\t+\tmapped");

    static final String SNP_ALL_LEGACY = withScores(SNP_DEFAULT_LEGACY,
            "0.0 1000.0 1000.0 1000.0 1000.0 900.0 900.0 500.0 500.0 0.0 800.0 100.0");

    static final String VAR_DEFAULT_LEGACY = lines(
            "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
            "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped",
            "chr1\t0\tACG\tT\tv04\tchr1\t199\t\tunmapped",
            "chr1\t1150\tA\tG\tv01\tchr1\t150\t+\tmapped",
            "chr1\t1160\t\tG\tv02\tchr1\t160\t+\tmapped",
            "chr1\t1198\tACG\tT\tv03\tchr1\t198\t+\tmapped",
            "chr1\t5062\t\tA\tv11\tchr1\t440\t-\tmapped",
            "chr1\t5071\tT\t>LED<\tv10\tchr1\t430\t-\tmapped",
            "chr1\t5075\tGT\tTAA,C\tv09\tchr1\t425\t-\tmapped",
            "chr1\t5081\tG\tC,T\tv08\tchr1\t420\t-\tmapped",
            "chr1\t5090\tGT\tC\tv07\tchr1\t410\t-\tmapped",
            "chr1\t8875\tA\tG\tv13\tchr1\t875\t+\tmapped",
            "chr1\t9875\tA\tG\tv13\tchr1\t875\t+\tmapped",
            "chr2\t2150\tA\tG\tv12\tchr1\t650\t+\tmapped",
            "chrM\t998\tA\tG\tv14\tchrM\t1000\t+\tmapped");

    static final String VAR_ALL_LEGACY = withScores(VAR_DEFAULT_LEGACY,
            "0.0 0.0 1000.0 1000.0 1000.0 900.0 900.0 900.0 900.0 900.0 500.0 500.0 800.0 100.0");

    static final String SEG_DEFAULT_LEGACY = lines(
            "Chrom\tStart\tEnd\tName\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus",
            "chr1\t0\t1\tdup\tchr1\t350\t360\t\tunmapped",
            "chr1\t0\t1\tg02\tchr1\t150\t250\t\tunmapped",
            "chr1\t0\t1\tg03\tchr1\t250\t450\t\tunmapped",
            "chr1\t1120\t1180\tg01\tchr1\t120\t180\t+\tmapped",
            "chr1\t5050\t5090\tg06\tchr1\t410\t450\t-\tmapped",
            "chr1\t8860\t8880\tg08\tchr1\t860\t880\t+\tmapped",
            "chr1\t9860\t9880\tg08\tchr1\t860\t880\t+\tmapped",
            "chr2\t2110\t2190\tg07\tchr1\t610\t690\t+\tmapped",
            "chrM\t998\t1098\tg09\tchrM\t1000\t1100\t+\tmapped");

    static final String SEG_ALL_LEGACY = withScores(SEG_DEFAULT_LEGACY,
            "0.0 0.0 0.0 1000.0 900.0 500.0 500.0 800.0 100.0");

    static final String SEG3_DEFAULT_LEGACY = lines(
            "Chrom\tStart\tEnd\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus",
            "chr1\t0\t1\tchr1\t150\t250\t\tunmapped",
            "chr1\t0\t1\tchr1\t250\t450\t\tunmapped",
            "chr1\t0\t1\tchr1\t350\t360\t\tunmapped",
            "chr1\t1120\t1180\tchr1\t120\t180\t+\tmapped",
            "chr1\t5050\t5090\tchr1\t410\t450\t-\tmapped",
            "chr1\t8860\t8880\tchr1\t860\t880\t+\tmapped",
            "chr1\t9860\t9880\tchr1\t860\t880\t+\tmapped",
            "chr2\t2110\t2190\tchr1\t610\t690\t+\tmapped",
            "chrM\t998\t1098\tchrM\t1000\t1100\t+\tmapped");

    static final String SEG3_ALL_LEGACY = withScores(SEG3_DEFAULT_LEGACY,
            "0.0 0.0 0.0 1000.0 900.0 500.0 500.0 800.0 100.0");

    static final String REFOPT_HEADER =
            "Chrom\tPos\tRef\tAlt\tLongRef\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus";

    static final String REFOPT_DEFAULT_LEGACY = lines(
            REFOPT_HEADER,
            "chr1\t0\t\tG\tAC\tchr1\t101\t\tunmapped",
            "chr1\t1195\tA\tG\tACGTACGTAC\tchr1\t195\t+\tmapped");

    static final String REFOPT_LONGREF_LEGACY = lines(
            REFOPT_HEADER,
            "chr1\t0\t\tG\tAC\tchr1\t101\t\tunmapped",
            "chr1\t0\tA\tG\tACGTACGTAC\tchr1\t195\t\tunmapped");

    static final String MA_LEGACY = lines(
            "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
            "chr1\t5081\tA\tTC,A\tm01\tchr1\t420\t-\tmapped");

    static final String BAM_DEFAULT_LEGACY = lines(
            "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS\thgold_Chrom\thgold_Pos\thgold_End\thgold_qStrand\thgold_liftoverStatus",
            "chr1\t0\t1\tr3\tAACG\tIIJK\t4M\t10\tchr1\t300\t350\t\tunmapped",
            "chr1\t1160\t1190\tr1\tAACG\tIIJK\t2M2S\t1170\tchr1\t160\t190\t+\tmapped",
            "chr1\t5050\t5090\tr2\tCGTT\tKJII\t2S2M\t5060\tchr1\t410\t450\t-\tmapped");

    static final String BAM_ALL_LEGACY = withScores(BAM_DEFAULT_LEGACY, "0.0 1000.0 900.0")
            + "chr3\t3160\t3190\tr1\tAACG\tIIJK\t2M2S\t3170\tchr1\t160\t190\t+\tmapped\t400.0\n";

    static final String ZERO_STRAND_HEADER =
            "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus";

    static final String ZERO_STRAND_LEGACY = lines(
            ZERO_STRAND_HEADER,
            "chr1\t0\tA\tG\to01\tchr1\t175\t\tunmapped");

    static final String ZERO_STRAND_ALL_LEGACY = lines(
            ZERO_STRAND_HEADER + "\thgold_score",
            "chr1\t0\tA\tG\to01\tchr1\t175\t\tunmapped\t0.0");

    static final String COLL_NOTE_LEGACY = lines(
            "CHROM\tPOS\thg19_note\thgold_POS\tqStrand\thg19_CHROM\thg19_POS\thg19_qStrand\thg19_liftoverStatus",
            "chr1\t1150\tn1\tx\ty\tchr1\t150\t+\tmapped");

    // ------------------------------------------------------------------------------------------------
    // Expected outputs after P4 (Task 11). Ties at one position sort by the rest of the row.
    // ------------------------------------------------------------------------------------------------

    private static final String SNP_OUT_HEADER =
            "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus";
    private static final String SEG_OUT_HEADER =
            "Chrom\tStart\tEnd\tName\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus";
    private static final String SEG3_OUT_HEADER =
            "Chrom\tStart\tEnd\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus";
    private static final String BAM_OUT_HEADER = "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS\thgold_Chrom"
            + "\thgold_Pos\thgold_End\thgold_qStrand\thgold_liftoverStatus";

    private static final String SNP_DUP = "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped";
    private static final String SEG_DUP = "chr1\t0\t1\tdup\tchr1\t350\t360\t\tunmapped";
    private static final String SEG3_DUP = "chr1\t0\t1\tchr1\t350\t360\t\tunmapped";
    private static final String V08_P4 = "chr1\t5081\tG\tT,C\tv08\tchr1\t420\t-\tmapped";
    private static final String V09_P4 = "chr1\t5075\tGT\tC,TAA\tv09\tchr1\t425\t-\tmapped";
    private static final String V10_P4 = "chr1\t5071\tT\t<DEL>\tv10\tchr1\t430\t-\tmapped";

    static final String SNP_DEFAULT_P4 = lines(
            SNP_OUT_HEADER,
            SNP_DUP,
            SNP_DUP,
            SNP_DUP,
            "chr1\t0\tA\tG\ts11\tchr1\t875\t\tambiguous",
            "chr1\t1150\tA\tG\ts01\tchr1\t150\t+\tmapped",
            "chr1\t1200\tA\tG\ts02\tchr1\t200\t+\tmapped",
            "chr1\t1201\tA\tG\ts03\tchr1\t201\t+\tmapped",
            "chr1\t1300\tA\tG\ts04\tchr1\t300\t+\tmapped",
            "chr1\t5001\tA\tG\ts09\tchr1\t500\t-\tmapped",
            "chr1\t5100\tA\tG\ts08\tchr1\t401\t-\tmapped",
            "chr2\t0\tA\tG\ts12\tchr2\t100\t\tunmapped",
            "chr2\t2150\tA\tG\ts10\tchr1\t650\t+\tmapped",
            "chrM\t998\tA\tG\ts13\tchrM\t1000\t+\tmapped");

    static final String SNP_ALL_P4 = withScores(lines(
            SNP_OUT_HEADER,
            SNP_DUP,
            SNP_DUP,
            SNP_DUP,
            "chr1\t1150\tA\tG\ts01\tchr1\t150\t+\tmapped",
            "chr1\t1200\tA\tG\ts02\tchr1\t200\t+\tmapped",
            "chr1\t1201\tA\tG\ts03\tchr1\t201\t+\tmapped",
            "chr1\t1300\tA\tG\ts04\tchr1\t300\t+\tmapped",
            "chr1\t5001\tA\tG\ts09\tchr1\t500\t-\tmapped",
            "chr1\t5100\tA\tG\ts08\tchr1\t401\t-\tmapped",
            "chr1\t8875\tA\tG\ts11\tchr1\t875\t+\tmapped",
            "chr1\t9875\tA\tG\ts11\tchr1\t875\t+\tmapped",
            "chr2\t0\tA\tG\ts12\tchr2\t100\t\tunmapped",
            "chr2\t2150\tA\tG\ts10\tchr1\t650\t+\tmapped",
            "chrM\t998\tA\tG\ts13\tchrM\t1000\t+\tmapped"),
            "0.0 0.0 0.0 1000.0 1000.0 1000.0 1000.0 900.0 900.0 500.0 500.0 0.0 800.0 100.0");

    static final String VAR_DEFAULT_P4 = lines(
            SNP_OUT_HEADER,
            SNP_DUP,
            SNP_DUP,
            "chr1\t0\tA\tG\tv13\tchr1\t875\t\tambiguous",
            "chr1\t0\tACG\tT\tv04\tchr1\t199\t\tunmapped",
            "chr1\t1150\tA\tG\tv01\tchr1\t150\t+\tmapped",
            "chr1\t1160\t\tG\tv02\tchr1\t160\t+\tmapped",
            "chr1\t1198\tACG\tT\tv03\tchr1\t198\t+\tmapped",
            "chr1\t5062\t\tA\tv11\tchr1\t440\t-\tmapped",
            V10_P4,
            V09_P4,
            V08_P4,
            "chr1\t5090\tGT\tC\tv07\tchr1\t410\t-\tmapped",
            "chr2\t2150\tA\tG\tv12\tchr1\t650\t+\tmapped",
            "chrM\t998\tA\tG\tv14\tchrM\t1000\t+\tmapped");

    static final String VAR_ALL_P4 = withScores(lines(
            SNP_OUT_HEADER,
            SNP_DUP,
            SNP_DUP,
            "chr1\t0\tACG\tT\tv04\tchr1\t199\t\tunmapped",
            "chr1\t1150\tA\tG\tv01\tchr1\t150\t+\tmapped",
            "chr1\t1160\t\tG\tv02\tchr1\t160\t+\tmapped",
            "chr1\t1198\tACG\tT\tv03\tchr1\t198\t+\tmapped",
            "chr1\t5062\t\tA\tv11\tchr1\t440\t-\tmapped",
            V10_P4,
            V09_P4,
            V08_P4,
            "chr1\t5090\tGT\tC\tv07\tchr1\t410\t-\tmapped",
            "chr1\t8875\tA\tG\tv13\tchr1\t875\t+\tmapped",
            "chr1\t9875\tA\tG\tv13\tchr1\t875\t+\tmapped",
            "chr2\t2150\tA\tG\tv12\tchr1\t650\t+\tmapped",
            "chrM\t998\tA\tG\tv14\tchrM\t1000\t+\tmapped"),
            "0.0 0.0 0.0 1000.0 1000.0 1000.0 900.0 900.0 900.0 900.0 900.0 500.0 500.0 800.0 100.0");

    static final String SEG_DEFAULT_P4 = lines(
            SEG_OUT_HEADER,
            SEG_DUP,
            SEG_DUP,
            "chr1\t0\t1\tg02\tchr1\t150\t250\t\tunmapped",
            "chr1\t0\t1\tg03\tchr1\t250\t450\t\tunmapped",
            "chr1\t0\t1\tg08\tchr1\t860\t880\t\tambiguous",
            "chr1\t1120\t1180\tg01\tchr1\t120\t180\t+\tmapped",
            "chr1\t5050\t5090\tg06\tchr1\t410\t450\t-\tmapped",
            "chr2\t2110\t2190\tg07\tchr1\t610\t690\t+\tmapped",
            "chrM\t998\t1098\tg09\tchrM\t1000\t1100\t+\tmapped");

    static final String SEG_ALL_P4 = withScores(lines(
            SEG_OUT_HEADER,
            SEG_DUP,
            SEG_DUP,
            "chr1\t0\t1\tg02\tchr1\t150\t250\t\tunmapped",
            "chr1\t0\t1\tg03\tchr1\t250\t450\t\tunmapped",
            "chr1\t1120\t1180\tg01\tchr1\t120\t180\t+\tmapped",
            "chr1\t5050\t5090\tg06\tchr1\t410\t450\t-\tmapped",
            "chr1\t8860\t8880\tg08\tchr1\t860\t880\t+\tmapped",
            "chr1\t9860\t9880\tg08\tchr1\t860\t880\t+\tmapped",
            "chr2\t2110\t2190\tg07\tchr1\t610\t690\t+\tmapped",
            "chrM\t998\t1098\tg09\tchrM\t1000\t1100\t+\tmapped"),
            "0.0 0.0 0.0 0.0 1000.0 900.0 500.0 500.0 800.0 100.0");

    static final String SEG3_DEFAULT_P4 = lines(
            SEG3_OUT_HEADER,
            "chr1\t0\t1\tchr1\t150\t250\t\tunmapped",
            "chr1\t0\t1\tchr1\t250\t450\t\tunmapped",
            SEG3_DUP,
            SEG3_DUP,
            "chr1\t0\t1\tchr1\t860\t880\t\tambiguous",
            "chr1\t1120\t1180\tchr1\t120\t180\t+\tmapped",
            "chr1\t5050\t5090\tchr1\t410\t450\t-\tmapped",
            "chr2\t2110\t2190\tchr1\t610\t690\t+\tmapped",
            "chrM\t998\t1098\tchrM\t1000\t1100\t+\tmapped");

    static final String SEG3_ALL_P4 = withScores(lines(
            SEG3_OUT_HEADER,
            "chr1\t0\t1\tchr1\t150\t250\t\tunmapped",
            "chr1\t0\t1\tchr1\t250\t450\t\tunmapped",
            SEG3_DUP,
            SEG3_DUP,
            "chr1\t1120\t1180\tchr1\t120\t180\t+\tmapped",
            "chr1\t5050\t5090\tchr1\t410\t450\t-\tmapped",
            "chr1\t8860\t8880\tchr1\t860\t880\t+\tmapped",
            "chr1\t9860\t9880\tchr1\t860\t880\t+\tmapped",
            "chr2\t2110\t2190\tchr1\t610\t690\t+\tmapped",
            "chrM\t998\t1098\tchrM\t1000\t1100\t+\tmapped"),
            "0.0 0.0 0.0 0.0 1000.0 900.0 500.0 500.0 800.0 100.0");

    static final String REFOPT_LONGREF_P4 = lines(
            REFOPT_HEADER,
            "chr1\t0\tA\tG\tACGTACGTAC\tchr1\t195\t\tunmapped",
            "chr1\t1101\t\tG\tAC\tchr1\t101\t+\tmapped");

    static final String MA_P4 = lines(
            SNP_OUT_HEADER,
            "chr1\t5081\tA\tA,TC\tm01\tchr1\t420\t-\tmapped");

    static final String BAM_DEFAULT_P4 = lines(
            BAM_OUT_HEADER,
            "chr1\t0\t1\tr3\tAACG\tIIJK\t4M\t0\tchr1\t300\t350\t\tunmapped",
            "chr1\t1160\t1190\tr1\tAACG\tIIJK\t2M2S\t1170\tchr1\t160\t190\t+\tmapped",
            "chr1\t5051\t5091\tr2\tCGTT\tKJII\t2S2M\t5081\tchr1\t410\t450\t-\tmapped");

    // the last row is r1 through the 400-score block; MPOS 170 lifts through that row's own block (N15)
    static final String BAM_ALL_P4 = withScores(BAM_DEFAULT_P4, "0.0 1000.0 900.0")
            + "chr3\t3160\t3190\tr1\tAACG\tIIJK\t2M2S\t3170\tchr1\t160\t190\t+\tmapped\t400.0\n";

    static final String COLL_NOTE_P4 = lines(
            "CHROM\tPOS\tliftover_note\thgold_POS\tqStrand\thg19_CHROM\thg19_POS\thg19_qStrand\thg19_liftoverStatus",
            "chr1\t1150\tn1\tx\ty\tchr1\t150\t+\tmapped");

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    /** Header and rows, each terminated by a newline. */
    static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    /** Appends a tab separated score column (header gets {@code hgold_score}) to a header + rows block. */
    static String withScores(String block, String spaceSeparatedScores) {
        String[] ls = block.split("\n");
        String[] scores = spaceSeparatedScores.split(" ");
        Assert.assertEquals("score count must match row count", ls.length - 1, scores.length);
        StringBuilder sb = new StringBuilder(ls[0]).append("\thgold_score\n");
        for (int i = 1; i < ls.length; i++) {
            sb.append(ls[i]).append('\t').append(scores[i - 1]).append('\n');
        }
        return sb.toString();
    }

    static String lift(String chainFixture, String inputFixture, String opts) {
        return TestUtils.runGorPipe("gor " + inputFixture + " | liftover " + chainFixture + " " + opts);
    }

    static String lift(String inputFixture, String opts) {
        return lift(CHAIN, inputFixture, opts);
    }

    private static String write(String name, String content) throws IOException {
        File f = new File(tmpdir.toFile(), name + ".gor");
        FileUtils.write(f, content, StandardCharsets.UTF_8);
        return f.getAbsolutePath();
    }

    private static String snpRow(String chr, int pos, String id) {
        return chr + "\t" + pos + "\tA\tG\t" + id;
    }

    @BeforeClass
    public static void setUp() throws IOException {
        tmpdir = Files.createTempDirectory("liftover-p0");
        tmpdir.toFile().deleteOnExit();

        CHAIN = write("chain", lines(CHAIN_HEADER,
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000",
                "chr1\t200\t300\tchr1\t1200\t1300\t+\t1000",
                "chr1\t400\t500\tchr1\t5000\t5100\t-\t900",
                "chr1\t600\t700\tchr2\t2100\t2200\t+\t800",
                "chr1\t800\t900\tchr1\t8800\t8900\t+\t500",
                "chr1\t850\t950\tchr1\t9850\t9950\t+\t500",
                "chrM\t300\t3100\tchrM\t298\t3098\t+\t100"));
        CHAIN_OVL = write("chain_ovl", lines(CHAIN_HEADER,
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000",
                "chr1\t150\t250\tchr3\t3150\t3250\t+\t400",
                "chr1\t400\t500\tchr1\t5000\t5100\t-\t900"));
        CHAIN_ZERO = write("chain_zero", lines(CHAIN_HEADER,
                "chr1\t100\t200\tchr1\t1100\t1200\t0\t1000"));

        SNP = write("snp", lines(SNP_HEADER,
                snpRow("chr1", 150, "s01"),
                snpRow("chr1", 200, "s02"),
                snpRow("chr1", 201, "s03"),
                snpRow("chr1", 300, "s04"),
                snpRow("chr1", 350, "dup"),
                snpRow("chr1", 350, "dup"),
                snpRow("chr1", 350, "dup"),
                snpRow("chr1", 401, "s08"),
                snpRow("chr1", 500, "s09"),
                snpRow("chr1", 650, "s10"),
                snpRow("chr1", 875, "s11"),
                snpRow("chr2", 100, "s12"),
                snpRow("chrM", 1000, "s13")));

        VAR = write("var", lines(SNP_HEADER,
                "chr1\t150\tA\tG\tv01",
                "chr1\t160\t\tG\tv02",
                "chr1\t198\tACG\tT\tv03",
                "chr1\t199\tACG\tT\tv04",
                "chr1\t350\tA\tG\tdup",
                "chr1\t350\tA\tG\tdup",
                "chr1\t410\tAC\tG\tv07",
                "chr1\t420\tC\tA,G\tv08",
                "chr1\t425\tAC\tG,TTA\tv09",
                "chr1\t430\tA\t<DEL>\tv10",
                "chr1\t440\t\tT\tv11",
                "chr1\t650\tA\tG\tv12",
                "chr1\t875\tA\tG\tv13",
                "chrM\t1000\tA\tG\tv14"));

        SEG = write("seg", lines(SEG_HEADER,
                "chr1\t120\t180\tg01",
                "chr1\t150\t250\tg02",
                "chr1\t250\t450\tg03",
                "chr1\t350\t360\tdup",
                "chr1\t350\t360\tdup",
                "chr1\t410\t450\tg06",
                "chr1\t610\t690\tg07",
                "chr1\t860\t880\tg08",
                "chrM\t1000\t1100\tg09"));
        SEG3 = write("seg3", lines(SEG3_HEADER,
                "chr1\t120\t180",
                "chr1\t150\t250",
                "chr1\t250\t450",
                "chr1\t350\t360",
                "chr1\t350\t360",
                "chr1\t410\t450",
                "chr1\t610\t690",
                "chr1\t860\t880",
                "chrM\t1000\t1100"));

        EDGE = write("edge", lines(SNP_HEADER,
                "chr1\t100\t\tA\te01",
                "chr1\t100\tA\tG\te02",
                "chr1\t101\t\tA\te03",
                "chr1\t101\tA\tG\te04",
                "chr1\t150\tA\tG\tmdup",
                "chr1\t150\tA\tG\tmdup",
                "chr1\t200\t\tA\te07",
                "chr1\t201\t\tA\te08",
                "chr1\t350\tA\tG\tdup",
                "chr1\t350\tA\tG\tzzz",
                "chr1\t350\tA\tG\tdup",
                "chr1\t500\t\tA\te12",
                "chr1\t501\t\tA\te13"));

        OVLIN = write("ovlin", lines(SNP_HEADER, "chr1\t175\tA\tG\to01"));
        OVLSEG = write("ovlseg", lines(SEG_HEADER, "chr1\t160\t190\to02"));

        REFOPT = write("refopt", lines("Chrom\tPos\tRef\tAlt\tLongRef",
                "chr1\t101\t\tG\tAC",
                "chr1\t195\tA\tG\tACGTACGTAC"));

        COLL = write("coll", lines("CHROM\tPOS\tliftover_note\thgold_POS\tqStrand",
                "chr1\t150\tn1\tx\ty"));

        ZSEG = write("zseg", lines(SEG_HEADER,
                "chr1\t100\t100\tz02",
                "chr1\t150\t150\tz01",
                "chr1\t200\t200\tz03"));

        MA = write("ma", lines(SNP_HEADER, "chr1\t420\tT\tT,GA\tm01"));

        BAM = write("bam", lines("Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS",
                "chr1\t160\t190\tr1\tAACG\tIIJK\t2M2S\t170",
                "chr1\t300\t350\tr3\tAACG\tIIJK\t4M\t310",
                "chr1\t410\t450\tr2\tAACG\tIIJK\t2M2S\t420"));

        MAPSTR_CHAIN = write("mapstr_chain", MAPSTR);
        BIGSCORE_IN = write("bigscore_in", lines(SNP_HEADER, "chr12\t200000\tA\tG\ta1"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FileUtils.deleteDirectory(tmpdir.toFile());
    }

    // ------------------------------------------------------------------------------------------------
    // -snp
    // ------------------------------------------------------------------------------------------------

    // P4-CHANGES: the dup rows no longer collapse; s11 becomes one ambiguous row.
    @Test
    public void snpDefault() {
        Assert.assertEquals(SNP_DEFAULT_P4, lift(SNP, "-snp"));
    }

    @Test
    public void snpBuildPrefix() {
        Assert.assertEquals(lift(SNP, "-snp").replace("hgold_", "hg19_"), lift(SNP, "-snp -build hg19"));
    }

    // P4-CHANGES: dup x3, each with 0.0; the two s11 rows stay, by design under -all (N14).
    @Test
    public void snpAll() {
        Assert.assertEquals(SNP_ALL_P4, lift(SNP, "-snp -all"));
    }

    // ------------------------------------------------------------------------------------------------
    // -var
    // ------------------------------------------------------------------------------------------------

    // P4-CHANGES: dup x2; v13 becomes ambiguous; v08, v09 and v10 ALT become per-allele (N16).
    @Test
    public void varDefault() {
        Assert.assertEquals(VAR_DEFAULT_P4, lift(VAR, "-var"));
    }

    // P4-CHANGES: dup x2, and v08/v09/v10 as in varDefault.
    @Test
    public void varAll() {
        Assert.assertEquals(VAR_ALL_P4, lift(VAR, "-var -all"));
    }

    // ------------------------------------------------------------------------------------------------
    // -seg
    // ------------------------------------------------------------------------------------------------

    // P4-CHANGES: dup x2; g08 becomes ambiguous.
    @Test
    public void segDefault() {
        Assert.assertEquals(SEG_DEFAULT_P4, lift(SEG, "-seg"));
    }

    // P4-CHANGES: the 350-360 row x2; 860-880 becomes ambiguous.
    @Test
    public void seg3Default() {
        Assert.assertEquals(SEG3_DEFAULT_P4, lift(SEG3, "-seg"));
    }

    // P4-CHANGES: dup x2, each with 0.0.
    @Test
    public void segAll() {
        Assert.assertEquals(SEG_ALL_P4, lift(SEG, "-seg -all"));
    }

    // P4-CHANGES: the 350-360 row x2, each with 0.0.
    @Test
    public void seg3All() {
        Assert.assertEquals(SEG3_ALL_P4, lift(SEG3, "-seg -all"));
    }

    // ------------------------------------------------------------------------------------------------
    // Empty REF edge cases (unchanged by P4)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void edgeVar() {
        Assert.assertEquals(lines(
                "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
                "chr1\t0\t\tA\te01\tchr1\t100\t\tunmapped",
                "chr1\t0\t\tA\te03\tchr1\t101\t\tunmapped",
                "chr1\t0\t\tA\te08\tchr1\t201\t\tunmapped",
                "chr1\t0\t\tA\te13\tchr1\t501\t\tunmapped",
                "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped",
                "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped",
                "chr1\t0\tA\tG\te02\tchr1\t100\t\tunmapped",
                "chr1\t0\tA\tG\tzzz\tchr1\t350\t\tunmapped",
                "chr1\t1101\tA\tG\te04\tchr1\t101\t+\tmapped",
                "chr1\t1150\tA\tG\tmdup\tchr1\t150\t+\tmapped",
                "chr1\t1150\tA\tG\tmdup\tchr1\t150\t+\tmapped",
                "chr1\t1200\t\tA\te07\tchr1\t200\t+\tmapped",
                "chr1\t5002\t\tT\te12\tchr1\t500\t-\tmapped"), lift(EDGE, "-var"));
    }

    @Test
    public void edgeSnp() {
        Assert.assertEquals(lines(
                "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus",
                "chr1\t0\t\tA\te01\tchr1\t100\t\tunmapped",
                "chr1\t0\t\tA\te13\tchr1\t501\t\tunmapped",
                "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped",
                "chr1\t0\tA\tG\tdup\tchr1\t350\t\tunmapped",
                "chr1\t0\tA\tG\te02\tchr1\t100\t\tunmapped",
                "chr1\t0\tA\tG\tzzz\tchr1\t350\t\tunmapped",
                "chr1\t1101\t\tA\te03\tchr1\t101\t+\tmapped",
                "chr1\t1101\tA\tG\te04\tchr1\t101\t+\tmapped",
                "chr1\t1150\tA\tG\tmdup\tchr1\t150\t+\tmapped",
                "chr1\t1150\tA\tG\tmdup\tchr1\t150\t+\tmapped",
                "chr1\t1200\t\tA\te07\tchr1\t200\t+\tmapped",
                "chr1\t1201\t\tA\te08\tchr1\t201\t+\tmapped",
                "chr1\t5001\t\tA\te12\tchr1\t500\t-\tmapped"), lift(EDGE, "-snp"));
    }

    // ------------------------------------------------------------------------------------------------
    // Overlapping chain blocks with unequal scores (unchanged by P4)
    // ------------------------------------------------------------------------------------------------

    private static final String OVL_SNP_HEADER =
            "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus";
    private static final String OVL_SEG_HEADER =
            "Chrom\tStart\tEnd\tName\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus";

    @Test
    public void overlapUnequalScores() {
        String snpExpected = lines(OVL_SNP_HEADER, "chr1\t1175\tA\tG\to01\tchr1\t175\t+\tmapped");
        Assert.assertEquals(snpExpected, lift(CHAIN_OVL, OVLIN, "-snp"));
        Assert.assertEquals(snpExpected, lift(CHAIN_OVL, OVLIN, "-var"));

        String snpAllExpected = lines(OVL_SNP_HEADER + "\thgold_score",
                "chr1\t1175\tA\tG\to01\tchr1\t175\t+\tmapped\t1000.0",
                "chr3\t3175\tA\tG\to01\tchr1\t175\t+\tmapped\t400.0");
        Assert.assertEquals(snpAllExpected, lift(CHAIN_OVL, OVLIN, "-snp -all"));
        Assert.assertEquals(snpAllExpected, lift(CHAIN_OVL, OVLIN, "-var -all"));

        Assert.assertEquals(lines(OVL_SEG_HEADER, "chr1\t1160\t1190\to02\tchr1\t160\t190\t+\tmapped"),
                lift(CHAIN_OVL, OVLSEG, "-seg"));
        Assert.assertEquals(lines(OVL_SEG_HEADER + "\thgold_score",
                        "chr1\t1160\t1190\to02\tchr1\t160\t190\t+\tmapped\t1000.0",
                        "chr3\t3160\t3190\to02\tchr1\t160\t190\t+\tmapped\t400.0"),
                lift(CHAIN_OVL, OVLSEG, "-seg -all"));
    }

    // ------------------------------------------------------------------------------------------------
    // -ref
    // ------------------------------------------------------------------------------------------------

    @Test
    public void refOption() {
        Assert.assertEquals(REFOPT_DEFAULT_LEGACY, lift(REFOPT, "-var"));
        // P4-CHANGES: the 101 row maps. P4 takes the span from LongRef, while before P4 the JOIN span came from
        // the empty Ref column and the strict empty-span overlap rejected it.
        Assert.assertEquals(REFOPT_LONGREF_P4, lift(REFOPT, "-var -ref LongRef"));
    }

    // ------------------------------------------------------------------------------------------------
    // Zero length segments, multi allelic, bam
    // ------------------------------------------------------------------------------------------------

    // A zero-length -seg row is a junction (N12); the rule is the strict tStart < x < tEnd.
    @Test
    public void zeroLengthSeg() {
        Assert.assertEquals(lines(
                "Chrom\tStart\tEnd\tName\thgold_Chrom\thgold_Start\thgold_End\thgold_qStrand\thgold_liftoverStatus",
                "chr1\t0\t1\tz02\tchr1\t100\t100\t\tunmapped",
                "chr1\t0\t1\tz03\tchr1\t200\t200\t\tunmapped",
                "chr1\t1150\t1150\tz01\tchr1\t150\t150\t+\tmapped"), lift(ZSEG, "-seg"));
    }

    // P4-CHANGES: per-allele (N16) gives chr1\t5081\tA\tA,TC\tm01\tchr1\t420\t-\tmapped.
    @Test
    public void multiAllelicRevcompl() {
        Assert.assertEquals(MA_P4, lift(MA, "-var"));
    }

    // P4-CHANGES: r2 coordinates and MPOS, r3 MPOS (N15). r1 overlaps both + blocks and keeps only the higher score.
    @Test
    public void bamDefault() {
        Assert.assertEquals(BAM_DEFAULT_P4, lift(CHAIN_OVL, BAM, "-bam"));
    }

    // P4-CHANGES: as bamDefault.
    @Test
    public void bamAll() {
        Assert.assertEquals(BAM_ALL_P4, lift(CHAIN_OVL, BAM, "-bam -all"));
    }

    // ------------------------------------------------------------------------------------------------
    // Chain loading quirks
    // ------------------------------------------------------------------------------------------------

    // P4-CHANGES: the load fails with "invalid strand '0'" (N13).
    @Test
    public void zeroStrandChain() {
        assertZeroStrandRejected("-snp");
    }

    // P4-CHANGES: the load fails with "invalid strand '0'" (N13).
    @Test
    public void zeroStrandChainAll() {
        assertZeroStrandRejected("-snp -all");
    }

    private static void assertZeroStrandRejected(String opts) {
        try {
            lift(CHAIN_ZERO, OVLIN, opts);
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("invalid strand '0'"));
        }
    }

    // P4-CHANGES: a nested chain runs on the engine (N7), so it follows snpDefault to SNP_DEFAULT_P4.
    @Test
    public void nestedChainQuery() {
        Assert.assertEquals(SNP_DEFAULT_P4,
                TestUtils.runGorPipe("gor " + SNP + " | liftover <(gor " + CHAIN + ") -snp"));
    }

    // Pins how a score above Int range prints in hgold_score. The native engine must print scores the same way.
    @Test
    public void allScoreAboveInt() {
        Assert.assertEquals(lines(
                "Chrom\tPos\tRef\tAlt\tId\thgold_Chrom\thgold_Pos\thgold_qStrand\thgold_liftoverStatus\thgold_score",
                "chr12\t90834\tA\tG\ta1\tchr12\t200000\t+\tmapped\t1.234103462E10"), lift(MAPSTR_CHAIN, BIGSCORE_IN, "-snp -all"));
    }

    // ------------------------------------------------------------------------------------------------
    // Option errors and column collisions
    // ------------------------------------------------------------------------------------------------

    @Test
    public void noTypeFlagDefaultsToSeg() {
        try {
            lift(SNP, "");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Illegal stop position in column #3"));
        }
    }

    @Test
    public void twoTypeFlags() {
        try {
            lift(SNP, "-snp -var");
            Assert.fail("expected GorParsingException");
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("specify only one liftover type"));
        }
    }

    // P4-CHANGES: a GorParsingException naming hgold_POS. Before P4 it was a GorDataException from RENAME,
    // raised at parse time (Rename.scala:74).
    @Test
    public void collisionDuplicatePos() {
        try {
            lift(COLL, "-snp");
            Assert.fail("expected GorParsingException");
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().getName(), e instanceof GorParsingException);
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("hgold_POS"));
        }
    }

    // P4-CHANGES: liftover_note will be kept.
    @Test
    public void collisionRenamesInput() {
        Assert.assertEquals(COLL_NOTE_P4, lift(COLL, "-snp -build hg19"));
    }

    // ------------------------------------------------------------------------------------------------
    // P4 additions: -bam reads, invalid intervals, chain validation
    // ------------------------------------------------------------------------------------------------

    private static final String BAM_MRNM_HEADER = "Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMRNM\tMPOS";

    // N12, N15: End == Pos - 1 is a junction, mapped strictly inside block [100,200); MPOS 0 stays 0.
    @Test
    public void bamJunctionRead() throws IOException {
        String in = write("bam_junction", lines("Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS",
                "chr1\t151\t150\tj1\tAC\tII\t2I\t0"));
        Assert.assertEquals(lines(BAM_OUT_HEADER,
                "chr1\t1151\t1150\tj1\tAC\tII\t2I\t0\tchr1\t151\t150\t+\tmapped"), lift(in, "-bam"));
    }

    // N15: a mate on another chrom (MRNM neither '=' nor the read's chrom) gets MPOS 0; '=' and '1' (chr1
    // without the chr prefix) lift MPOS.
    @Test
    public void bamMateOtherChrom() throws IOException {
        String in = write("bam_mrnm", lines(BAM_MRNM_HEADER,
                "chr1\t160\t190\tm1\tAACG\tIIJK\t4M\tchr2\t170",
                "chr1\t160\t190\tm2\tAACG\tIIJK\t4M\t=\t170",
                "chr1\t160\t190\tm3\tAACG\tIIJK\t4M\t1\t170"));
        Assert.assertEquals(lines(BAM_MRNM_HEADER + "\thgold_Chrom\thgold_Pos\thgold_End\thgold_qStrand"
                        + "\thgold_liftoverStatus",
                "chr1\t1160\t1190\tm1\tAACG\tIIJK\t4M\tchr2\t0\tchr1\t160\t190\t+\tmapped",
                "chr1\t1160\t1190\tm2\tAACG\tIIJK\t4M\t=\t1170\tchr1\t160\t190\t+\tmapped",
                "chr1\t1160\t1190\tm3\tAACG\tIIJK\t4M\t1\t1170\tchr1\t160\t190\t+\tmapped"),
                lift(in, "-bam"));
    }

    @Test
    public void bamMissingSeqColumn() {
        try {
            lift(SNP, "-bam");
            Assert.fail("expected GorParsingException");
        } catch (GorParsingException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("SEQ"));
        }
    }

    // N12: end < start (-seg), end < start - 1 (-bam) is an error, no longer an unmapped row.
    @Test
    public void reversedIntervalThrows() throws IOException {
        String seg = write("reversed_seg", lines(SEG_HEADER, "chr1\t150\t149\tx"));
        try {
            lift(seg, "-seg");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(),
                    e.getMessage().startsWith("LIFTOVER: invalid interval [150,149] at chr1:150"));
        }
        String bam = write("reversed_bam", lines("Chrom\tPos\tEnd\tQName\tSEQ\tQUAL\tCIGAR\tMPOS",
                "chr1\t151\t149\tx\tA\tI\t1M\t0"));
        try {
            lift(bam, "-bam");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(),
                    e.getMessage().startsWith("LIFTOVER: invalid interval [151,149] at chr1:151"));
        }
    }

    // N9: an unsorted chain fails the load. Before P4 the same query silently gave unmapped rows.
    @Test
    public void invalidChainRejectedP4() throws IOException {
        String unsorted = write("chain_unsorted", lines(CHAIN_HEADER,
                "chr1\t200\t300\tchr1\t1200\t1300\t+\t1000",
                "chr1\t100\t200\tchr1\t1100\t1200\t+\t1000"));
        try {
            lift(unsorted, SNP, "-snp");
            Assert.fail("expected GorDataException");
        } catch (GorDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Liftover chain"));
        }
    }
}
