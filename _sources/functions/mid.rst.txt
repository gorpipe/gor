.. _mid:

======
MID
======

The **MID** function extracts a portion of a source string based on two integers: the zero-based start point and the size of the string to extract.

The function takes three parameters - the source of the string, the start point, and the size of the string to extract. The position indicator is zero-based, meaning that setting a start point of zero will take from the very beginning of the source string and a start point of one will start from the second character in the string and so on.

Usage
=====

``MID(string,integer,integer) : string``

Example
=======
The example query shown here will return a stream of rows that contain a reference value with a length of 5 bases. It will then create a column called "test" with a substring of the reference column minus the first character in the source string.

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) = 5 | CALC test mid(reference,1,4) | TOP 10

If we only wanted to take the middle character from the string we would have to define the start and end points as follows:

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) = 5 | CALC test mid(reference,2,1) | TOP 10