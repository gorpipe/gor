.. _md5:

===
MD5
===

The **MD5** function uses the md5 message-digest algorithm to produce a 128-hash of the value of the cell.

Usage
=====

``md5(string) : string``

Example
=======


.. code-block:: gor

   gorrow 1,1,1 | CALC input '0123456789' | CALC test md5(input)

