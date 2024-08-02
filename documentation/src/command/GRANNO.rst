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

Column selection options for annotation
=======================================

Columns selected with these options will be the inputs to annotations.

+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-sc cols``      | String columns (-ac has been deprecated).                                                           |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-ic cols``      | Integer columns.                                                                                    |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-fc cols``      | Floating valued columns.                                                                            |
+-------------------+-----------------------------------------------------------------------------------------------------+

Annotation options
==================

Each of the options below causes a column to be added for each applicable input column
(selected using ``-sc``, ``-ic``, and/or ``-fc`` as listed above) whose values will be the
result computed across the group.
The result column name is generated as the option name joined with the input name, separated with ``_``.

+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-min``          | Calculate minimum within group                                                                      |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-med``          | Calculate median within group                                                                       |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-max``          | Calculate maximum within group                                                                      |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-dis``          | Calculate the number of distinct values                                                             |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-set``          | Return a comma separated set with the distinct values in the column.                                |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-lis``          | Return a comma separated list with the values in the column.                                        |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-avg``          | Calculate average (numeric columns only)                                                            |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-std``          | Calculate standard deviation (numeric columns only)                                                 |
+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-sum``          | Calculate sum (numeric columns only)                                                                |
+-------------------+-----------------------------------------------------------------------------------------------------+

Annotation modifier options
===========================

These options adjust how annotation results are produced.

+-------------------+-----------------------------------------------------------------------------------------------------+
| ``-len number``   | Specify the maximum column length of a set.  Defaults to 200 chars.                                 |
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

Other annotations are illustrated in this contrived example, where ``##roster##``
is a table that includes columns Name, Team, and Age:

.. code-block:: gor

    nor [##roster##] | granno -gc team -ic age -sc name -avg -dis

That adds columns ``dis_Name`` (the number of distinct names on each team);
``dis_Age`` (the number of distinct ages, by team), and ``avg_Age`` (the average age, by team).

See also the :ref:`RANK` command which is of similar nature as the GRANNO (group anno) command

