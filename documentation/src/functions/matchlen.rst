.. _matchlen:

========
MATCHLEN
========

The **MATCHLEN** function is used to measure the length of the longest matching initial substrings between
the two supplied strings,
i.e., the number of characters from the start for which the two strings are identical.

Usage
=====

``MATCHLEN(string, string) : integer``

Example
=======
The following example creates columns called ``ref`` and ``alt`` with string values ``CCATGGA`` and ``CCAGC``,
and computes the length of the common prefix string, ``3``, the length of ``CCA``.

.. code-block:: gor

   gorrow 1,1,1 | CALC ref 'CCATGGA' | CALC alt 'CCAGC' | calc prefixlen MATCHLEN(ref,alt)

