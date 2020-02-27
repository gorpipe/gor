.. _ceil:

====
CEIL
====

The **CEIL** function returns the smallest integer that is larger than or equal to the supplied number. The result is cast into a integer type in the operation.

Usage
=====

``CEIL(number) : integer``

Example
=======

The example query adds a calculated column with a value of 5, which is the smallest integer that is larger than the supplied number.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test ceil(4.2)


.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      5

Related Functions
=================

:ref:`floor`