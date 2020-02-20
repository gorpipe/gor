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

package gorsat.analysis;

public class AnalysisTestData {

    static final String DBSNP_ALL_CHROMOSOMES_HEADER = "Chrom\tPOS\treference\tallele\tdifferentrsIDs";
    static final String[] DBSNP_ALL_CHROMOSOMES = {
            "chr1\t10179\tC\tCC\trs367896724",
            "chr1\t10250\tA\tC\trs199706086",
            "chr10\t60803\tT\tG\trs536478188",
            "chr10\t61023\tC\tG\trs370414480",
            "chr11\t61248\tG\tA\trs367559610",
            "chr11\t66295\tC\tA\trs61869613",
            "chr12\t60162\tC\tG\trs544101329",
            "chr12\t60545\tA\tT\trs570991495",
            "chr13\t19020013\tC\tT\trs181615907",
            "chr13\t19020145\tG\tT\trs28970552",
            "chr14\t19000009\tG\tC\trs373840300",
            "chr14\t19000060\tC\tG\trs28973059",
            "chr15\t20000018\tT\tC\trs374194708",
            "chr15\t20000043\tA\tG\trs375627562",
            "chr16\t60008\tA\tT\trs374973230",
            "chr16\t60087\tC\tG\trs62028703",
            "chr17\t186\tG\tA\trs547289895",
            "chr17\t460\tG\tA\trs554808397",
            "chr18\t10025\tC\tA\trs140072522",
            "chr18\t10147\tTTAACCCTAACCCTT\tT\trs199766986",
            "chr19\t62155\tA\tG\trs201739106",
            "chr19\t70443\tA\tG\trs373133808",
            "chr2\t10181\tA\tG\trs572458259",
            "chr2\t10200\tA\tT\trs563059835",
            "chr20\t60568\tA\tC\trs533509214",
            "chr20\t60808\tG\tA\trs534548532",
            "chr21\t9411302\tG\tT\trs531010746",
            "chr21\t9411384\tC\tT\trs554702871",
            "chr22\t16050036\tA\tC\trs374742143",
            "chr22\t16050527\tC\tA\trs587769434",
            "chr3\t60197\tG\tA\trs115479960",
            "chr3\t60419\tT\tG\trs558166806",
            "chr4\t10035\tT\tA\trs150076536",
            "chr4\t10130\tC\tCC\trs572745514",
            "chr5\t10058\tC\tA\trs547354230",
            "chr5\t10066\tC\tCAA\trs546237653",
            "chr6\t64163\tT\tC\trs199606246",
            "chr6\t70838\tT\tC\trs111875673",
            "chr7\t10367\tG\tA\trs201460812",
            "chr7\t13591\tT\tG\trs201325637",
            "chr8\t10059\tC\tT\trs371829072",
            "chr8\t10467\tC\tG\trs199753717",
            "chr9\t10047\tC\tT\trs567034784",
            "chr9\t10097\tCCA\tC\trs201803828",
            "chrX\t2699625\tA\tG\trs6655038",
            "chrX\t2699968\tA\tG\trs2306737",
            "chrY\t10003\tA\tC\trs375039031",
            "chrY\t10069\tT\tA\trs111065272"
    };

    static final String GENES_ALL_CHROMOSOMES_HEADER = "Chrom\tgene_start\tgene_end\tGene_Symbol";
    static final String[] GENES_ALL_CHROMOSOMES = {
            "chr1\t11868\t14412\tDDX11L1",
            "chr1\t14362\t29806\tWASH7P",
            "chr10\t60000\t60544\tRP11-631M21.1",
            "chr10\t90651\t92824\tRP11-631M21.6",
            "chr11\t75779\t76143\tRP11-304M2.1",
            "chr11\t86611\t87605\tOR4F2P",
            "chr12\t67606\t69138\tRP11-598F7.1",
            "chr12\t73184\t73322\tAC215219.1",
            "chr13\t19041311\t19059588\tZNF962P",
            "chr13\t19114636\t19124837\tLINC00349",
            "chr14\t19109938\t19118336\tRP11-754I20.1",
            "chr14\t19172102\t19188741\tRP11-754I20.3",
            "chr15\t20083768\t20093074\tRP11-79C23.1",
            "chr15\t20104586\t20104812\tRP11-173D3.3",
            "chr16\t61552\t64093\tDDX11L10",
            "chr16\t64042\t69452\tWASH4P",
            "chr17\t4960\t5048\tAC108004.1",
            "chr17\t5809\t31427\tDOC2B",
            "chr18\t11102\t15928\tAP005530.1",
            "chr18\t14194\t16898\tAP005530.2",
            "chr19\t60104\t70966\tAC008993.5",
            "chr19\t68402\t69146\tAC008993.2",
            "chr2\t38813\t46870\tFAM110C",
            "chr2\t197568\t202605\tAC079779.7",
            "chr20\t68350\t77174\tDEFB125",
            "chr20\t123009\t126392\tDEFB126",
            "chr21\t9683190\t9683272\tCR381670.1",
            "chr21\t9825831\t9826011\tMIR3648",
            "chr22\t16062156\t16063236\tLA16c-4G1.3",
            "chr22\t16076051\t16076172\tLA16c-4G1.4",
            "chr3\t65430\t66175\tAY269186.1",
            "chr3\t95030\t96029\tAY269186.2",
            "chr4\t48990\t50018\tZ95704.4",
            "chr4\t53178\t88099\tZNF595",
            "chr5\t58312\t59030\tRP11-811I15.1",
            "chr5\t92265\t139978\tCTD-2231H16.1",
            "chr6\t105918\t106856\tRP1-24O22.1",
            "chr6\t131909\t144885\tLINC00266-3",
            "chr7\t19756\t35479\tAC093627.6",
            "chr7\t70971\t71835\tAC093627.7",
            "chr8\t14090\t14320\tAC144568.4",
            "chr8\t22600\t29428\tAC144568.2",
            "chr9\t11055\t11620\tAL928970.1",
            "chr9\t11986\t14522\tDDX11L5",
            "chrM\t576\t647\tJ01415.2",
            "chrM\t647\t1601\tJ01415.23",
            "chrX\t170409\t172712\tLINC00108",
            "chrX\t192988\t220023\tPLCXD1",
            "chrY\t2654895\t2655740\tSRY",
            "chrY\t2657867\t2658369\tRNASEH2CP1"
    };
}
