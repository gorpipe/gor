.. _adddays:

=======
ADDDAYS
=======

The **ADDDAYS** function calculates the date that is some days after the given date.


Usage
=====

``adddays(format, date, num_days) : date``

Examples
========

The following example adds a column ``x`` with a date that is 15 days later than the date in column ``first``.

.. code-block:: gor

   ... | CALC x adddays('dd/MM/yyyy', first, 15) | ...

