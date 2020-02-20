.. _abs:

===
ABS
===

An **ABS** function converts a given number into the absolute value of that number. The function only accepts numerical values.


Usage
=====

``abs(num) : num``

Examples
========

The following example creates a single NOR row with a column x and a value of -42. The second calculated column uses the **ABS** functions to convert x into the absolute value of x, or ``42``.

.. code-block:: gor

   NORROWS 1 | CALC x -42 | CALC y abs(x)

