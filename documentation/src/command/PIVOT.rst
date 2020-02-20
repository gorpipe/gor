.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _PIVOT:

=====
PIVOT
=====
The **PIVOT** command allows you to extract significant information from a large, detailed data set by taking row-based data and mapping it into horizontal columns. Pivoting in the GOR language is different from other languages because it assumes that you are pivoting genomic-ordered data and has performance advantages as a result of this.

Usage
=====

.. code-block:: gor

	gor ... | PIVOT pivotCol -v value1,value2,.. [ attributes ]

OPTIONS
=======

+----------------------+-------------------------------------------------------------------+
| ``-gc cols``         | Additional grouping columns (other than chr, pos)                 |
+----------------------+-------------------------------------------------------------------+
| ``-e char``          | Character to denote empty field. Defaults to ?.                   |
+----------------------+-------------------------------------------------------------------+
| ``-v val, val2, ..`` | The values are string without quotes and must be comma-separated. |
+----------------------+-------------------------------------------------------------------+
| ``-vf values``       | Values read from a file or a nested query                         |
+----------------------+-------------------------------------------------------------------+
| ``-vp prefixes``     | Column prefixes read from a file or a nested query                |
+----------------------+-------------------------------------------------------------------+
| ``-ordered``         | Assume rows are ordered by the grouping columns                   |
+----------------------+-------------------------------------------------------------------+

The pivot-aggregate is First, e.g. assuming single-valued data per pivot value per group.
Use :ref:`GROUP` to define other aggregates for multi-valued/row data.
Use :ref:`SELECT` to pick a subset of columns from the output.

The -ordered flag can reduce the memory usage significantly, especially when the number of pivot
values is high. Note that there are no checks to see if the order is correct - only use this option
if the input stream is correctly ordered.
