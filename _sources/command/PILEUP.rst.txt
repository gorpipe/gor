.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _PILEUP:

======
PILEUP
======
The :ref:`PILEUP` command describes the base-pair formation at each chromosomal position. It summarizes the base calls of aligned sequence reads to a reference sequence, which facilitates the visual display of SNP/Indel calling and alignment.

Usage
=====

.. code-block:: gor

	gor file.bam | ... | PILEUP [ options ]

Options
=======

+-------------------+---------------------------------------------------------------------------------------+
| ``-i number``     | Max iSize of reads in bp. The default is no limit.                                    |
+-------------------+---------------------------------------------------------------------------------------+
| ``-q number``     | Min alignment quality, defaults to 0.                                                 |
+-------------------+---------------------------------------------------------------------------------------+
| ``-bq number``    | Minimum base quality score, e.g. 1 - 96. The default value is 1.                      |
+-------------------+---------------------------------------------------------------------------------------+
| ``-nf``           | Deprecated. By default there is not flag filtering.                                   |
+-------------------+---------------------------------------------------------------------------------------+
| ``-df``           | "Apply default flag filtering,                                                        |
|                   | e.g. ((flag & 0x0200)==0 && flag & 0x0400)==0 && (flag & 0x0002)==2 && iSize!=0)      |
|                   | See BAMFLAG to perform special filtering of the bitmap flags using the WHERE command. |
|                   | Also see the BAMTAG formula, e.g. calc RG bamtag(TAG_VALUES,'RG'),                    |
|                   | to filter reads on TAG_VALUES in BAM files                                            |
+-------------------+---------------------------------------------------------------------------------------+
| ``-depth``        | Only calculate the depth.                                                             |
+-------------------+---------------------------------------------------------------------------------------+
| ``-gc cols``      | Additional grouping columns, e.g. a PN, CaseControl column or both.                   |
+-------------------+---------------------------------------------------------------------------------------+
| ``-gt``           | Call SNPs using Bayesian caller.                                                      |
+-------------------+---------------------------------------------------------------------------------------+
| ``-mprob number`` | The prior probability that a base differs from the reference.                         |
+-------------------+---------------------------------------------------------------------------------------+
| ``-sex``          | Use sex-chromosomal model instead of autosomal Bayesian model.                        |
+-------------------+---------------------------------------------------------------------------------------+
| ``-span``         | Max read span. The default is 1000bp.                                                 |
+-------------------+---------------------------------------------------------------------------------------+

The Chi value in the output is based on a calculation where the 3 non-major bases are assumed equally distributed.  The SNP column is 1 or 0 depending on comparison of the most likely genotype, GT, with the reference base.  GT2 is the second most likely genotype and the LOD is the log of the log of the likelihood ratio of GT and GT2.
