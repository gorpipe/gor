:orphan:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | JOIN -snpseg #exons# | CALC snptype reference+'/'+allele
   | HIDE rsIDs | GROUP chrom -gc gene_symbol -count

.. list-table:: Aggregation of SNPs for each gene with position information
   :widths: 5  5  5 5 5 5  5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - chromstart
     - chromend
     - gene_symbol
     - allCount
   * - chr21
     - 0
     - 48129895
     - 10199942
     - 10200025
     - CR381653.1
     - 9
   * - chr21
     - 0
     - 48129895
     - 10380379
     - 10380661
     - RN7SL52P
     - 13
   * - chr21
     - 0
     - 48129895
     - 10385952
     - 10386047
     - SNORA70
     - 9
   * - chr21
     - 0
     - 48129895
     - 10475514
     - 10476061
     - bP-21201H5.1
     - 57
   * - chr21
     - 0
     - 48129895
     - 10862621
     - 10862667
     - IGHV1OR21-1
     - 64
   * - chr21
     - 0
     - 48129895
     - 10862750
     - 10863057
     - IGHV1OR21-1
     - 243
   * - chr21
     - 0
     - 48129895
     - 10862750
     - 10863067
     - IGHV1OR21-1
     - 250
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...
     - ...
