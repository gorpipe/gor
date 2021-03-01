.. _daydiff:

=======
DAYDIFF
=======

The **DAYDIFF** function calculates the difference, in days, between two given dates.


Usage
=====

``daydiff(format, date1, date2) : num``

Examples
========

The following example adds a column ``x`` with the value 2.

.. code-block:: gor

   ... | CALC x daydiff('dd/MM/yyyy', '16/06/2017', '18/06/2017') | ...

