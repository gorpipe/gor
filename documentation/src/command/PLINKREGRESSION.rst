.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _PLINKREGRESSION:

===============
PLINKREGRESSION
===============

The :ref:`PLINKREGRESSION` is used to run Plink's --glm command on the incoming source. The incoming source must have columns
containing reference and alternative alleles and character encoded genotype values.

Usage
=====

.. code-block:: gor

	gor ... | PLINKREGRESSION phenoFile [-imp] [-threshold d] [-covar covFile]

Options
=======

+------------------+----------------------------------------------------------------------+
| ``-imp``         | Indicates that the value column contains imputed genotypes.          |
+------------------+----------------------------------------------------------------------+
| ``-threshold d`` | The threshold to use when converting imputed genotypes to hardcalls. |
|                  | The default value is 0.9.                                            |
+------------------+----------------------------------------------------------------------+
| ``-firth``       | For Firth test.                                                      |
+------------------+----------------------------------------------------------------------+
| ``-covar covs``  | To provide covariates.                                               |
+------------------+----------------------------------------------------------------------+
| ``-hwe thres``   | Hardy-Weinberg equilibrium threshold to filter on.                   |
+------------------+----------------------------------------------------------------------+
| ``-geno thres``  | The --geno option on plink2 regression.                              |
+------------------+----------------------------------------------------------------------+
| ``-maf thres``   | The --maf option on plink2 regression.                               |
+------------------+----------------------------------------------------------------------+
| ``-dom``         | Use dominant model.                                                  |
+------------------+----------------------------------------------------------------------+
| ``-rec``         | Use recessive model.                                                 |
+------------------+----------------------------------------------------------------------+
| ``-cvs``         | The --covar-variance-standardize option on plink2 regression.        |
+------------------+----------------------------------------------------------------------+
| ``-vs``          | The --variance-standardize option on plink2 regression.              |
+------------------+----------------------------------------------------------------------+
| ``-qn``          | The --quantile-normalize option on plink2 regression.                |
+------------------+----------------------------------------------------------------------+
| ``-hc``          | The hide-covar option on plink2 regression.                          |
+------------------+----------------------------------------------------------------------+
| ``-1``           | Use 0/1 phenotypes instead of 1/2.                                   |
+------------------+----------------------------------------------------------------------+

Flags used in this command are directly from the PLINK2 command line tool, which is documented on the following website.

See: https://www.cog-genomics.org/plink/2.0/general_usage

Examples
========

The following general example shows how PLINK2 can be used within a GOR command.

.. code-block:: gor

    def ##ref## = ref; def ##freqmax## = ##ref##/freq_max.gorz; def #ref/genes.gorz# = ##ref##/genes.gorz;
    create ##PNs## = nor user_data/rep/PhenoGrid_example.rep | select PN | distinct;
    create ##pheno## = nor user_data/rep/PhenoGrid_example.rep | rename PN IID | select IID,2- | distinct;
    create ##covar## = nor user_data/rep/CovarGrid_example_PLINK2.rep | rename PN IID | select IID,2- | inset -c IID [##pheno##];
    create xxx = gor -p chr22 source/hvar/ukbb_array.gord -nf | skip -111 | CSVSEL source/hvar/ukbb_array_buckets.tsv [##PNs##] -u 3 -gc 3,4,5 -vs 1
    | PLINKREGRESSION [##pheno##] -covar [##covar##];
    gor [xxx] | group chrom -count

In the next example, we show an example where we are running the command on directly measured genotypes:

.. code-block:: gor

    pgor -split 500 UKBB/genotype_array/array.gord -nf | CSVSEL UKBB/genotype_array/array_buckets.tsv [##PNs##] -u 3 -gc 3,4,5 -vs 1
 | PLINKREGRESSION [##pheno##] -geno 0.1 -hwe 1.0E-50 -maf 0.01 -hc -covar [##covar##]

When running on imputed data, we would use the following:

.. code-block:: gor

    pgor -split 500 UKBB/genotype_array/imputed.gord -nf -ff [##PNs##] | CSVSEL UKBB/genotype_array/imputed_buckets.tsv [##PNs##] -u ' ' -gc 3,4,5 -vs 2
 | PLINKREGRESSION [##pheno##] -threshold 0.9 -geno 0.1 -hwe 1.0E-50 -maf 0.01 -hc -covar [##covar##]