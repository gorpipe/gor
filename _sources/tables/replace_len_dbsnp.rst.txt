.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | REPLACE reference len(reference) | REPLACE allele len(allele) | TOP 6

.. list-table:: #dbsnp# (with calculated columns for lengths replacing reference and allele)
   :widths: 5  10 5  5 15
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
   * - chr1
     - 10233
     - 28
     - 1
     - rs200462216
   * - chr1
     - 10332
     - 25
     - 1
     - rs201106462
   * - chr1
     - 10621
     - 22
     - 1
     - rs376342519
   * - chr1
     - 12940
     - 5
     - 1
     - rs756849893
   * - chr1
     - 13421
     - 1
     - 5
     - rs777038595
   * - chr1
     - 15189
     - 22
     - 1
     - rs768510816

