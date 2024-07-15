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

Expressions
===========

The ``expression`` parameter is interpreted as a regular expression.  Its capture group(s)
define how the values for output columns are extracted.

Any of the characters ``(``, ``)``, ``*``, ``+``, ``?``, ``^``, ``$``, ``.``, ``[``, ``]``, and ``|`` will be interpreted as a regular expression
metacharacter unless it is escaped.  To insert one of these to be matched as a literal character, it must be preceded
in the query by *two* backslashes, e.g., ``\\+``.  (The escaping yields a string value containing one backslash.
When that string is used to construct the regular expression, the backslash escapes the metacharacter.)


Examples
========

The example shown below will match the content of the 'source' column and split the content of that column into separate columns 'abc', 'def', and 'ghi' according to the provided matching expression. Notice that there are three pairs of curved brackets in the matching expression, which correspond to the three new columns.

.. code-block:: gor

   gorrow chr1,1,1 | CALC source '41000_1_1' | REGSEL abc,def,ghi source '(.*)_(.*)_(.*)'

The new columns are filled with the matched content from the source column in order of appearance. For those who are used to working with regular expressions, it may be expected that you could tag the matching expresssion with the names of the columns, such '(<?abc>.*)_(<?def>.*)_(<?ghi>.*)' but that is *not* the case here.

If a default value should be entered into the new columns when no match is found, that default value can be entered. In the next example, all the columns will return as '0' because the expression does not match the value in the 'source' column and a default value of '0' has been defined in the expression.

.. code-block:: gor

   gorrow chr1,1,1 | CALC source '41000_1' | REGSEL abc,def,ghi source '(.*)_(.*)_(.*)' -e 0

The next example illustrates splitting an input value at a marker character that happens to be a metacharacter:

.. code-block:: gor

   norrows 1 | calc s 'cov/70.gorz|.cov/5.gorz' | calc match regsel(s, '(.*)\\|.*') | select match

The substring ``\\|`` in ``expression`` is used to match the ``|`` contained in input values, so that all characters before ``|``
are returned as the value for ``match`` (i.e., ``cov/70.gorz``).