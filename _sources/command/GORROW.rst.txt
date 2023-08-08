.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GORROW:

======
GORROW
======
**GORROW** is a :term:`Source commands` that returns a single row of data based on the input parameters.

To use GORROW, you define the chromosome, start position and end position. This command is a tool similar to the SQL command ``SELECT * FROM DUAL`` when working with Oracle database. It is useful when you need to perform a calculation and you want to test the calculation out on a single row before applying the calculation as a pipe step in a larger query with an actual source.

The **GORROW** command is particular useful when testing out a calculated step in a query.

Usage
=====

.. code-block:: gor

   GORROW chromosome,start_position,stop_position

   GORROW chromosome,position


Examples
========
The following example creates a row and waits 1000 milliseconds. Useful for debugging purposes.

.. code-block:: gor

   GORROW chr1,1,1 | WAIT 1000

Output::

    chrom	bpStart	bpStop
    chr1	1	1

The below query creates a row with a position value of 10.

.. code-block:: gor

   GORROW chr1,10

Output::

    chrom	pos
    chr1	10