.. _dayofyear:

=======
DAYOFYEAR
=======

The **DAYOFYEAR** function returns the day of year of the given date, as an int from 1-365 (or 366 for leap years).


Usage
=====

``dayofyear(format, date) : int``

See :ref:`date` for a description of the date format.

Examples
========

The following example adds a column ``x`` with the day of year number of the date in column ``first``.

.. code-block:: gor

   ... | CALC x dayofyear('dd/MM/yyyy', first) | ...

