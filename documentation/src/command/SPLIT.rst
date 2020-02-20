.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SPLIT:

=====
SPLIT
=====
The **SPLIT** command outputs *multiple rows* for columns that split into more than one value. If multiple columns are split and they have different number of items, missing values are replaced by an empty cell. Output values are white space trimmed.

Note that a different command, :ref:`COLSPLIT`, is used to split a column into multiple columns based on a separator pattern.

Usage
=====

.. code-block:: gor

	gor file | split cols [ attributes ]

Options
=======

+--------------+------------------------------------------------------------------------------+
| ``-s 'sep'`` | The split separator pattern, e.g. ';' or ',|;', or '[;,]'.                   |
|              | If no ``-s`` option is used, the default the split pattern is ','.           |
+--------------+------------------------------------------------------------------------------+
| ``-e char``  | Character to denote empty field. Defaults to empty string, i.e. of length 0. |
+--------------+------------------------------------------------------------------------------+


Examples
========

The following example shows a GOR query that will split the list into four separate rows. Note that if single quotation marks are used, the individual separator patterns much be separated by the pipe symbol.

.. code-block:: gor

   norrows 1 | CALC x '1,2;3,4' | SPLIT x -s ';|,'

The next query is an equivalent expression using square brackets around the separator patterns, which obviates the need for the pipe symbol.

.. code-block:: gor

   norrows 1 | CALC x '1,2;3,4' | SPLIT x -s '[;,]'