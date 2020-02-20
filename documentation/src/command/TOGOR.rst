.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _TOGOR:

=====
TOGOR
=====
The :ref:`TOGOR` command is used to switch from NOR mode to a GOR mode, making GOR specific commands such as join available on NOR data. The user has to make sure the Chrom,Pos columns are provided.

Usage
=====

.. code-block:: gor

	nor ... | TOGOR | [GOR specific commands]

Examples
========
The NOR data could have chromosome and position columns in different location than GOR. Rearrange columns and then TOGOR:

.. code-block:: gor

    nor left.tsv | select #3,#4,#1,#2 | togor | join -snpsnp right.gorz

or generate chrom,pos columns:

.. code-block:: gor

    nor ref/genes.gor | calc c,p 'chrN','1' | select c,p,Gene_Symbol