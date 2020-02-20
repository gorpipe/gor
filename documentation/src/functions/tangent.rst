.. _tangent:

===
TAN
===

The **TAN** function calculates the tangent (in radians) of the given value.


Usage
=====

``tan(num) : num``

Examples
========

The following example creates a single NOR row with a column x that calculates the tangent of 3.14159 / 4 (which is 45° in radians) resulting in an answer of 1.0.

.. code-block:: gor

   NORROWS 1 | CALC x tan(3.14159/4)