.. _left:

====
LEFT
====

The **LEFT** function returns the left-most character or characters in a string. The function takes two parameters, the first being the source string and the second being the number of characters to return.

Usage
=====

``LEFT(string,integer) : string``

Example
=======
The example query returns a column labelled "test" that contains only the left most base in the reference column in the #dbsnp# reference data file.

.. code-block:: gor

   gor #dbsnp# | TOP 10 | CALC test left(reference,1)

The next query returns the first ten rows in #dbsnp# where the left-most base in the reference column is "A".

.. code-block:: gor

   gor #dbsnp# | WHERE left(reference,1) = 'A' | TOP 10