.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _LDANNO:

======
LDANNO
======
The program left-joins the stdin with all the annotation files based on both Pos and Pos2.

Usage
=====

.. code-block:: gor

	gor ... | LDANNO file1.gor [ ... [ fileN.gor ] ... ]  [ attributes ]

Options
=======

+-----------------+--------------------------------------------------------------------------------------------+
| ``-s``          | Only annotate with a single lookup value (in case many are found).                         |
|                 | By default, all annotations are shown, formatted as comma-separated list.                  |
+-----------------+--------------------------------------------------------------------------------------------+
| ``-nt``         | NO to-listing where there is multiple match,                                               |
|                 | i.e. a regular join behavior that may cause multiplication of source rows.                 |
+-----------------+--------------------------------------------------------------------------------------------+
| ``-h``          | Eliminate the header from the output.                                                      |
+-----------------+--------------------------------------------------------------------------------------------+
| ``-e char``     | Character to denote empty field.  Defaults to an empty string, i.e. string of length 0.    |
+-----------------+--------------------------------------------------------------------------------------------+
| ``-pos column`` | The column name for second position.  Defaults to Pos2.                                    |
+-----------------+--------------------------------------------------------------------------------------------+
