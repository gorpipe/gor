.. _dayofmonth:

==========
DAYOFMONTH
==========

The **DAYOFMONTH** function returns the day of month of the given date, as an int from 1-31.


Usage
=====

``dayofmonth(format, date) : int``

See :ref:`date` for a description of the date format.

Examples
========

The following example adds a column ``x`` with the day of month number of the date in column ``first``.

.. code-block:: gor

   ... | CALC x dayofmonth('dd/MM/yyyy', first) | ...

