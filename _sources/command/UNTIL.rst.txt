.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _UNTIL:

=====
UNTIL
=====
The **UNTIL** command will terminate the stream when a condition is matched. Conditions can be written in the same way as with a :ref:`WHERE` command.

Usage
=====

.. code-block:: gor

   gor ... | UNTIL <condition>

Examples
========
The following GOR query will return all rows from the ``#dbsnp#`` table from chromosome 10. Note that since, alphabetically, chr10 comes after chr1, we only get chr10 in the result from the query.

.. code-block:: gor

   gor #dbsnp# | WHERE #1 > 'chr1' | UNTIL #1 > 'chr10'

Of course, this is much more efficiently written as:

.. code-block:: gor

   gor -p chr10 #dbsnp#