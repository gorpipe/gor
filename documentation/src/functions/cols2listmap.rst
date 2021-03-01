.. _cols2listmap:

============
COLS2LISTMAP
============

The **COLS2LISTMAP** function collapses columns into a single column, similar to **COLS2LIST**, applying an expression
to each value as it combines to a list with a separator between the values.
The separator defaults to ",", but can be set to anything.

Usage
=====

``COLS2LISTMAP(string, string) : string``

``COLS2LISTMAP(string, string, string) : string``

Example
=======
.. code-block:: gor

   gor ... | CALC test COLS2LISTMAP('cols*', 'lower(x)')

``test`` now contains the values in lowercase from all columns starting with 'cols', separated by commas.

.. code-block:: gor

   gor ... | CALC test COLS2LISTMAP('cols*', 'lower(x)', ':')

``test`` now contains the values in lowercase from all columns starting with 'cols', separated by colons.

.. code-block:: gor

   gor ... | CALC test IF(CONTAINS(COLS2LISTMAP('cols*', 'lower(x)'), 'foo'), 1, 0)

``test`` now contains 1 if any of the values from all columns starting with 'cols' contain the string 'foo'.
