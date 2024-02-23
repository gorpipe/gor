.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _FILTERINVALIDROWS:

=================
FILTERINVALIDROWS
=================
The :ref:`FILTERINVALIDROWS` filters out invalid rows, for example rows with too few columns.  This can be useful when working with corrupt data.

Usage
=====

.. code-block:: gor

	gor ... | FILTERINVALIDROWS

Examples
========

.. code-block:: gor

    gor #LD# | FILTERINVALIDROWS | SELECT chrom,pos2,pos,r2 | sort 1000000

The above command filters invalid rows out of the LD data, making sure the following gor commands succeed.