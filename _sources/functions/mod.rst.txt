.. _mod:

===
MOD
===

The **MOD** function returns the remainder in integer division. The function takes two parameters, the numerator and denominator to be used in the division calculation.


Usage
=====

``MOD(number,number) : integer``


Example
=======

The example query divides 10 by 3 and returns the remainder from the result, which is 1.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test mod(10,3)

.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      1

Related Functions
=================

:ref:`div`