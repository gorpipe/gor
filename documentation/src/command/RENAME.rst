.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _RENAME:

======
RENAME
======
The :ref:`RENAME` command enables you to rename the column in the output of the GOR query. The column that you are
renaming does not need to be referred to by name, but may be referred to by it's column number in the same way as is
used by the :doc:`SELECT` command. You should not use a number when naming the columns, as this will result in an error.

As with other column renaming commands, any collisions in column names will result in an "x" being appended to one of
the column names in the GOR query  output.

Rename does also supports regular-expression with binding variables to rename multiple columns.

Usage
=====

.. code-block:: gor

	gor ... | RENAME oldColname newColname

OPTIONS
=======

+----------------------+-------------------------------------------------------------------+
| ``-s``               | Strict - report an error if no match for column is found          |
+----------------------+-------------------------------------------------------------------+

Examples
========
The following query will eliminate set\_ from the beginning of column names:

.. code-block:: gor

	gor ... | RENAME set\_(.*) #{1}

and

.. code-block:: gor

	gor ... | RENAME (.*)_VEP(.*) VEP#{2}

will rename columns like set_VEPa and lis_VEPb to VEPa and VEPb, respectively.


