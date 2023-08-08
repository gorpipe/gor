:orphan:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs | GROUP chrom -count -gc snptype

.. list-table:: #dbsnp# chr21: SNP types grouped over the chromosome
   :widths: 5  5  5 5 5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - snptype
     - allCount
   * - chr21
     - 0
     - 48129895
     - A/C
     - 117055
   * - chr21
     - 0
     - 48129895
     - A/G
     - 399872
   * - chr21
     - 0
     - 48129895
     - A/T
     - 109699
   * - chr21
     - 0
     - 48129895
     - C/A
     - 152813
   * - chr21
     - 0
     - 48129895
     - C/G
     - 133850
