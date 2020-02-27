.. code-block:: gor

   gor -p chr21 #VEP# | WHERE Max_Impact = 'HIGH' | SELECT 1-7

.. list-table:: #wgsvars# for a single individual (C416TO_INDEX)
   :widths: 10 10 5  5  5 25  10
   :header-rows: 1

   * - Chrom
     - Pos
     - Reference
     - Call
     - Max_Impact
     - Biotype
     - Gene_Symbol
   * - chr21
     - 9909279
     - T
     - A
     - HIGH
     - processed_transcript_KNOWN,transcribed_unprocessed_pseudogene_KNOWN
     - TEKT4P2
   * - chr21
     - 9911927
     - C
     - A
     - HIGH
     - transcribed_unprocessed_pseudogene_KNOWN
     - TEKT4P2
   * - chr21
     - 10862623
     - T
     - C
     - HIGH
     - IG_V_gene_KNOWN
     - IGHV1OR21-1
   * - chr21
     - 10862624
     - G
     - T
     - HIGH
     - IG_V_gene_KNOWN
     - IGHV1OR21-1
   * - chr21
     - 10862629
     - G
     - A
     - HIGH
     - IG_V_gene_KNOWN
     - IGHV1OR21-1
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...
     - ...