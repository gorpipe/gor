.. _substr:

======
SUBSTR
======

The **SUBSTR** function retrieves a portion of a string based on two integers that indicate the beginning and end point. The function takes three parameters - the source of the string and the start and end position in the string. The position indicators are zero-based, so setting a start point of zero will take the substring from the very beginning of the source string and a start point of one will start from the second character in the string and so on.

Usage
=====

``SUBSTR(string,integer,integer) : string``

Example
=======
The example query shown here will return a stream of rows that contain a reference value with a length of 5 bases. It will then create a column called "test" with a substring of the reference column minus the first and last character in the string (i.e. characters 2 to 4 inclusive).

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) = 5 | CALC test substr(reference,1,4) | TOP 10

If we only wanted to take the middle character from the string we would have to define the start and end points as follows:

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) = 5 | CALC test substr(reference,2,3) | TOP 10
