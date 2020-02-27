.. _isint:

=====
ISINT
=====

An **ISINT** function can be used to determine if the value of a column is an integer and can be used within an :ref:`IF`.


Usage
=====

.. code-block:: gor

	ISINT(col) : bool

Examples
========

In the example below, we take an integer ('foo'), change it using a integer type conversion into a integer (at which point it becomes 5) and then use the **ISINT** within an :ref:`if` statement to evaluate if it is a float or not. (It is.)

.. code-block:: gor

   gorrow 1,1,1 | CALC foo 5.0 | CALC bar int(foo) | CALC int? IF(ISINT(bar),'true','false')