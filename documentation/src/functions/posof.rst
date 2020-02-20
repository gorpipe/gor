.. _posof:

=====
POSOF
=====

The **POSOF** function returns the position of a string within a source string. The function takes two parameters: the source string and the string for which you wish to find the position. If the string is not found, ``-1`` is returned instead of the position. If the string is found more than once, the position is only returned for the first instance.

Usage
=====

``POSOF(string,string) : output``

Example
=======
The query below reports the position for the first instance it finds of ``A`` in the reference column for ``#dbsnp#``.

.. code-block:: gor

   gor #dbsnp# | CALC test posof(reference, 'A') | TOP 20