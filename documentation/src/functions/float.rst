.. _float:

=====
FLOAT
=====

A **FLOAT** function can be used to convert a string (or integer) to a float. This function take an additional parameter, which


Usage
=====

``FLOAT(col, float) : float``

Examples
========

In the example below, we take an integer ('foo'), change it using a :ref:`float type conversion<float>` into a float (at which point it becomes 5.0) and then use the **ISFLOAT** within an :ref:`if` statement to evaluate if it is a float or not. (It is.)

.. code-block:: gor

   gorrow 1,1,1 | CALC foo 5 | CALC bar float(foo) | CALC float? IF(ISFLOAT(bar),'true','false')