.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLTYPE:

=======
COLTYPE
=======
The **COLTYPE** command prefixes the column type to the cell in the stream (keeping the value of the cell within
parentheses). This can be helpful when debugging issues in CALC expressions.

This command does not take any parameters.

Usage
=====

.. code-block:: gor

	gor ... | COLTYPE

Examples
========

.. code-block:: gor

   gor #dbsnp# | CALC data 'this is a test' | COLTYPE | TOP 10

The above command will output the first ten rows of the ``#dbsnp#`` table with the cells prefixed with the column type.

