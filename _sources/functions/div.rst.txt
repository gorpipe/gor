.. _div:

===
DIV
===

The **DIV** function returns the quotient in integer division. The function takes two parameters, the numerator and denominator to be used in the division calculation.


Usage
=====

``DIV(number,number) : integer``


Example
=======

The example query divides 10 by 3 and returns the quotient from the result, which is 3.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test div(10,3)

.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      3

Related Functions
=================

:ref:`mod`