.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GORROWS:

=======
GORROWS
=======
**GORROWS** is a :term:`source command<source commands>` that generates a number of genomic ordered rows based on the input parameters.

To use GORROWS, you define the chromosome, start position and stop position to generate genomic ordered rows where first row has position equal to the start position and the last row has position equal to the stop position.

It is useful when you need to perform a calculation and you want to test the calculation out on multiple rows before applying the calculation as a pipe step in a larger query with an actual source.

The **GORROWS** command is particularly useful when testing out a calculated step in a query.

Usage
=====

.. code-block:: gor

   GORROWS -p chromosome:start_position-stop_position -segment [value] -step [value]


Options
=======

+---------------------------------------------------+---------------------------------------------------------------------------------------------------+
| ``-p [chromosome:start_position-stop_position]``  | Generate rows for chromosome with a position value ranging from start_position to stop_position.  |
+---------------------------------------------------+---------------------------------------------------------------------------------------------------+
| ``-segment [value > 0]``                          | Segment length between start and stop positions for each generated row.                           |
+---------------------------------------------------+---------------------------------------------------------------------------------------------------+
| ``-step [value > 0] default is 1``                | Step size between each position in each generated row.                                            |
+---------------------------------------------------+---------------------------------------------------------------------------------------------------+


Examples
========
The following example generates 10 rows with position starting from start_position to stop_position.

.. code-block:: gor

   GORROWS -p chr1:0-10

.. code-block:: bash

   chrom    pos
   chr1     0
   chr1     1
   chr1     2
   chr1     3
   chr1     4
   chr1     5
   chr1     6
   chr1     7
   chr1     8
   chr1     9

The following example generates 10 rows where the segment value is the length between the start and stop positions.

.. code-block:: gor

   GORROWS -p chr1:0-10 -segment 100

.. code-block:: bash

   chrom    bpStart bpStop
   chr1     0       100
   chr1     1       101
   chr1     2       102
   chr1     3       103
   chr1     4       104
   chr1     5       105
   chr1     6       106
   chr1     7       107
   chr1     8       108
   chr1     9       109

The following example generates 10 rows where the step value is the size between each position in the generated rows.

.. code-block:: gor

   GORROWS -p chr1:0-100 -step 10

.. code-block:: bash

   chrom    pos
   chr1     0
   chr1     10
   chr1     20
   chr1     30
   chr1     40
   chr1     50
   chr1     60
   chr1     70
   chr1     80
   chr1     90

The following example generates 10 rows where the segment value is the length between the start and stop positions.

.. code-block:: gor

   GORROWS -p chr1:0-100 -segment 100 -step 10

.. code-block:: bash

   chrom    bpStart bpStop
   chr1     0       100
   chr1     10      110
   chr1     20      120
   chr1     30      130
   chr1     40      140
   chr1     50      150
   chr1     60      160
   chr1     70      170
   chr1     80      180
   chr1     90      190