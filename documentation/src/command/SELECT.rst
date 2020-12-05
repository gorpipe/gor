.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SELECT:

======
SELECT
======

The :ref:`SELECT` command allows you to choose which columns to show from the source by entering a comma-delimited list.

When selecting in GOR, genomic ordered relational data is expected, so it is mandatory to have a ``CHROM`` and ``POS`` as the first two columns. However, when the SELECT statement is used in NOR, you can choose whatever columns you want.  If columns other than the first two (e.g. ``#1`` and ``#2``) are selected to become the first two columns, care must be taken to ensure that proper genomic order is preserved (see discussion on the ``-ir`` option in the :ref:`JOIN` command).

See also the :ref:`SELWHERE` command.

Usage
=====

.. code-block:: gor

   gor ... | SELECT < columns >

Examples
========
The following example shows a SELECT command that includes many different types of notation.

.. code-block:: gor

	gor ... | SELECT 1,2,#3-#5,Cola,7-Colb,Info*,Cola[+1]-Colg[-1],Colm-

Included in this SELECT are: columns 1, 2, 3 through to 5, a column named ``Cola``, columns from 7 up to ``Colb, any column starting with ``Info``,the column right after ``Cola`` up to the column right before ``Colg``, and from ``Colm`` onwards to the end of the column list.

Frequently, the SELECT command is used to limit the number of columns returned by the query. In the following example, we are retrieving only those columns that define the variant calls from the whole genome sequencing variants table:

.. code-block:: gor

	gor source/var/wgs_varcalls.gord
      | select chrom,pos,reference,call
      | distinct
      | top 100



