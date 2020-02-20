.. _arccosine:

====
ACOS
====

The **ACOS** function calculates the arccosine, or inverse cosine, (in radians) of the given value, which must be between -1 and 1.


Usage
=====

``acos(num) : num``

Examples
========

The following example creates a single NOR row with a column x that calculates the arccosine of 0.5 resulting in an answer of 1.0472 (which is 60° in radians).

.. code-block:: gor

   NORROWS 1 | CALC x asin(0.5)