:orphan:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | JOIN -snpseg #exons# | CALC snptype reference+'/'+allele
   | HIDE rsIDs | GROUP chrom -gc gene_symbol -count

.. list-table:: Aggregation of SNPs for each Gene
   :widths: 5  5  5 5 5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - gene_symbol
     - allCount
   * - chr21
     - 0
     - 48129895
     - 7SK
     - 107
   * - chr21
     - 0
     - 48129895
     - ABCC13
     - 1654
   * - chr21
     - 0
     - 48129895
     - ABCG1
     - 5180
   * - chr21
     - 0
     - 48129895
     - ADAMTS1
     - 1955
   * - chr21
     - 0
     - 48129895
     - ADARB1
     - 1103
   * - chr21
     - 0
     - 48129895
     - ADAMTS5
     - 5693
   * - ...
     - ...
     - ...
     - ...
     - ...
