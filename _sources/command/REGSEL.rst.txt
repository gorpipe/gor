.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _REGSEL:

======
REGSEL
======

The **REGSEL** uses a regular expression to select parts of a given column and output them in new columns.

Usage
=====

.. code-block:: gor

    gor ... | REGSEL columns sourceColumn expression (-e emptyValue)

Options
=======

+--------------+------------------------------------------------------------------------------+
| ``-e char``  | Character to denote empty field. Defaults to empty string, i.e. of length 0. |
|              | This is used when the expression doesn't produce a match.                    |
+--------------+------------------------------------------------------------------------------+

Examples
========

The example shown below will match the content of the 'source' column and split the content of that column into separate columns 'abc', 'def', and 'ghi' according to the provided matching expression. Notice that there are three pairs of curved brackets in the matching expression, which correspond to the three new columns.

.. code-block:: gor

   gorrow chr1,1,1 | CALC source '41000_1_1' | REGSEL abc,def,ghi source '(.*)_(.*)_(.*)'

The new columns are filled with the matched content from the source column in order of appearance. For those who are used to working with regular expressions, it may be expected that you could tag the matching expresssion with the names of the columns, such '(<?abc>.*)_(<?def>.*)_(<?ghi>.*)' but that is *not* the case here.

If a default value should be entered into the new columns when no match is found, that default value can be entered. In the next example, all the columns will return as '0' because the expression does not match the value in the 'source' column and a default value of '0' has been defined in the expression.

.. code-block:: gor

   gorrow chr1,1,1 | CALC source '41000_1' | REGSEL abc,def,ghi source '(.*)_(.*)_(.*)' -e 0