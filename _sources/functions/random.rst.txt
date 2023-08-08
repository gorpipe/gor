.. _random:

======
RANDOM
======

The **RANDOM** function creates a random number between 0.0 and 1.0.

Usage
=====

``RANDOM(number) : float``

Example
=======

The example query adds a calculated column with a random number.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test rand()


.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      0.342