.. list-table:: #exons# for MC1R
   :widths: 5  5  5  10 10
   :header-rows: 1

   * - chrom
     - chromstart
     - chromend
     - gene_symbol
     - exon
   * - chr16
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523
   * - chr16
     - 89979639
     - 89981576
     - MC1R
     - ENSE00002202946
   * - chr16
     - 89979825
     - 89981548
     - MC1R
     - ENSE00002508940
   * - chr16
     - 89984283
     - 89984455
     - MC1R
     - ENSE00002445563
   * - chr16
     - 89984286
     - 89987385
     - MC1R
     - ENSE00002458332
   * - chr16
     - 89985258
     - 89986616
     - MC1R
     - ENSE00002477640
   * - chr16
     - 89986997
     - 89987381
     - MC1R
     - ENSE00002535013

.. code-block:: gor

   gor #exons# | WHERE gene_symbol LIKE 'MC1R*' | SELECT 1-5