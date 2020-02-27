.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _ATMAX:

=====
ATMAX
=====
The :ref:`ATMAX` command allows you to select a single row based on a maximum value of a defined column. Note that the column needs to be a number.

Using **ATMAX** in a nor query will give an error if ``binsize`` input parameter is used.

Usage
=====

.. code-block:: gor

	gor ... | ATMAX binsize column [ attributes ]

.. code-block:: gor

 	nor ... | ATMAX column [ attributes ]

Options
=======

+-------------------+----------------------------------------------------------------------+
| ``-last``         | Returns the last value for multiple possible maximums.               |
+-------------------+----------------------------------------------------------------------+
| ``-gc cols``      | Grouping columns (other than bin).                                   |
+-------------------+----------------------------------------------------------------------+

Use ``binsize = chrom`` to aggregate for a whole chromosome and ``binsize = genome`` to aggregate for the entire genome. Note, that the column must be a numeric column.

Examples
========

.. code-block:: gor

	gor ... | atmax 1000 gene_start

Returns a row with the maximum gene_start value over 1000 base pairs.

.. code-block:: gor

	gor ... | atmax chrom value_column -last -gc date

Returns a row with the maximum value_column value per chromosome. Group rows by the date column, each group is represented with a single row. If multiple maximums exist take the last one found.
