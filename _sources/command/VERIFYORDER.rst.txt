.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VERIFYORDER:

===========
VERIFYORDER
===========
The :ref:`VERIFYORDER` command ensures that the genomic order of the stream is correct and raises an exception if the order of the stream is violated.

Usage
=====

.. code-block:: gor

	gor ... | VERIFYORDER

Examples
========

.. code-block:: gor

    gor #LD# | SELECT chrom,pos2,pos,r2 | sort 1000000 | VERIFYORDER

The above command orders the LD data by the second column and ensures that the order is correct, i.e. checks if the binsize that is provided for the sort is large enough.