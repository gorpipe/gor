.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SEGWHERE:

========
SEGWHERE
========
The **SEGWHERE** command turns a stream of annotations into a stream of non-overlapping segments.
The output is Chrom, bpStart, bpStop. Use ``-minseg size`` to set the minimum segment size required to be considered a
segment. Using the ``-sh``option will start the segment halfway from the previous false condition. If the first available
row meets the condition then the segment starts at the row position. Using the ``-eh`` option will end the segment halfway
from the previous true condition. If the last available row meets the condition then the segment ends at the row position.

Usage
=====

.. code-block:: gor

	gor ... | segwhere -minseg 1000 -sh -eh ref != 'T'

Options
=======

+------------------+---------------------------------------------------------------------------------------------------+
| ``-minseg size`` | Minimum segment size required to be considered a segment. Default is unlimited                    |
+------------------+---------------------------------------------------------------------------------------------------+
| ``-sh``          | Segment starts halfway from previous false condition. If first available row meets the condition  |
|                  | then the segment starts at row position.                                                          |
+------------------+---------------------------------------------------------------------------------------------------+
| ``-eh``          | Segment ends halfway from previous true condition. If last available row meets the condition      |
|                  | then the segment ends at row position.                                                            |
+------------------+---------------------------------------------------------------------------------------------------+
