.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _LEFTJOIN:

========
LEFTJOIN
========
The **LEFTJOIN** command joins two genomic-ordered tables and displays a merged data set that contains all matching rows of the two tables, in addition to any data from the left source that does not match the right source.

Using LEFTJOIN in a GOR query is equivalent to using a regular :doc:`JOIN` with the ``-l`` option.

Usage
=====

.. code-block:: gor

   gor fileA.gor | LEFTJOIN fileB.gor

