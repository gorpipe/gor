.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _GRANNO:

======
GRANNO
======
The :ref:`GRANNO` command is a "single-pass" aggregation and annotation. GRANNO adds annotation columns to the output stream for all the rows that fall into the given binsize in addition to those that would normally be added by the GROUP command.

When using **GRANNO** in a gor query, the command takes a binsize parameter, which divides the entire range of values into a series of intervals. The command then annotates the values falling into each interval.

.. note:: Using GRANNO in a nor query omits the ``binsize`` input parameter.

Usage
=====

.. code-block:: gor

	gor ... | GRANNO binsize [ attributes ]

.. code-block:: gor

	nor ... | GRANNO [ attributes ]


Options
=======

+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-count``        | Return the count for each bin.                                                                      |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-cdist``        | Return the number of distinct rows for each bin.                                                    |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-gc cols``      | Grouping columns (other than bin).                                                                  |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-sc cols``      | String columns (-ac has been deprecated).                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-ic cols``      | Integer columns.                                                                                    |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-fc cols``      | Floating valued columns.                                                                            |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-min``          | Calculate the min for any type of column.                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-med``          | Calculate the median for any type of column.                                                        |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-max``          | Calculate the max for any type of column.                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-dis``          | Calculate the number of distinct values for any type of column.                                     |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-set``          | Return a comma separated set with the distinct values in the column.                                |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-lis``          | Return a comma separated list with the values in the column.                                        |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-len number``   | Specify the maximum column length of a set.  Defaults to 200 chars.                                 |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-avg``          | Calculate the avg of all numeric columns.                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-std``          | Calculate the std of all numeric columns.                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-sum``          | Calculate the sum of all numeric columns.                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-steps number`` | The number of sliding steps per group window.                                                       |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-s 'sep'``      | The separator for elements in lists and sets.                                                       |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-range``        |  This interpretes the binsize as the maximum range or span for which the group (as specified        |
|                   |  with the -gc option) extends.  Groups that extend beyond the specified range will not be           |
|                   |  properly aggregated and annotations of row belonging to those group may be incorrect.              |
|                   |  A special binsize value, "gene", can be used to denote 3Mbp in conjunction with the range option.  |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-ordered``      |  Assume the grouping columns are ordered.                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+

When using GRANNO in a NOR context, the ordered flag can both speed up the operation and reduce the memory usage
significantly. Note that there are no checks to see if the order is correct - only use this option if the input
stream is correctly ordered.


Examples
========

.. code-block:: gor

    gor #dbSNP# | JOIN -snpseg #genes#
    | GRANNO chrom -gc genestart,genestop,genename -count

is equivalent to the more verbose

.. code-block:: gor

    gor #dbSNP# | join -snpseg <(gor #dbSNP# | JOIN -snpseg #genes#
    | GROUP chrom -gc genestart,genestop,genename
    | SELECT chrom,genestart,genestop,allCount | SORT chrom)

The range option can be used like this:

.. code-block:: gor

    gor #dbSNP# | JOIN -snpseg #genes# | GRANNO gene -range -gc gene_symbol -count

will add a column (allCount) to each SNP row with, representing the number of rows (SNPs)
that belong to each gene.

See also the :ref:`RANK` command which is of similar nature as the GRANNO (group anno) command

