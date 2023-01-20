.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _INFLATECOLUMN:

=============
INFLATECOLUMN
=============
The :ref:`INFLATECOLUMN` command allows you to select a single column and de-compress its content. Use the :ref:`DEFLATECOLUMN` to compress the column.

Usage
=====

.. code-block:: gor

	gor ... | INFLATECOLUMN column


Options
=======

The `INFLATECOLUMN` command will de-compress columns starting with the `zip::` keyword at the start of the column indicating the the content is compressed. Trying to de-compress a column that is already de-compressed will return the original column data.

Examples
========

.. code-block:: gor

	gor ... | inflatecolumn data

Returns a row where the column named `data` is de-compressed if its content is compressed.