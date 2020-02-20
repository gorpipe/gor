.. _brackets:

========
BRACKETS
========

**BRACKETS** is a string-conversion function that adds brackets around a given string.

Usage
=====

``BRACKETS(input) : output``

Example
=======
The example query will create a column called "test" in the output stream which contains the position of the rows contained in brackets.

.. code-block:: gor

   gorrows -p chr1:0-10 | CALC test brackets(pos)