.. _len:

===
LEN
===

The **LEN** function is used to determined the character length of the given string in a cell.

Usage
=====

``len(string) : integer``

Example
=======
The following example creates a column called 'list' and gives it the value '0123456789'. It then calculates the length of the string, which is 10.

.. code-block:: gor

   gorrow 1,1,1 | CALC list '0123456789' | CALC length len(list)

Note that ``len`` is not counting the length of the list and the following gives a length of 19:

.. code-block:: gor

   gorrow 1,1,1 | CALC list '0,1,2,3,4,5,6,7,8,9' | CALC length len(list)

The length calculate is based on the contents of the cell, not the expression itself so the following gives a length of 2:

.. code-block:: gor

   gorrow 1,1,1 | CALC list 1+2+3+4 | CALC length len(list)
