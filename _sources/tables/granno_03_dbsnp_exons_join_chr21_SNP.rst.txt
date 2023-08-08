:orphan:

.. code-block:: gor

   gor -p chr21 #dbsnp# | JOIN -snpseg #exons#
   | WHERE len(reference)=1 AND len(allele)=1 | CALC snptype reference+'/'+allele
   | GRANNO gene -range -gc gene_symbol -ic chromstart,chromend -max -min
   | SELECT 1-4,chromstart,chromend,gene_symbol,snptype-

.. list-table:: Aggregation of SNPs for each gene with GRANNO command
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - chromstart
     - chromend
     - gene_symbol
     - snptype
     - min_chromstart
     - max_chromstart
     - min_chromend
     - max_chromend
   * - chr21
     - 9683195
     - G
     - A
     - 9683190
     - 9683272
     - CR381670.1
     - G/A
     - 9683190
     - 9683190
     - 9683272
     - 9683272
   * - chr21
     - 9683199
     - C
     - G
     - 9683190
     - 9683272
     - CR381670.1
     - C/G
     - 9683190
     - 9683190
     - 9683272
     - 9683272
   * - chr21
     - 9683201
     - G
     - T
     - 9683190
     - 9683272
     - CR381670.1
     - G/T
     - 9683190
     - 9683190
     - 9683272
     - 9683272
