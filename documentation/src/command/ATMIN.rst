.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _ATMIN:

=====
ATMIN
=====
The :ref:`ATMIN` command allows you to select a single row based on a minimum value of a defined column. Note that the column needs to be a number.

Using **ATMIN** in a nor query will give an error if ``binsize`` input parameter is used.

Usage
=====

.. code-block:: gor

	gor ... | ATMIN binsize column [ attributes ]

.. code-block:: gor

 	nor ... | ATMIN column [ attributes ]

Options
=======

+-------------------+----------------------------------------------------------------------+
| ``-last``         | Returns the last value for multiple possible minimums.               |
+-------------------+----------------------------------------------------------------------+
| ``-gc cols``      | Grouping columns (other than bin).                                   |
+-------------------+----------------------------------------------------------------------+
| ``-ordered``      | Assume the grouping columns are ordered.                             |
+-------------------+----------------------------------------------------------------------+

Use ``binsize = chrom`` to aggregate for a whole chromosome and ``binsize = genome`` to aggregate for the entire genome. Note, that the column must be a numeric column.

When using ATMIN in a NOR context, the ordered flag can both speed up the operation and reduce the memory usage
significantly. Note that there are no checks to see if the order is correct - only use this option if the input
stream is correctly ordered.

Examples
========

.. code-block:: gor

	gor ... | ATMIN 1000 gene_start

Returns a row with the minimum gene_start value over 1000 base pairs.

.. code-block:: gor

	gor ... | ATMIN chrom value_column -last -gc date

Returns a row with the minimum value_column value per chromosome. Group rows by the date column, each group is represented with a single row. If multiple minimums exist take the last one found.
