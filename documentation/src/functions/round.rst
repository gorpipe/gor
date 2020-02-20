.. _round:

=====
ROUND
=====

The **ROUND** function rounds the supplied number to the nearest integer.

Usage
=====

``ROUND(number) : integer``

Example
=======

The example query adds a calculated column with the numbered rounded.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test round(4.5)


.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      5