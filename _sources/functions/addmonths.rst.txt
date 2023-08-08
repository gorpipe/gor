.. _addmonths:

=========
ADDMONTHS
=========

The **ADDMONTHS** function calculates the date that is some months after the given date.


Usage
=====

``addmonths(format, date, num_months) : date``

Examples
========

The following example adds a column ``x`` with a date that is 5 months later than the date in column ``first``.

.. code-block:: gor

   ... | CALC x addmonths('dd/MM/yyyy', first, 5) | ...

