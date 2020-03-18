.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLNUM:

======
COLNUM
======
The **COLNUM** command prefixes a column number to the cell in the stream (keeping the value of the cell within parentheses). The POS header is prefixed but not the values of the positions. The CHROM column is also omitted from the numbering (since this is assumed to be first in the GOR context. It is used to help you figure out the number of columns in a result and is particularly useful when working with GORpipe on the command-line in a shell that does not have tabular visualisation.

This command does not take any parameters.

Usage
=====

.. code-block:: gor

	gor ... | COLNUM

Examples
========

.. code-block:: gor

   gor -p chr1 #dbsnp# | COLNUM | TOP 10

The above command will output the first ten rows of the ``#dbsnp#`` table with the cells prefixed with the column number.

.. image:: ../images/commands/COLNUM.png
