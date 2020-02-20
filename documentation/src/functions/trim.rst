.. _trim:

====
TRIM
====

The **TRIM** function is used to trim whitespace from the beginning and end of a string.

Usage
=====

``trim(string) : string``

Example
=======
The following will trim the extra whitespace from around the numerical string (and add square brackets around it to show that the whitespace has been removed.

.. code-block:: gor

   gorrow 1,1,1 | CALC input '          0123456789          ' | CALC test '['+trim(input)+']'

