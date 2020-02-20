.. list-table:: #dbsnp# (with a calculated column showing the reference length)
   :widths: 5  10 5  5  15 5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - rownum
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
     - 1
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
     - 2
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
     - 3
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
     - 4
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
     - 5
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816
     - 6

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | ROWNUM | TOP 6