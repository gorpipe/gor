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

package org.gorpipe.model.gor

object GorReferenceBuildDefaults {

  // Generic
  // Build 36, hg18
  val buildSize_generic = scala.collection.immutable.Map(
    "chr1" -> 250000000,
    "chr2" -> 250000000,
    "chr3" -> 200000000,
    "chr4" -> 200000000,
    "chr5" -> 200000000,
    "chr6" -> 200000000,
    "chr7" -> 200000000,
    "chr8" -> 150000000,
    "chr9" -> 150000000,
    "chr10" -> 150000000,
    "chr11" -> 150000000,
    "chr12" -> 150000000,
    "chr13" -> 150000000,
    "chr14" -> 150000000,
    "chr15" -> 150000000,
    "chr16" -> 100000000,
    "chr17" -> 100000000,
    "chr18" -> 100000000,
    "chr19" -> 100000000,
    "chr20" -> 100000000,
    "chr21" -> 100000000,
    "chr22" -> 100000000,
    "chrX" -> 200000000,
    "chrY" -> 100000000,
    "chrM" -> 20000,
    "chrXY" -> 1)

  val buildSplit_generic = scala.collection.immutable.Map.empty[String, Int]

  // Build 36, hg18
  val buildSize_hg18 = scala.collection.immutable.Map(
    "chr1" -> 247249719,
    "chr2" -> 242951149,
    "chr3" -> 199501827,
    "chr4" -> 191273063,
    "chr5" -> 180857866,
    "chr6" -> 170899992,
    "chr7" -> 158821424,
    "chr8" -> 146274826,
    "chr9" -> 140273252,
    "chr10" -> 135374737,
    "chr11" -> 134452384,
    "chr12" -> 132349534,
    "chr13" -> 114142980,
    "chr14" -> 106368585,
    "chr15" -> 100338915,
    "chr16" -> 88827254,
    "chr17" -> 78774742,
    "chr18" -> 76117153,
    "chr19" -> 63811651,
    "chr20" -> 62435964,
    "chr21" -> 46944323,
    "chr22" -> 49691432,
    "chrX" -> 154913754,
    "chrY" -> 57772954,
    "chrM" -> 16571,
    "chrXY" -> 1)

  val buildSplit_hg18 = scala.collection.immutable.Map(
    "chr1" -> 124000000,
    "chr10" -> 40350000,
    "chr11" -> 52750000,
    "chr12" -> 35000000,
    "chr2" -> 93100000,
    "chr3" -> 91350000,
    "chr4" -> 50750000,
    "chr5" -> 47650000,
    "chr6" -> 60125000,
    "chr7" -> 59330000,
    "chr8" -> 45500000,
    "chr9" -> 53700000,
    "chrX" -> 60000000)

  // Build, hg19
  val buildSize_hg19 = scala.collection.immutable.Map(
    "chr1" -> 249250621,
    "chr2" -> 243199373,
    "chr3" -> 198022430,
    "chr4" -> 191154276,
    "chr5" -> 180915260,
    "chr6" -> 171115067,
    "chr7" -> 159138663,
    "chr8" -> 146364022,
    "chr9" -> 141213431,
    "chr10" -> 135534747,
    "chr11" -> 135006516,
    "chr12" -> 133851895,
    "chr13" -> 115169878,
    "chr14" -> 107349540,
    "chr15" -> 102531392,
    "chr16" -> 90354753,
    "chr17" -> 81195210,
    "chr18" -> 78077248,
    "chr19" -> 59128983,
    "chr20" -> 63025520,
    "chr21" -> 48129895,
    "chr22" -> 51304566,
    "chrX" -> 155270560,
    "chrY" -> 59373566,
    "chrM" -> 16571,
    "chrXY" -> 1)

  val buildSplit_hg19 = scala.collection.immutable.Map(
    "chr1" -> 124000000,
    "chr10" -> 40350000,
    "chr11" -> 52750000,
    "chr12" -> 35000000,
    "chr2" -> 93100000,
    "chr3" -> 91350000,
    "chr4" -> 50750000,
    "chr5" -> 47650000,
    "chr6" -> 60125000,
    "chr7" -> 59330000,
    "chr8" -> 45500000,
    "chr9" -> 53700000,
    "chrX" -> 60000000)

  val buildSize_hg38 = scala.collection.immutable.Map(
    "chr1" -> 248956422,
    "chr2" -> 242193529,
    "chr3" -> 198295559,
    "chr4" -> 190214555,
    "chr5" -> 181538259,
    "chr6" -> 170805979,
    "chr7" -> 159345973,
    "chr8" -> 145138636,
    "chr9" -> 138394717,
    "chr10" -> 133797422,
    "chr11" -> 135086622,
    "chr12" -> 133275309,
    "chr13" -> 114364328,
    "chr14" -> 107043718,
    "chr15" -> 101991189,
    "chr16" -> 90338345,
    "chr17" -> 83257441,
    "chr18" -> 80373285,
    "chr19" -> 58617616,
    "chr20" -> 64444167,
    "chr21" -> 46709983,
    "chr22" -> 50818468,
    "chrX" -> 156040895,
    "chrY" -> 57227415,
    "chrM" -> 16569,
    "chrXY" -> 1)

  val buildSplit_hg38 = scala.collection.immutable.Map(
    "chr1" -> 123400000,
    "chr10" -> 39800000,
    "chr11" -> 53400000,
    "chr12" -> 35500000,
    "chr2" -> 93900000,
    "chr3" -> 90900000,
    "chr4" -> 50000000,
    "chr5" -> 48800000,
    "chr6" -> 59800000,
    "chr7" -> 60100000,
    "chr8" -> 45200000,
    "chr9" -> 43000000,
    "chrX" -> 61000000)
}
