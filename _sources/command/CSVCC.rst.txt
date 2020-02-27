.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _CSVCC:

=====
CSVCC
=====
The **CSVCC** command is used aggregate or count genotypes stored in horizontal CSV format.  See :ref:`CSVSEL` for more
description of the requirements for the data input.

The aggregation is grouped according to the phenotype relation, which can be of the form (tag,status) or (tag,pheno,status).
Typically, the relation is a binary relation with PN and case vs control status.  Affection status for multiple phenotypes
can be represented by using the class.

Usage
=====

.. code-block:: gor

    gor ... | CSVCC tagbucketrelation phenorelation [-gc cols] [-u undefsymbol] [-s sep] [-vs charsize]

Options
=======

+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gc cols``        | Grouping columns other than (chrom,pos)                                                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-s sep``          | Specify separator for the elements in values.  Default to comma.                                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-vs size``        | Use a fixed character size for values, e.g. rather than variable length separated with comma.      |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-u undefsymbol``  | The symbol to use in case a bucket does not have the corresponding position and grouping columns.  |
|                     | Default value is 4.  Typical values are 3, 4 or NA. Defaults to 3.                                 |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-probunphased``   | The input is probabilistic genotypes, two chars, representing Pr(gt=1) and Pr(gt=2).               |
|                     | The probability is Phred-like, defining probabilities from 1(!) to 0(~) as round((1.0-pr)*93.0)+33 |
|                     | With this option the GTcount column is floating point number, unless threshold is used.            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-probphased``     | The input is probabilistic haplotypes, two chars, representing Pr(father=Alt) and Pr(mother=Alt    |
|                     | Chars defined as in unphased.                                                                      |
|                     | Pr(gt=1) = Pr(father=alt)*(1-Pr(mother=Alt))+Pr(mother=alt)*(1-Pr(father=Alt)) etc                 |
|                     | With this option the GTcount column is floating point number, unless threshold is used.            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-threshold val``  | A threshold to discretise the genotypes.  Typical values in the range 0.75-1.0                     |
|                     | This causes gt=3 to be used if the Pr(gt=n) < threshold, for all n in 0,1,2                        |
+---------------------+----------------------------------------------------------------------------------------------------+


Examples
========

With `[#PNbuckets#]` and `[#horvars#]` defined analogous manner as in :doc:`CSVSEL`, we could perform case-control analysis as

.. code-block:: gor

    /* Creating arbitrary case control status on PNs */

    create #casecontrol# = nor [#PNbuckets#] | select #PN | calc ccstatus = if(random()>0.5,'CASE','CTRL');

    gor [#horvars#] | CSVCC [#PNbuckets#] [#casecontrol#] -gc #3,#4 -u 3
    | PIVOT -gc reference,call,ccstatus GT -v 0,1,2,3 -e 0
    | PIVOT -gc reference,call -v CASE,CTRL ccstatus -e 0
    | CALC pVal_mm EFORM(PVAL(case_2_GTcount*2+case_1_GTcount,case_0_GTcount*2
                              + case_1_GTcount,ctrl_2_GTcount*2+ctrl_1_GTcount,ctrl_0_GTcount*2 + ctrl_1_GTcount),5,1)


The above query uses :doc:`CSVSEL` to count the number of different allele states; 0 (homozygous reference), 1 (heterozygous), 2 (homozygous) and 3 (unknown)
per variant, e.g. (chrom,pos,reference,call).  Using ``-u 3`` means that absence of data is treated as a "stored unknown".  By using a
value such as 0, one can for instance assume that absence of data means homozygous reference.  The output column GT represents this
genotype allele state (or unknown status) and GTcount the number of subject harboring the corresponding genotype.  The pivot commands
transform the output into multiple horizontal columns representing the different statuses for cases and controls.

It is also possible to perform case-control statistics for multiple phenotypes, using the same variant input stream

.. code-block:: gor

    /* Creating arbitrary case control status on PNs */

    create #phenocasecontrol# = nor [#PNbuckets#] | SELECT #PN
    | CALC phenotype = 'PhenoA,PhenoB' | SPLIT phenotype | CALC ccstatus = if(random()>0.5,'CASE','CTRL');

    gor [#horvars#] | CSVCC [#PNbuckets#] [#casecontrol#] -gc #3,#4 -u 3
    | PIVOT -gc reference,call,pheno,ccsatus GT -v 0,1,2,3 -e 0
    | PIVOT -gc reference,call,pheno -v CASE,CTRL ccstatus -e 0
    | CALC pVal_mm EFORM(PVAL(case_2_GTcount*2+case_1_GTcount,case_0_GTcount*2
                              + case_1_GTcount,ctrl_2_GTcount*2+ctrl_1_GTcount,ctrl_0_GTcount*2 + ctrl_1_GTcount),5,1)
    | SELECT 1,2,reference,call,pheno,pVal_mm
    | PIVOT -gc reference,call pheno -v 'PhenoA,PhenoB' -e 'NA'

Related commands
----------------

:ref:`CSVSEL`