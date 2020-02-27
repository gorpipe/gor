.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _DISTINCT:

========
DISTINCT
========
The **DISTINCT** command eliminates duplicate rows from the output stream. Unlike the SQL command ``select distinct *``, the DISTINCT command in the GOR language is non-blocking and very efficient because it takes advantage of the genomic order or the output from the streams to which it is applied.

Usage
=====

.. code-block:: gor

	gor ... | DISTINCT | ...





