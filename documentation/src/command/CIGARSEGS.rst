.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _CIGARSEGS:

=========
CIGARSEGS
=========

The :ref:`CIGARSEGS` command takes the sequence read from a BAM-like stream and splits them into multiple reads based on the CIGAR string. As such, the input must have a column named CIGAR. The ``-gc`` option can be used to annotate the reads with other columns from the input.

CIGAR is a string which describes how an individual read aligns with the larger reference sequence. A CIGAR may consist of one or many components, with each component having an operator and a number of bases that the operator applies to. Operators can be DHIMNPSX or =. These are explained in the following table:

+----------+--------------------------------------------------------------------------------------------------+
| Operator | Description                                                                                      |
+----------+--------------------------------------------------------------------------------------------------+
| ``D``    | Deletion, i.e. the nucleotide is _not_ present in the read, but is present in the reference.     |
+----------+--------------------------------------------------------------------------------------------------+
| ``H``    | Hard Clipping; the clipped nucleotides are not present in the read.                              |
+----------+--------------------------------------------------------------------------------------------------+
| ``I``    | Insertion, i.e. the nucleotide is present in the read, but is _not_ present in the reference.    |
+----------+--------------------------------------------------------------------------------------------------+
| ``M``    | Match, i.e. the nucleotide is present in both the read and the reference.                        |
+----------+--------------------------------------------------------------------------------------------------+
| ``N``    | Skipped region, where a whole region of nucleotides is not present in the read.                  |
+----------+--------------------------------------------------------------------------------------------------+
| ``P``    | Padding, where there exists a padded area in the read but not in the reference.                  |
+----------+--------------------------------------------------------------------------------------------------+
| ``S``    | Soft Clipping; the clipped nucleotides are present in the read.                                  |
+----------+--------------------------------------------------------------------------------------------------+
| ``X``    | Read mismatch, where the nucleotide is present in the reference.                                 |
+----------+--------------------------------------------------------------------------------------------------+
| ``=``    | Read match, where the nucleotide is present in the reference.                                    |
+----------+--------------------------------------------------------------------------------------------------+

Usage
=====

.. code-block:: gor

	gor *.bam ... | CIGARSEGS [-seq] [-gc Cols -readlength size (def. 1000bp)]

Options
=======

+-------------------+---------------------------------------------------------------------------------+
| ``-gc cols``      | Annotate the reads with the specified columns from the reads.                   |
+-------------------+---------------------------------------------------------------------------------+
| ``-seq``          | Output the sequence of the segment.                                             |
+-------------------+---------------------------------------------------------------------------------+
| ``-readlength s`` | The max read length.                                                            |
+-------------------+---------------------------------------------------------------------------------+

Examples
========
                
Following is an example that finds the distribution of how RNA reads map to 0, 1, 2, ..., N exons:

.. code-block:: gor

    gor file.bam | ROWNUM | RENAME rownum readID | CIGARSEGS -gc pos,readID | SORT 10000 | JOIN -segseg #exons# -l
    | CALC overlap IF(genes != '',1,0) | SELECT 1,pos,readid,overlap | SORT 10000 | GROUP 1 -gc readID -sum -ic overlap
    | GROUP genome -gc sum_overlap -count

See also :ref:`BASES` and :ref:`VARIANTS`, but variants is equivalent to the deprecated ``-ref`` option in CIGARSEGS.