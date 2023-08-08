.. _monthdiff:

=========
MONTHDIFF
=========

The **MONTHDIFF** function calculates the difference, in months, between two given dates.


Usage
=====

``monthdiff(format, date1, date2) : num``

See :ref:`date` for a description of the date format.

Examples
========

The following example adds a column ``x`` with the value 2.

.. code-block:: gor

   ... | CALC x monthdiff('dd/MM/yyyy', '16/06/2017', '18/08/2017') | ...

