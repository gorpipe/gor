.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _ROWNUM:

======
ROWNUM
======
The ROWNUM command adds a column labeled "rownum" with an automatically increasing unique sequential row-number, based on the number of rows that have been passed through in the query. This is useful, if you wish to create a unique ID on the output of the query you are working with.
The row number can also be counted on a group basis with the ``-gc`` option. This will count the row number within each group of the specified columns.

Note that this command may have unexpected behaviour if used with nested queries, as the output of the nested queries are also given row numbers.

OPTIONS
=======

+----------------------+-------------------------------------------------------------------+
| ``-gc cols``         | Grouping columns                                                  |
+----------------------+-------------------------------------------------------------------+

Usage
=====

.. code-block:: gor

	gor ... | ROWNUM

Example above adds a column "rownum" to the output of the query containing the row number.

.. code-block:: gor

	gor ... | ROWNUM -gc col1,col2

Example above adds a column "rownum" to the output of the query containing the row number for the selected grouping.
