.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VARJOIN:

=======
VARJOIN
=======
The :ref:`VARJOIN` command behaves similarly as a join with additional constraint that the columns denoting the
reference and alternative allele are equal in both the left- and the right-source.

The command does join on single alleles, e.g. (ref,all), or multi-alleles (ref,a1,a2) or (ref,a1/a2).
Also, the variations only have to be equivalent and not necessarily represented in the same manner,
i.e. (chr1,2,'A','C') and (chr1,1,'AA','AC') are equivalent representations of the same variants.

The system can be configured to assume normalized variants.  In that case, the varjoin defaults to ``-norm`` and
``-nonorm`` is needed for the dynamic normalization (-span relates to its function).  If configured the other way
around (like we do now) ``-norm`` skips the dynamic normalization on indels (the implementation is equivalent
but faster than to apply varnorm first).

.. code-block:: gor

	varjoin -nonorm == varnorm | join -snpsnp -xl ref,alt -xr ref,alt

.. code-block:: gor

	varjoin -norm == join -snpsnp -xl ref,alt -xr ref,alt

Usage
=====

.. code-block:: gor

	gor ... | VARJOIN rightfile.gor [ attributes ]

Options
=======

+--------------------+---------------------------------------------------------------------------------------+
| ``-ref col``       | The column denoting the reference seq. (for both left- and right-source).             |
+--------------------+---------------------------------------------------------------------------------------+
| ``-alt col``       | The column denoting the alternate (alleles) seq. (for both left- and right-source).   |
+--------------------+---------------------------------------------------------------------------------------+
| ``-refl col``      | The column denoting the reference seq. in left-source.                                |
+--------------------+---------------------------------------------------------------------------------------+
| ``-altl col``      | The column denoting the alternate (alleles) seq. left-source.                         |
+--------------------+---------------------------------------------------------------------------------------+
| ``-refr col``      | The column denoting the reference seq. right-source.                                  |
+--------------------+---------------------------------------------------------------------------------------+
| ``-altr col``      | The column denoting the alternate (alleles) seq. right-source.                        |
|                    | By default, ref and alt are in columns named Ref and Alt,                             |
|                    | Reference and Call or columns #3 and #4.                                              |
+--------------------+---------------------------------------------------------------------------------------+
| ``-as share``      | Defines the multi-allelic sharing threshold.                                          |
|                    | If not specified single allele (alt) is assumed with a threshold of 1.                |
+--------------------+---------------------------------------------------------------------------------------+
| ``-xl cols``       | The columns to perform additional equi-join in the left-source.                       |
+--------------------+---------------------------------------------------------------------------------------+
| ``-xr cols``       | The columns to perform additional equi-join in the right-source.                      |
+--------------------+---------------------------------------------------------------------------------------+
| ``-xcis``          | Use case insensitive equi-join.                                                       |
+--------------------+---------------------------------------------------------------------------------------+
| ``-maxseg size``   | The maximum ref segment size. Defaults to 1kb.                                        |
+--------------------+---------------------------------------------------------------------------------------+
| ``-l``             | Left-join style overlap, shows all rows in the left-source.                           |
+--------------------+---------------------------------------------------------------------------------------+
| ``-n``             | Return only rows from leftfile.gor that do NOT overlap with rightfile.gor.            |
+--------------------+---------------------------------------------------------------------------------------+
| ``-i``             | Return only rows from leftfile.gor that are included in the overlap                   |
|                    | (without the columns from the right-source).                                          |
+--------------------+---------------------------------------------------------------------------------------+
| ``-ic``            | Return the rows in left-source and a column named overlap.                            |
|                    | Count which shows how many rows in right-source overlap.                              |
+--------------------+---------------------------------------------------------------------------------------+
| ``-ir``            | Return the rows from right-source that overlap with the left-source.                  |
|                    | Depending on the nature of the left-source and the overlap condition,                 |
|                    | the join may have to be followed with SORT step to ensure GOR.                        |
+--------------------+---------------------------------------------------------------------------------------+
| ``-rprefix name``  | A prefix to be added to the columns in the right-source.                              |
+--------------------+---------------------------------------------------------------------------------------+
| ``-r``             | Reduced output. Do not include ref/reference and alt/call from the right source.      |
+--------------------+---------------------------------------------------------------------------------------+
| ``-s colname``     | Source column name (for GOR dictionary database file)                                 |
+--------------------+---------------------------------------------------------------------------------------+
| ``-e char``        | Character to denote empty field. Defaults to empty string, i.e. of length 0.          |
+--------------------+---------------------------------------------------------------------------------------+
| ``-norm``          | Assume left or right normalised variants, i.e. -span is zero.                         |
|                    | Skip dynamic normalization on indels. Equivalent to VARNORM, but faster.              |
+--------------------+---------------------------------------------------------------------------------------+
| ``-nonorm``        | Do NOT assume left or right normalised variants, i.e. ambiguity of InDels             |
|                    | defined by -span size. Default join type -norm or -nonorm is set by Java system       |
|                    | property 'gor.varjointype' or 'varjointype' in gor config file.                       |
+--------------------+---------------------------------------------------------------------------------------+

See also :ref:`VARMERGE` and the example for the :ref:`LEFTWHERE` command in relation to this.
                
The right-source can also be specified as a gor-stream, using the <(...) notation as in the :ref:`JOIN` command.


