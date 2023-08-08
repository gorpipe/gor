.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _DEFLATECOLUMN:

=============
DEFLATECOLUMN
=============

The :ref:`DEFLATECOLUMN` command allows you to select a single column and compress its content if it meets the minimum size defined by the ``-m`` option. Use the :ref:`INFLATECOLUMN` to de-compress the column.

Usage
=====

.. code-block:: gor

	gor ... | DEFLATECOLUMN column [ attributes ]


Options
=======

+-------------------+----------------------------------------------------------------------+
| ``-m size``       | Defines the minimum size needed to compress the column. Default size |
|                   | is 100 and minimum size is 10.                                       |
+-------------------+----------------------------------------------------------------------+

The `DEFLATECOLUMN` command will insert `zip::` at the start of the column indicating the the content is compressed, followed by the original column size. Trying to compress a column that is already compressed will return the original compressed column.

Examples
========

.. code-block:: gor

	gor ... | deflatecolumn data -m 20

Returns a row where the column named data is compressed if its content is larger than 20 characters.