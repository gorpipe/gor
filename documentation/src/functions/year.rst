.. _year:

=======
YEAR
=======

The **YEAR** function returns the year of the given date, as an int.


Usage
=====

``year(format, date) : int``

See :ref:`date` for a description of the date format.

Examples
========

The following example adds a column ``x`` with the year of the date in column ``first``.

.. code-block:: gor

   ... | CALC x year('dd/MM/yyyy', first) | ...

