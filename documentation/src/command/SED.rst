.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SED:

===
SED
===
The **SED** command works as a search and replace on the GOR stream to which it is applied. You can use the ``-c`` option to specify which columns you wish to apply the command to, but the default is all columns.

The patterns for match and replace are in the form of regular expressions. To make the regular expressions case-insensitive, you can use the ``-i`` option, but this does not work in regular expressions with binding/group variables.

Use the ``-f`` option to only replace the first match in each column.

Usage
=====

.. code-block:: gor

   gor ... | SED matchpattern replacepattern [-c columns] [-i] [-f]

Options
=======

+----------------+--------------------------------------------------------------------------------------+
| ``-c columns`` | The columns where to search for the pattern and replace. Default is ALL the columns. |
+----------------+--------------------------------------------------------------------------------------+
| ``-i``         | The regular expression pattern match is case-insensitive.                            |
+----------------+--------------------------------------------------------------------------------------+
| ``-f``         | Replace only the first match in each column (default is ALL).                        |
|                | The pattern is applied for each column specified (except columns #1 and #2).         |
+----------------+--------------------------------------------------------------------------------------+

For more information on regular expressions, the user can refer to any number of online guides, such as https://www.regular-expressions.info/refquick.html.

.. _a link: https://www.cheatography.com/davechild/cheat-sheets/regular-expressions/