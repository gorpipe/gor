.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _TOP:

===
TOP
===
The :ref:`TOP` command specifies how many rows are returned by the query. The command terminates once all the specified number of rows have been returned. If no number is entered in the TOP command, the default of 1 is used and the query will return a single row.

This command is useful to limit the amount of results from a query that would otherwise return a large number of rows and therefore take a long time to run completely.

Usage
=====

.. code-block:: gor

	gor ... | top [number]

Examples
========
The following query will return the first 100 rows from the ``#dbsnp#`` table

.. code-block:: gor

	gor #dbsnp# | top 100
