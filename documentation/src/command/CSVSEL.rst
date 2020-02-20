.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _CSVSEL:

======
CSVSEL
======
The **CSVSEL** command is used to select a subset of data stored in a horizontal manner, as compared to data on a row level.
Many analysis commands, such as regression and case-contol can leverage this representation for efficient evaluation.

The left-input stream must contain the columns ``values`` and ``bucket``.  The tag-bucket relation is used to define
how the values for different tags are stored in the value column in each bucket.  The tag selection specifies which
tags are selected.  The output is a new value row with the tag values in a single column and a single row (buckets are collapsed).
If the ``-tag`` option is used, the tag values are output in separate rows.

The ``-u`` option provides the flexibility of having sparse buckets on row-level, i.e. "missing" rows.  As an example, when
variants are stored in this format, old buckets that were generated in the past might have less total number of variants
than those newly generated.  This option allows the output value for instance to be either homozygous reference or a value
denoting missing data.

Data stored horizontally in tables (dictionaries) uses multi-tags for each bucket file.  Thus, when the data is read,
the selection on the dictionary typically uses no filtering on the rows, i.e. ``-nf``, since it is assumed that a subset of the tags
should return the entire row.  Thus, the filtering options ``-f`` and ``-ff`` in the :ref:`GOR` command are not used to filter
the data, but rather the :ref:`CSVSEL` command.

Usage
=====

.. code-block:: gor

    gor ... | CSVSEL tagbucketrelation tagselection [-gc cols] [-u undefsymbol] [-tag tagcolname] [-s sep] [-vs charsize]

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
|                     | Default value is 4.  Typical values can be 3, 4 or NA.                                             |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-tag colname``    | This option causes the horizontal values representing the individual tags to be unpivoted into     |
|                     | multiple rows and the column named as specified.                                                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-hide blacklist`` | This option is only to be used together with the -tag option. The option hides those rows whose    |
|                     | corresponding value from the horizontal data, is on the blacklist. The blacklist should be a comma |
|                     | separated list of strings.                                                                         |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-vcf``            | Takes the cropped out strings, X, which are assumed to be of length 1, computes DECODE(CHARS2GT(x))|
|                     | of them and writes the results in separate columns.                                                |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-threshold thres``| Only to be used together with the -vcf option. Takes the cropped out strings, X, which are assumed |
|                     | to be of length 2, computes DECODE(CHARS2GT(x,thres),‘0’,‘0/0’,‘1’,‘0/1’,2,‘1/1’,‘./.’) of them.   |
|                     | Writes the results in separate columns.                                                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-dose``           | Only to be used together with the -vcf option. Instead of using 'chars2prprpr' the function        |
|                     | 'chars2dose' is used.                                                                              |
+---------------------+----------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

    /* An example of creating horizontal variations stored in buckets */

    create #PNbuckets# = nor -asdict #wesVars# | SELECT #2 | RENAME #1 PN | ROWNUM
    | CALC bucket 'bucket'+str(div((rownum-1),500)+1) | SELECT PN,bucket | SORT -c PN;

    create #allvars# = gor #wesvars# -ff [#PNbuckets#] | SELECT 1-4 | DISTINCT;

    create #horvars# = gor [#allvars#]
    | CALC GT IF( Depth = 9999 OR GL_Call >= 5 AND Depth >= 8 AND
      (CallCopies = 2 AND CallRatio >= 0.66 OR CallCopies = 1 AND CallRatio >= 0.2
      AND CallRatio <= 1.0-0.2), CallCopies, 3)
    | gtgen -gc #3,#4 [#PNbuckets#] <(gor source/cov/goodcov_8.wgs.gord -s PN -ff [#PNbuckets#]);

    /* Selecting a random subset of PNs*/
    gor [#horvars#] | CSVSEL [#PNbuckets#] <(nor [#PNbuckets#] | SELECT #PN
    | WHERE random() < 0.05) -gc #3,#4 -u 4

The above query example generates a tag-bucket relation and a GOR relation with all variants.  Then horizontal variants are then created by expanding each variants with PN and bucket and followed with a left-join into the sparse variant table ``#wesVars#`` and the coverage table. The variant stream is then sorted into a PN order, consistent with the tag-bucket relation, and then collapsed into a comma-separated list per variant. The final query reads a random subset of PNs from the ``#horvars#`` relation.

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

:ref:`CSVCC` :ref:`GTGEN` :ref:`GTLD`