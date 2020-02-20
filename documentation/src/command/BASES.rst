.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _BASES:

=====
BASES
=====
The :ref:`BASES` command splits the sequence read (specifically the ``SEQ`` column in BAM) into the individual bases, outputting a row for each of the bases and showing the relative position within the read along with the :term:`base-quality`. The MDI label indicates whether the read comes from a match section in the CIGAR, a deletion, or an insert section.

Note that the readlength is set by default to 1000 basepairs. Setting the length to a lower value can lead to the genomic order of the gor output being violated.

Usage
=====

.. code-block:: gor

	gor file.bam | BASES [ -gc cols ] [-readlength size (def 1000bp)]

Options
=======

+-------------------+-----------------------------------------------------------------------------------------------------------------------------+
| ``-gc``           | To specify grouping columns. Used to select extra columns from the input stream such that they show up in the output.       |
+-------------------+-----------------------------------------------------------------------------------------------------------------------------+
| ``-readlength``   |  The maximum length of the aligned read, i.e. the maximum of #3 - #2 but not the maximum of len(seq).                       |
|                   |  The default value is 1000bp.                                                                                               |
+-------------------+-----------------------------------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

   gor -p chr1:999000- file.bam | UNTIL #2 > 1000000+1000 | BASES -gc MAPQ | WHERE MDI != 'I'
    | CALC LQ if(BaseQual <= 30 or MAPQ <=30,1,0)  | CALC HQ if(not(BaseQual > 30 or MAPQ <= 30),1,0)
    | CALC type base+if(MDI='D','Del','') | GROUP 1 -gc ref,type -sum -ic LQ,HQ
    | WHERE #2 > 1000000 and #2 <= 1000000+1000
    | RENAME sum_LQ LQ | RENAME sum_HQ HQ | PIVOT type -v A,C,G,T,D -gc ref -e 0

The above query uses shows the pileup depth of high and low quality A,C,G,T and Del for each location in a 1000bp genomic region, from chr1:1M to chr1:1M+1000bp. The quality is based both on the mapping quality of the read and the quality of individual bases in the read. The range filtering is to avoid boundary effects due to missing overlapping reads