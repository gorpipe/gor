.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLSPLIT:

========
COLSPLIT
========
The **COLSPLIT** command splits the content of a column into separate columns based on a defined split separator. The number of output columns, which is specified below in the usage with ``colnum``, must be smaller than 100. Output column names are one-based, e.g. prefix_1, ..., prefix_colnum.

Usage
=====

.. code-block:: gor

	gor ... | colsplit splitcol colnum prefix [-s sepVal] [-m missVal ]

Options
=======

+----------------+-----------------------------------------------------------------------------+
| ``-s sepval``  | Split separator. By default it is a comma ','.                              |
+----------------+-----------------------------------------------------------------------------+
| ``-m missval`` | The value to put into the last columns if the list is shorter than colnums. |
+----------------+-----------------------------------------------------------------------------+
| ``-o``         | Add a column with the number of columns in the list.                        |
+----------------+-----------------------------------------------------------------------------+


Examples
========
The following example will take the QName column in the specified BAM file and split the value at each colon. The split values will be inserted into separate columns prefixed with ``split_`` followed by a number. If there is not enough input for all the split columns, the question mark character will be inserted for any empty columns.

.. code-block:: gor

	gor file.bam | COLSPLIT QName 7 split -s ':' -m '?'


