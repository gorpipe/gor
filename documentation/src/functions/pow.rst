.. _pow:

====
POW
====

The **POW** function raises the first parameter to the power of the second. Both parameters must be numbers (either an integer or float). The result will always be a floating point number.

Usage
=====

``POW(number,number) : float``

Example
=======

The example query raises the first parameter ``4`` to the power of ``2``.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test pow(4,2)


.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      16.0
