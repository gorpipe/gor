.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _WAIT:

====
WAIT
====
The **WAIT** command instructs the GOR query to wait the specified number of milliseconds before proceeding to the next row. The command is used primarily for debugging purposes and often in conjunction with the :ref:`GORROW` command in deployment testing.

Usage
=====

.. code-block:: gor

   gor ... | WAIT <time_in_milliseconds>

Examples
========

.. code-block:: gor

   GORROW chr1,1,1 | WAIT 61000

The below example waits for 100ms for 100 rows resulting in a 10 second wait in total.

.. code-block:: gor

   gor #dbsnp# | top 100 | wait 100