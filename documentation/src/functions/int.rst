.. _int:

===
INT
===

An **INT** function can be used to convert a float into an integer.


Usage
=====

``INT(col) : float``

Examples
========

In the example below, we take a float ('foo') and cast into an integer (at which point it becomes 5) and then use the :ref:`isint` within an :ref:`if` statement to evaluate if it is a float or not.

.. code-block:: gor

   gorrow 1,1,1 | CALC foo 5.0 | CALC bar int(foo) | CALC int? IF(ISINT(bar),'true','false')