.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _GTGEN:

=====
GTGEN
=====
The **GTGEN** command is used to generate genotypes in a horizontal bucket format, e.g. with one or more rows per variant
(chrom,pos,ref,alt) and ``bucket`` and ``values`` column to store the genotypes as characters (0,1,2,3).  The input is a
input stream of row-based variants with a genotype column (``GT``), a tagbucketrelation and coverage relation.

The ``GT`` column (0,1,2,3) is mapped to a bucket and the horizontal character seat in the ``Values``.
GT in rows with empty tag value (PN) are not put into the value column, but can be used to enforce that
a variant row is present for each bucket (see the -u option in :ref:`CSVCC` and :ref:`CSVSEL`).

A presence of a coverage segment (per tag ``PN``) indicates sufficient coverage, i.e. missing variant data (GT not 0,1, or 3)
is interpreted as homozygous reference.

Usage
=====

.. code-block:: gor

    gor ... | GTGEN tagbucketrelation coveragefile [ attributes ]

Options
=======

+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gc cols``        | Grouping columns other than (chrom,pos)                                                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-tag tagcolname`` | Override the default PN colunm name used to represent the tag value.                               |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-maxseg bpsize``  | The maximum span size of the coverage segments.  Defaults to 10000 (10kb).                         |
+---------------------+----------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

    /* An example of creating horizontal variations stored in buckets */

    create #PNbuckets# = nor -asdict #wesVars# | SELECT #2 | RENAME #1 PN | ROWNUM
    | CALC bucket 'bucket'+str(div((rownum-1),500)+1) | SELECT PN,bucket | SORT -c PN;

    create #allvars# = gor #wesvars# -ff [#PNbuckets#] -s PN | SELECT 1-8,PN | DISTINCT;

    create #horvars# = gor [#allvars#]
    | CALC GT IF( Depth = 9999 OR GL_Call >= 5 AND Depth >= 8 AND
      (CallCopies = 2 AND CallRatio >= 0.66 OR CallCopies = 1 AND CallRatio >= 0.2
      AND CallRatio <= 1.0-0.2), CallCopies, 3)
    | gtgen -gc #3,#4 [#PNbuckets#] <(gor source/cov/goodcov_8.wgs.gord -s PN -ff [#PNbuckets#]);

    /* Selecting a random subset of PNs*/
    gor [#horvars#] | CSVSEL [#PNbuckets#] <(nor [#PNbuckets#] | SELECT PN | WHERE random() < 0.05) -gc #3,#4 -u 3

The above query example generates a tag-bucket relation and a GOR relation with all variants.
The :ref:`GTGEN` command generates horizontal genotypes from the sparse row-based genotypes, based on
the PN-bucket structure, the quality of the variants and the presence of coverage information.  The
output has a ``bucket`` column and ``values`` column storing the genotypes of the PN as a single character (0,1,2, or 3)
according to the position within the bucket.

For comparison, the #horvar# query can also be generated in the following manner using left-join logic, although
it is much less efficient.

.. code-block:: gor

    create #horvars# = gor [#allvars#] | SELECT 1-4 | MULTIMAP -cartesian -h [#PNbuckets#]
    | VARJOIN -r -l -e 0 -xl pn -xr pn <(gor #wesVars# -ff [#PNbuckets#]
    | CALC qual IF( Depth = 9999 OR GL_Call >= 5 AND Depth >= 8 AND
      (CallCopies = 2 AND CallRatio >= 0.66 OR CallCopies = 1 AND CallRatio >= 0.2
      AND CallRatio <= 1.0-0.2),1,-1)
    | SELECT 1-4,PN,callcopies,qual )
    | JOIN -snpseg -l -r -maxseg 10000 -e 0 -xl pn -xr pn
      <(gor source/cov/goodcov_8.wgs.gord -s PN -ff [#PNbuckets#]  | CALC goodcov 1)
    | REPLACE callcopies IF(qual=-1,'NA',IF(callcopies='0' AND goodcov=0,'NA',callcopies))
    | SORT 1 -c #3,#4,PN | GROUP 1 -gc #3-#4,bucket -sc callcopies -lis -len 100000
    | RENAME lis_CallCopies values | SELECT 1-4,values,bucket;

Then horizontal variants are then created by expanding each variants with PN and bucket and followed with a left-join into
the sparse variant table ``#wesVars#`` and the coverage table. The variant stream is then sorted into a PN order, consistent with the tag-bucket relation, and then collapsed into a comma-separated list per variant. The final query reads a random subset of PNs from the ``#horvars#`` relation.

.. code-block:: gor

    /* Selecting 10 PNs from the first bucket and return as rows */

    gor [#horvars#] | CSVSEL -tag PN [#PNbuckets#]
      <(nor [#PNbuckets#] | WHERE bucket = 'bucket1' | SELECT #PN | top 10) -gc #3,#4 -u 4

    /* Selecting 10 PNs from the first bucket, a slightly faster version */

    gor [#horvars#] | WHERE bucket = 'bucket1' | CSVSEL -tag PN [#PNbuckets#]
      <(nor [#PNbuckets#] | WHERE bucket = 'bucket1' | SELECT #PN | top 10) -gc #3,#4 -u 4

The last example show how data is read from a table with horizontal buckets and filtered, partially in the reading of the bucket partitions and also in the :ref:`CSVSEL` step.

.. code-block:: gor

    gor source/hvar/transwgs.gord -nf -ff myPNs.tsv | CSVSEL source/hvar/PNbuckets.tsv myPNs.tsv -gc #3,#4 -u 4


Related commands
----------------

:ref:`CSVCC` :ref:`CSVSEL` :ref:`GTLD`