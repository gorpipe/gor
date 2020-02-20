.. code-block:: gor

   gor -p chr21 #dbsnp# | JOIN -snpseg #exons#
   | WHERE len(reference)=1 AND len(allele)=1 | CALC snptype reference+'/'+allele
   | GRANNO gene -range -gc gene_symbol -ic chromstart,chromend -max -min
   | GROUP chrom -gc min_chromstart,max_chromend,gene_symbol,snptype -count
   | SELECT Chrom,min_chromstart- | SORT chrom

.. list-table:: Aggregation of SNPs using both GRANNO and GROUP, genomic-ordered stream
   :widths: 5  5  5 5 5 5
   :header-rows: 1

   * - Chrom
     - min_chromstart
     - max_chromend
     - gene_symbol
     - snptype
     - allCount
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - A/C
     - 1
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - C/G
     - 1
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - C/T
     - 2
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - G/A
     - 1
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - G/T
     - 3
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...