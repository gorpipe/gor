.. list-table:: #dbsnp# (showing different Indel examples)
   :widths: 5  10 5  5  15
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | TOP 6
