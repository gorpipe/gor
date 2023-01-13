.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLUMNREORDER:

=============
COLUMNREORDER
=============
The :ref:`COLUMNREORDER` command reorders the columns specified to the beginning of the column list. The rest of the columns will then appear after the specified columns.

Usage
=====

.. code-block:: gor

	gor ... | COLUMNREORDER column-list [-t]

Examples
========
The following query will take the first five rows of chromosome 1 from the ``#dbsnp#`` table, keep the Chrom, POS and Allele columns in the front and then the rest of the columns in maintained order.

.. code-block:: gor

   gor -p chr1 #dbsnp# | TOP 5 | COLUMNREORDER Chrom,POS,Allele

