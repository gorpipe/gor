.. _month:

=======
MONTH
=======

The **MONTH** function returns the month of the given date, as an int from 1-12.


Usage
=====

``month(format, date) : int``

See :ref:`date` for a description of the date format.

Examples
========

The following example adds a column ``x`` with the month of the date in column ``first``.

.. code-block:: gor

   ... | CALC x month('dd/MM/yyyy', first) | ...

