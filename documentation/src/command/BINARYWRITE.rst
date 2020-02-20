.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _BINARYWRITE:

===========
BINARYWRITE
===========

The :ref:`BINARYWRITE` command can be used to write the content of a GOR stream into a .pgen or .bgen file.

The incoming stream must have a column containing the reference allele, another containing the alternative allele and a
third containing genotype values.

Usage
=====

.. code-block:: gor

	gor ... | binarywrite filename [-imp] [-gv] [-threshold d]

Options
=======

+------------------+----------------------------------------------------------------------+
| ``-imp``         | Indicates that the value column contains imputed genotypes.          |
+------------------+----------------------------------------------------------------------+
| ``-gv``          | Causes lines with same chr, pos, ref to be written out as single     |
|                  | multi-allelic variants.                                              |
+------------------+----------------------------------------------------------------------+
| ``-threshold d`` | The threshold to use when converting imputed genotypes to hard-calls.|
|                  | The default value is 0.9.                                            |
+------------------+----------------------------------------------------------------------+

Examples
========

.. code-block:: gor

    gor imputedGenotypeDataInHorizontalBucketFormat.gorz | csvsel buckets.tsv pns.tsv -gc #3,#4,#5 -vs 2 | binarywrite -imp -threshold 0.95 pgenFile.pgen

The above query will write the hardcalls and dosage values parsed from imputedGenotypeDataInHorizontalBucketFormat.gorz to pgenFile.pgen

.. code-block:: gor

    gor hardCallsInHorizontalBucketFormat.gorz | csvsel buckets.tsv pns.tsv -gc #3,#4,#5 -vs 1 | binarywrite pgenFile.pgen

The above query will write the hardcalls from hardCallsInHorizontalBucketFormat.gorz to pgenFile.pgen