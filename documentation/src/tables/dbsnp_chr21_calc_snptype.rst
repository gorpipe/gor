:orphan:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs

.. list-table:: #dbsnp# chr21 with SNP type
   :widths: 5  5 5  5  5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - snptype
   * - chr21
     - 9411199
     - T
     - C
     - T/C
   * - chr21
     - 9411236
     - G
     - A
     - G/A
   * - chr21
     - 9411239
     - G
     - A
     - G/A
   * - chr21
     - 9411242
     - C
     - A
     - C/A
   * - chr21
     - 9411243
     - A
     - C
     - A/C
