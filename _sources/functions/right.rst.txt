.. _right:

=========
RIGHT
=========

The **RIGHT** function returns the right-most character or characters in a string. The function takes two parameters, the first being the source string and the second being the number of characters to return.

Usage
=====

``RIGHT(string,integer) : string``

Example
=======
The example query returns a column labelled "test" that contains only the right-most base in the reference column in the #dbsnp# reference data file.

.. code-block:: gor

   gor #dbsnp# | TOP 10 | CALC test right(reference,1)

The next query returns the first 10 rows where the right-most base in the reference column is "G".

.. code-block:: gor

   gor #dbsnp# | WHERE right(reference,1) = 'G' | TOP 10