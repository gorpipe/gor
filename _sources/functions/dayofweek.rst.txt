.. _dayofweek:

=========
DAYOFWEEK
=========

The **DAYOFWEEK** function returns the day of week of the given date. The int value returned follows the ISO-8601
standard, from 1 (Monday) to 7 (Sunday).


Usage
=====

``dayofweek(format, date) : int``

See :ref:`date` for a description of the date format.

Examples
========

The following example adds a column ``x`` with the weekday number of the date in column ``first``.

.. code-block:: gor

   ... | CALC x dayofweek('dd/MM/yyyy', first) | ...

