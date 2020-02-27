.. _unbracket:

=========
UNBRACKET
=========

The **UNBRACKET** function removes brackets from the start and end of a given string.

Usage
=====

``UNBRACKET(string) : string``

Example
=======
The example query here will remove the brackets from around the text "fooblat".

.. code-block:: gor

   gorrow chr1,1,1 | CALC test unbracket('(fooblat)')

Note that brackets will only be removed if they are at the beginning and end of the string. Therefore, the following
will not remove the brackets.

.. code-block:: gor

   gorrow chr1,1,1 | CALC test unbracket('xx(fooblat)xx')