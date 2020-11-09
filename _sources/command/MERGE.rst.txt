.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _MERGE:

=====
MERGE
=====
The :ref:`MERGE` command combines two independent sources into a single genomic-ordered stream. This is analogous to UNION in SQL.

Usage
=====

.. code-block:: gor

	gor ... | MERGE <(right-source) [ attributes ]

Options
=======

+-------------------+------------------------------------------------------------------------------+
| ``-i``            | Return the INTERSECTION of the columns, instead of the UNION.                |
+-------------------+------------------------------------------------------------------------------+
| ``-u``            | Return the UNION of the columns (the default).                               |
+-------------------+------------------------------------------------------------------------------+
| ``-s``            | Add a source column with the value of L or R.                                |
+-------------------+------------------------------------------------------------------------------+
| ``-e char``       | Character to denote empty field. Defaults to empty string, i.e. of length 0. |
+-------------------+------------------------------------------------------------------------------+
| ``-c cols:order`` | Additional sorting with r (reverse) and n (number) qualifier.                |
+-------------------+------------------------------------------------------------------------------+

Use ``-c`` option to specify the sort order of the sources. This applies in particular to NOR sources
that have been sorted.