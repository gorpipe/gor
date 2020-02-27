.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _NORROWS:

=======
NORROWS
=======

**NORROWS** is a :term:`source command<source commands>` that generates a number of rows based on the input parameters.

To use NORROWS, you specify the number of rows to generate.
It is useful when you need to perform a calculation and you want to test the calculation out on multiple rows before applying the calculation as a pipe step in a larger query with an actual source.
The **NORROWS** command is particular useful when testing out a calculated step in a query.

Usage
=====

.. code-block:: gor

   NORROWS <number of rows> [options]

Options
=======

+----------------------+---------------------------------------------------------------------------------------------------+
| ``-step [number]``   | The size of each increment.                                                                       |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-offset [number]`` | The number to start the enumeration from.                                                         |
+----------------------+---------------------------------------------------------------------------------------------------+




Examples
========
The following example creates 10 rows starting from 10 with increment of 10.

.. code-block:: gor

   NORROWS -step 10 -offset 10 10

Output::

   #RowNum
   10
   20
   30
   40
   50
   60
   70
   80
   90
   100