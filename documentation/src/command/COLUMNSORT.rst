.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLUMNSORT:

==========
COLUMNSORT
==========
The :ref:`COLUMNSORT` command sorts the columns specified to the beginning of the column list. The rest of the columns will then appear after the specified columns and will be sorted alphabetically based on their name.

Usage
=====

.. code-block:: gor

	gor ... | COLUMNSORT column-list

Examples
========
The following query will take the first five rows of chromosome 1 from the ``#dbsnp#`` table, keep the Chrom and POS columns in the front and then sort the rest of the columns alphabetically. Essential, this command will reverse the order of the Reference and Allele columns.

.. code-block:: gor

   gor -p chr1 #dbsnp# | TOP 5 | COLUMNSORT Chrom,POS

