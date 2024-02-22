.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SEGWHERE:

========
SEGWHERE
========
The **SEGWHERE** command turns a stream of annotations into a stream of non-overlapping segments.
The output is Chrom, bpStart, bpStop.

Usage
=====

.. code-block:: gor

	gor ... | segwhere -m 1000 ref != 'T'

Options
=======

+------------------+---------------------------------------------------------------------------------------------------+
| ``-m size``      | The maximum distance from one row to the next before a span gets created. Defaults to 3000        |
+------------------+---------------------------------------------------------------------------------------------------+
