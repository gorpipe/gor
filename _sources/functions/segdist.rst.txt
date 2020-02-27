.. _segdist:

=======
SEGDIST
=======

The **SEGDIST** function calculates the distance between two segments (``x-y`` and ``a-b`` in the examples below), which is inclusive of the start and end points of the segments that are being compared. If there is no distance between them, the segments are overlapping and the function returns 0.

It doesn't matter which segment is defined first. The distance can be determined whether x-y comes before or after a-b.


Usage
=====

``SEGDIST(x,y,a,b) : int``

Examples
========

The following example shows a single NOR row with two segments ``x-y`` and ``a-b``. The difference between the end of the first segment and the beginning of the second is 6 (inclusive of the end and start points).

.. code-block:: gor

   NORROWS 1 | CALC x 10 | CALC y 20 | CALC a 25 | CALC b 35 | CALC test segdist(x,y,a,b)

In the next example, we show x-y appearing after a-b in sequence. The result is the same.

.. code-block:: gor

   NORROWS 1 | CALC x 25 | CALC y 35 | CALC a 10 | CALC b 20 | CALC test segdist(x,y,a,b)

Finally, we give the example of two segments that are overlapping. In this case, the function returns 0. Again, it doesn't matter which segment is first in sequence. To check whether two segments overlap, use the :ref:`segoverlap` function instead.

.. code-block:: gor

   NORROWS 1 | CALC x 10 | CALC y 20 | CALC a 15 | CALC b 25 | CALC test segdist(x,y,a,b)

Related Functions
=================

:ref:`segoverlap`

