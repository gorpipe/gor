:orphan:

.. list-table:: #dbsnp# (with a replaced and calculated column relabeled as "refLength")
   :widths: 5  10 5  5 5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - refLength
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - 28
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - 25
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - 22
   * - chr1
     - 12940
     - AAACA
     - A
     - 5
   * - chr1
     - 13421
     - A
     - AGAGA
     - 1
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - 22

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | REPLACE rsIDs len(reference) | RENAME #5 refLength | TOP 6