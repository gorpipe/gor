.. _reverse:

=======
REVERSE
=======

The **REVERSE** function is used to reverse the contents of a string.

Usage
=====

``reverse(string) : string``

Example
=======
The following will result in a column "test" with the value "9876543210".

.. code-block:: gor

   gorrow 1,1,1 | CALC input '0123456789' | CALC test reverse(input)

