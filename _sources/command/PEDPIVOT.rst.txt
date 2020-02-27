.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _PEDPIVOT:

========
PEDPIVOT
========
The **PEDPIVOT** command allows you to extract significant information from a large, detailed data set by taking row-based data and mapping it into horizontal columns. This command is similar to the :ref:`PIVOT` command, but differs in that it takes a pedigree file as a parameter and uses this pedigree file as additional values to pivot the data.

Pivoting in the GOR language is different from other languages because it assumes that you are pivoting genomic-ordered data and has performance advantages as a result of this.

Usage
=====

.. code-block:: gor

	gor ... | pedpivot personCol pedigree_file.txt [ attributes ]

Options
=======

+--------------+---------------------------------------------------------------------------------------+
| ``-a``       | Expand the pedigree, i.e. include parents if missing as index persons.                |
+--------------+---------------------------------------------------------------------------------------+
| ``-v``       | Use Person\_, Father\_ and Mother\_ as column prefixes indstead of P\_, F\_ and M\_.  |
+--------------+---------------------------------------------------------------------------------------+
| ``-gc cols`` | Additional grouping columns (other than chr, pos).                                    |
+--------------+---------------------------------------------------------------------------------------+
| ``-e char``  | Character to denote empty field. Defaults to ?.                                       |
|              | cols is a one-based listing of columns, e.g. 3,5-7 for col3, col5, col6 and col7.     |
|              | The pedigree file should be a 3 column tab-del text file: Person, Father, Mother.     |
+--------------+---------------------------------------------------------------------------------------+

