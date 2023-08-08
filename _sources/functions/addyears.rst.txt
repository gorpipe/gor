.. _addyears:

========
ADDYEARS
========

The **ADDYEARS** function calculates the date that is some years after the given date.


Usage
=====

``addyears(format, date, num_years) : date``

Examples
========

The following example adds a column ``x`` with a date that is 5 years later than the date in column ``first``.

.. code-block:: gor

   ... | CALC x addyears('dd/MM/yyyy', first, 5) | ...

