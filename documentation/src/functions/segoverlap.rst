.. _segoverlap:

==========
SEGOVERLAP
==========

The **SEGOVERLAP** function determines whether two segments overlap and returns a Boolean value.


Usage
=====

``SEGOVERLAP(x,y,a,b) : boolean``

Examples
========

The following example shows a single NOR row with two segments ``x-y`` and ``a-b``, which do not overlap. The test column will return 0.

.. code-block:: gor

   NORROWS 1 | CALC x 10 | CALC y 20 | CALC a 25 | CALC b 35 | CALC test segoverlap(x,y,a,b)

In the next example, we show x-y overlapping a-b. The test column in this case will show 1.

.. code-block:: gor

   NORROWS 1 | CALC x 10 | CALC y 20 | CALC a 15 | CALC b 25 | CALC test segoverlap(x,y,a,b)

To calculate the distance between two non-overlapping segments, use the :ref:`segdist` function.

Related Functions
=================

:ref:`segdist`

