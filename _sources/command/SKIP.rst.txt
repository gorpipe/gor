.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SKIP:

====
SKIP
====
The :ref:`SKIP` command specifies how many rows are skipped (blocked) before the first rows are passed through.

Usage
=====

.. code-block:: gor

	gor ... | SKIP number

Examples
========

In the following table we have five rows:

**example.gor**

+---------+--------------+-----------+------------+
| CHROM   | POS          | reference | alternate  |
+---------+--------------+-----------+------------+
| chr1    | 10020        | AA        | A          |
+---------+--------------+-----------+------------+
| chr1    | 10039        | A         | C          |
+---------+--------------+-----------+------------+
| chr1    | 10043        | T         | A          |
+---------+--------------+-----------+------------+
| chr1    | 10051        | A         | G          |
+---------+--------------+-----------+------------+
| chr1    | 10055        | T         | A          |
+---------+--------------+-----------+------------+

If we execute the following GOR query:

.. code-block:: gor

	gor example.gor | skip 2

we will be left with the following output:

+---------+--------------+-----------+------------+
| CHROM   | POS          | reference | alternate  |
+---------+--------------+-----------+------------+
| chr1    | 10043        | T         | A          |
+---------+--------------+-----------+------------+
| chr1    | 10051        | A         | G          |
+---------+--------------+-----------+------------+
| chr1    | 10055        | T         | A          |
+---------+--------------+-----------+------------+