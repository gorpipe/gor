.. _listcomb:

========
LISTCOMB
========

The **LISTCOMB** function returns a semi-comma separated list of all combinations of elements in the input list of length within the interval specified by the input integers. The input string must be a comma-separated list and the output combinations are comma separated lists. E.g. listcomb('a,b,c',1,2) returns 'a,b;a,c;b,c;a;b;c'. The second integer can be skipped and then the function only a list of combinations of elements in the input list of length equal to the integer.

Usage
=====

``LISTCOMB(string,integer,integer) : string``

Example
=======
The example query does something.

.. code-block:: gor

   gorrow chr1,1,1