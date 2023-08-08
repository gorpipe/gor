.. _eform:

=====
EFORM
=====

The **eform** function formats a number with scientific notation, specifying the space and decimals (C-style %x.ye format style).

The function takes three parameters: the number you are formatting, the width of the output, and the number of
decimal places to include.

Usage
=====

``EFORM(num,integer,integer) : string``

Example
=======
The query below formats the number given to 2 decimal places, resulting in an output of 3.14e0.

.. code-block:: gor

   gorrow 1,1,1 | CALC test EFORM(3.14159,1,2)

If you are using the number in an output string that is intended to be read as is, care must be taken to allow for enough spaces in the output number, which is the purpose of the first integer parameter. For example in the following example, a string width of ``5`` is given to allow for the 3 decimal places, the decimal itself, and the space between the number and the text.

.. code-block:: gor

   gorrow 1,1,1 | CALC test EFORM(3.14159,5,2) | CALC new 'this is a number '+test

