.. _arcsine:

====
ASIN
====

The **ASIN** function calculates the arcsine, or inverse sine, (in radians) of the given value.


Usage
=====

``asin(num) : num``

Examples
========

The following example creates a single NOR row with a column x that calculates the arcsine of 1.0 resulting in an answer of 1.5708 (which is 90° in radians).

.. code-block:: gor

   NORROWS 1 | CALC x asin(1.0)