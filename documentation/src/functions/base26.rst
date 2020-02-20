.. _base26:

======
BASE26
======

The **BASE26** function can be used to convert an integer into the hexavigesimal numeral system, or Base 26. The symbols used in this system are 0-9 and A-P.

Usage
=====

``base26(int) : string``

Example
=======
The following example converts the integer 19770612 to Base 26.

.. code-block:: gor

   gorrow 1,1,1 | CALC foo 19770612 | CALC bar base26(foo)