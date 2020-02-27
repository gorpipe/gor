.. _iooa:

====
IOOA
====

The **IOOA** function returns an integer number based on the order in which the string value shows up.

Usage
=====

``IOOA(string) : integer``

Example
=======
The query below will create a column ("test") with numbers from 1 to 10 indicating the order in which the values in the "pos" column show up.

.. code-block:: gor

   gorrows -p chr1:0-10 | CALC test IOOA(pos)

