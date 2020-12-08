.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SELWHERE:

========
SELWHERE
========

The :ref:`SELWHERE` command allows you to choose which columns to show from the source by by evaluating an expression referring to the column index (starting from 1), or the column name.
The expression is evaluated for each column - if the expression returns true, the column is included.

When selecting in GOR, genomic ordered relational data is expected, so it is mandatory to have a ``CHROM`` and ``POS`` as the first two columns. However, when the SELWHERE statement is used in NOR, you can choose whatever columns you want.  If columns other than the first two are selected to become the first two columns, care must be taken to ensure that proper genomic order is preserved (see discussion on the ``-ir`` option in the :ref:`JOIN` command).

See also the :ref:`SELECT` command.

Usage
=====

.. code-block:: gor

   gor ... | SELWHERE < expression >

Examples
========
The following example shows a SELWHERE command that selects column based on an expression.

.. code-block:: gor

	gor ... | SELWHERE colnum <= 2 or int(right(colname,4)) < 300

Included in this selection are the first two columns (chrom and pos), as well as columns where the numeric value of the last four characters in the column name is less than 300.


