.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor</span>

.. _VARGROUP:

========
VARGROUP
========
Takes in a GOR stream of hardcalls stored in horizontal manner and groups together the lines corresponding to the same reference allele at a given position.
The genotypes should be stored in a column named `Values`, and the reference and alternative alleles in a column whose name starts with `ref` and `alt`, respectively.

Usage
=====

.. code-block:: gor

	gor ... | VARGROUP [-gc groupCols]

Options
=======

+-------------------+---------------------------------------------------------------------------+
| ``-gc groupcols   | The cols to group on, in addition to chrom, pos and the reference column. |
+-------------------+---------------------------------------------------------------------------+
| ``-sep separator``| The separator to separate the outcoming genotypes with. Default to comma. |
+-------------------+---------------------------------------------------------------------------+
