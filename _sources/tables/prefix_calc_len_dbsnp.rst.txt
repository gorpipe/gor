.. list-table:: #dbsnp# (with a calculated column showing the reference length)
   :widths: 5  10 5  5  15 5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - calc_refLength
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
     - 28
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
     - 25
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
     - 22
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
     - 5
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
     - 1
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816
     - 22

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | CALC refLength len(reference) | PREFIX refLength calc | TOP 6