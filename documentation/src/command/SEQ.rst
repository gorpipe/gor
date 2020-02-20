.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SEQ:

===
SEQ
===
The **SEQ** command returns the corresponding reference sequence read for each row of the output based on the position column.

Other columns can be used instead of the position column (#2) using the ``-c`` option. The length of the flanking sequence (the number of bases on either side of the position defaults to 10, but can be changed using the ``-l`` option.

Usage
=====

.. code-block:: gor

	gor file | SEQ [ attributes ]

Options
=======

+---------------+----------------------------------------------------------------------------------+
| ``-c cols``   | The columns for which the reference sequence is found.                           |
|               | By default, only column number 2 is used.                                        |
+---------------+----------------------------------------------------------------------------------+
| ``-l number`` | The length of the flanking sequence, e.g. total seq. length is equal to 2*l + 1. |
+---------------+----------------------------------------------------------------------------------+

