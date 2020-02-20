.. _basepn:

======
BASEPN
======

The **BASEPN** function can be used to convert an integer into a PN-like seven-letter code.

Usage
=====

``basepn(int) : string``

Example
=======
The following example creates a stream of 101 gor rows with positions from 0 to 100 on chromsome 1 and uses a calc statement to convert the position column to "Base PN".

.. code-block:: gor

   gorrows -p chr1:0-100 | CALC PN basepn(pos)