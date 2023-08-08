.. _chisquare:

=========
CHISQUARE
=========

The **CHISQUARE** distribution is used in the common chi-squared tests for goodness of fit of an observed distribution to a theoretical one.

Usage
=====

``CHISQUARE(number) : integer``

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