.. _arctangent:

====
ATAN
====

The **ATAN** function calculates the arctangent, or inverse tangent, (in radians) of the given value.


Usage
=====

``atan(num) : num``

Examples
========

The following example creates a single NOR row with a column x that calculates the arctangent of 1.0 resulting in an answer of 0.7854 (which is 45° in radians).

.. code-block:: gor

   NORROWS 1 | CALC x atan(1)