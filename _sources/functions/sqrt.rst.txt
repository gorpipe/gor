.. _sqrt:

====
SQRT
====

The **SQRT** function returns the square root of a supplied number. The result is cast into a floating point type in the operation.

Usage
=====

``SQRT(number) : float``

Example
=======

The example query adds a calculated column with the square root of 4.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test sqrt(4)


.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      2.0

