.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _REGRESSION:

==========
REGRESSION
==========
The **REGRESSION** command is used to run regression with phenotypes as dependent variables against numbers associated
to the variants and other covariates. The table must contain a column names ``values`` which can either be a list of
numbers or a string of character encoded genotype values. In the latter case, the alternative allele dosage is taken as
an independent variable.

Usage
=====

.. code-block:: gor

    gor ... | REGRESSION phenotypes [-linear] [-logistic] [-covar covariates] [-imp] [-s sep]

Options
=======
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-linear``         | Indicates that a linear regression should be run.                                                  |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-logistic``       | Indicates that a logistic regression should be run.                                                |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-covar covarFile``| If there are some additional covariates. The file containing the covariates should be tab separated|
|                     | with header. The first column should contain the sample identities and the other columns should    |
|                     | contain the covariates.                                                                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-s sep``          | Specify separator for the elements in values, if they are listed as numbers.                       |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-imp``            | To indicate that the value column contains a string of imputed character encoded genotypes.        |
+---------------------+----------------------------------------------------------------------------------------------------+

