.. _floor:

=====
FLOOR
=====

The **FLOOR** function returns the largest integer that is less than or equal to the supplied number. The result is cast into a integer type in the operation.

Usage
=====

``FLOOR(number) : integer``

Example
=======

The example query adds a calculated column with a value of 4, which is the largest integer that is less than the supplied number.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test floor(4.2)


.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      4

Related Functions
=================

:ref:`ceil`