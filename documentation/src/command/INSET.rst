.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _INSET:

=====
INSET
=====
The :ref:`INSET` command only passes rows where the column value is found in the setfile, unless the option ``-b`` is used. The setfile must be a single column file or a <(nor ... ) expression.

Usage
=====

.. code-block:: gor

	gor ... | INSET setfile.txt -c PN

Options
=======

+---------------+----------------------------------------------------------+
| ``-c column`` | The column which the lookup is based upon.               |
+---------------+----------------------------------------------------------+
| ``-cis``      | Case-insensitive column lookup.                          |
+---------------+----------------------------------------------------------+
| ``-b``        | Return 1 (true) or 0 (false) to indicate set membership. |
+---------------+----------------------------------------------------------+
| ``-n name``   | Override the default value output column name (inSet).   |
+---------------+----------------------------------------------------------+
| ``-not``      | Negate the condition, i.e. NOTINSET.                     |
+---------------+----------------------------------------------------------+
