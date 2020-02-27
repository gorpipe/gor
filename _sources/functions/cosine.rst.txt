.. _cosine:

===
COS
===

The **COS** function calculates the cosine (in radians) of the given value.


Usage
=====

``sin(num) : num``

Examples
========

The following example creates a single NOR row with a column x that calculates the cosine of 3.14159 (which is 180° in radians) resulting in an answer of -1.0.

.. code-block:: gor

   NORROWS 1 | CALC x cos(3.14159)

Note that due to rounding, the cosine of 3.14159/2, or 90°, is given as 1.33E-6 instead of its actual value of 0.