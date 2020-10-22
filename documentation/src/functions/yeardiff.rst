.. _yeardiff:

========
YEARDIFF
========

The **YEARDIFF** function calculates the difference, in years, between two given dates.


Usage
=====

``yeardiff(format, date1, date2) : num``

Examples
========

The following example adds a column ``x`` with the value 3.

.. code-block:: gor

   ... | CALC x daydiff('dd/MM/yyyy', '16/06/2014', '18/06/2017') | ...

