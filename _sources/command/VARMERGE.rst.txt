.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VARMERGE:

========
VARMERGE
========
The :ref:`VARMERGE` command ensures that overlapping variants, represented as reference sequence and alternative sequence, are denoted in an equivalent manner.

The ``-seg`` option is used to indicate if the variants are denoted with zero-based segments (e.g. Chrom,bpStart,bpStop,Ref,Alt) as opposed to the default one-based position format (e.g. Chrom,Pos,Ref,Alt).

By default the variants are represented in a right-normalized format such that SNPs have one letter notation and InDels have an identical base in the first letter of refcol and altcol.  The ``-nonorm`` option does skip the normalization step and represents the variants with the maximum span of the reference sequence (depends on the overlap between reference sequences in the input stream).

Right-normalising has the benefit of presenting the variations in different rows into a coherent form. This is done because most aligners do not guarantee consistent representations of InDels for sequence reads in repeat regions.

The merge span (as defined by the ``-span`` option) is capped at 1000 base-pairs.

Usage
=====

.. code-block:: gor

	gor ... | VARMERGE refcol altcol [ -seg | -nonorm| -span]

Options
=======
+-------------+---------------------------------------------------------------------------+
| ``-seg``    | The variant is denoted as segment, e.g. (chr,bpstart,bpstop,ref,call).    |
+-------------+---------------------------------------------------------------------------+
| ``-nonorm`` | Do not minimize (normalize) the variant after merging the reference seqs. |
+-------------+---------------------------------------------------------------------------+
| ``-span``   | Max merge span. The default is 100bp.                                     |
+-------------+---------------------------------------------------------------------------+

Examples
========
For examples of how to use the :ref:`VARMERGE` command, check the chapter on :ref:`Merging Variants<mergingVariants>`.

