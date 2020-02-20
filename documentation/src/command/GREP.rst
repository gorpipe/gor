.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _GREP:

====
GREP
====
The **GREP** command allows through any rows that match the pattern specified for the given column. Regular expressions can be used in the match pattern. The ``-s`` and ``-i`` options are used to specify case sensitivity and insensitivity respectively.

Usage
=====

.. code-block:: gor

	... | GREP matchpattern [-c columns] [-i]

Options
=======

+------------------+------------------------------------------------------------------------------+
| ``-c columns``   | The columns to search for the match pattern.  Default is ALL the columns.    |
+------------------+------------------------------------------------------------------------------+
| ``-s``           | The regular expression pattern match is case-sensitive.                      |
+------------------+------------------------------------------------------------------------------+
| ``-i``           | The regular expression pattern match is case-insensitive (default).          |
+------------------+------------------------------------------------------------------------------+
| ``-v``           | The regular expression pattern match is inverted.                            |
+------------------+------------------------------------------------------------------------------+

Examples
========
The following query streams from the ``#genes#`` table and returns only those rows that have a match to the string "BRCA" in the ``Gene_Symbol`` column.

.. code-block:: gor

   gor #genes# | GREP "BRCA" -c Gene_Symbol

Regular expressions can also be used in the match pattern, as shown in the equivalent query below:

.. code-block:: gor

   gor #genes# | GREP ^BRCA+ -c Gene_Symbol

Further help
============
https://regex101.com/
https://www.cheatography.com/davechild/cheat-sheets/regular-expressions/