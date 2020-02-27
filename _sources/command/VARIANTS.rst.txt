.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VARIANTS:

========
VARIANTS
========
The **VARIANTS** command returns the variants found in sequence reads and their associated quality. The variant quality is simply the :term:`base-quality` of the first position in the :term:`variant<variants>`.

Usage
=====

.. code-block:: gor

	gor file.bam | VARIANTS [-gc cols] [-readlength size (def 1000bp)] [-bpmergedist bpsize (def 3)]

Options
=======

+-------------------------+-------------------------------------------------------------------------------------------+
| ``-gc``                 | Include this column (or columns) from the BAM file in the output.                         |
+-------------------------+-------------------------------------------------------------------------------------------+
| ``-readlength``         | The max read length. Defaults to 1000 basepairs if none is specified.                     |
+-------------------------+-------------------------------------------------------------------------------------------+
| ``-bpmergedist bpsize`` | If larger than 0 then the variants command will also return merged phased variation,      |
|                         | i.e. it will in addition to the basic varations return merge variations for variations    |
|                         | within the given merge distance                                                           |
+-------------------------+-------------------------------------------------------------------------------------------+
| ``-count``              | Group by variant and add a column called ``varCount`` with the count.                     |
|                         | Equivalent to adding GROUP 1 -count -gc Ref,Alt | RENAME allCount varCount                |
+-------------------------+-------------------------------------------------------------------------------------------+

Examples
========

.. code-block:: gor

    gor file.bam | BAMFLAG -v | VARIANTS -gc firstInPair | GROUP 1 -gc ref,alt -count -min -max -ic firstInpair
    | WHERE allcount > 2 AND min_firstInPair = 0 AND max_firstInPair = 1