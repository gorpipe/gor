.. code-block:: gor

   gor -p chr21 #wgsVars# -f C416TO_INDEX | SELECT 1-4
   | VARJOIN #VEP# | WHERE (Max_Impact = 'HIGH') AND Reference != Referencex | SELECT 1-10

.. list-table:: #wgsvars# for a single individual (C416TO_INDEX)
   :widths: 10 10 5  5  10 5  5  5 25  10
   :header-rows: 1

   * - Chrom
     - Pos
     - Reference
     - Call
     - Posx
     - Referencex
     - Callx
     - Max_Impact
     - Biotype
     - Gene_Symbol
   * - chr21
     - 28215836
     - GC
     - G
     - 28215837
     - CC
     - C
     - HIGH
     - protein_coding_PUTATIVE
     - ADAMTS1
   * - chr21
     - 34948684
     - G
     - GA
     - 34948685
     - A
     - AA
     - HIGH
     - protein_coding_KNOWN,protein_coding_PUTATIVE
     - SON
   * - chr21
     - 38092247
     - GA
     - G
     - 38092257
     - AA
     - A
     - HIGH
     - protein_coding_PUTATIVE
     - SIM2
   * - chr21
     - 47545369
     - A
     - AC
     - 47545376
     - C
     - CC
     - HIGH
     - protein_coding_NOVEL
     - COL6A2
