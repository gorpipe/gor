.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs | GROUP 100000 -count -gc snptype
   | GROUP chrom -max -min -avg -ic allCount -gc snptype

.. list-table:: #dbsnp# chr21: SNP types (max, min, avg)
   :widths: 5  5  5 5 5 5  5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - snptype
     - min_allCount
     - max_allCount
     - avg_allCount
   * - chr21
     - 0
     - 48129895
     - A/C
     - 37
     - 936
     - 327.885154
   * - chr21
     - 0
     - 48129895
     - A/G
     - 66
     - 2337
     - 1120.08963
   * - chr21
     - 0
     - 48129895
     - A/T
     - 24
     - 1279
     - 307.28011
   * - chr21
     - 0
     - 48129895
     - C/A
     - 61
     - 1422
     - 428.04761
   * - chr21
     - 0
     - 48129895
     - C/G
     - 39
     - 1055
     - 374.92997
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...
     - ...