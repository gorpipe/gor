.. _min:

===
MIN
===

The **MIN** function compares two numbers and returns the minimum of the two. The supplied numbers can be references to columns.


Usage
=====

``MIN(number,number) : float``


Example
=======

The example query here explicitly lists two numbers (4 and 5) and creates a calculated column called "test" with the minimum number (4).

.. code-block:: gor

   gorrow chr1,1,1 | CALC test min(4,5)

.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      4

The next query creates two columns (number1 and number2) with random numbers and then compares and writes the lower number in the test column.

.. code-block:: gor

   gorrow chr1,1,1 | CALC number1 random() | CALC number2 random() | CALC test min(number1,number2)

The following query contains nested **MIN** functions to show how you might want to compare three different numbers.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test min(4,min(3,5))

.. code-block:: bash

   chrom    bpStart bpStop test
   chr1     1       1      3


Related Functions
=================

:ref:`max`

