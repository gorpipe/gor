:orphan:

.. code-block:: gor

   gor -p chr16:89978525-89978550 #dbsnp# | JOIN -snpseg #exons# -f 10 | WHERE gene_symbol = 'MC1R' | SELECT 1-10

.. list-table:: Joining #dbsnp# and #exons#
   :widths: 5  10 5  5  15 5  10 10 5  15
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - distance
     - chromstart
     - chromend
     - gene_symbol
     - exon
   * - chr16
     - 89978525
     - C
     - T
     - rs908913173
     - 2
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523
   * - chr16
     - 89978529
     - A
     - T
     - rs187669455
     - 0
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523
   * - chr16
     - 89978533
     - C
     - T
     - rs111341249
     - 0
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523
   * - chr16
     - 89978543
     - A
     - G
     - rs112125468
     - 0
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523
   * - chr16
     - 89978545
     - T
     - A
     - rs62635282
     - 0
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523