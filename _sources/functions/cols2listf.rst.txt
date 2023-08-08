.. _cols2listf:

=========
COLS2LIST
=========

The **COLS2LIST** function collapses columns into a single column, with a separator between the values.
The separator defaults to ",", but can be set to anything with the optional separator argument.

Usage
=====

``COLS2LIST(string, [sep]) : string``

Example
=======
.. code-block:: gor

   gor ... | CALC test COLS2LIST('cols*')

``test`` now contains the values from all columns starting with 'cols', separated by commas.

.. code-block:: gor

   gor ... | CALC test COLS2LIST('cols*', ':')

``test`` now contains the values from all columns starting with 'cols', separated by colons.

.. code-block:: gor

   gor ... | CALC test IF(CONTAINS(COLS2LIST('cols*'), 'foo'), 1, 0)

``test`` now contains 1 if any of the values from all columns starting with 'cols' contain the string 'foo'.
