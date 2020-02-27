.. _sine:

===
SIN
===

The **SIN** function calculates the sine (in radians) of the given value.


Usage
=====

``sin(num) : num``

Examples
========

The following example creates a single NOR row with a column x that calculates the sin of 3.14159 / 2 (which is 90° in radians) resulting in an answer of 1.0.

.. code-block:: gor

   NORROWS 1 | CALC x sin(3.14159/2)