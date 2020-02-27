.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _JOIN:

====
JOIN
====
The :ref:`JOIN` command can be used in GOR queries to work with multiple sources (files, tables, or GOR streams) at once based on some overlap condition between the sources. It is possible to define the type of overlap condition and also to structure the output of the query based on some options that are defined in the table below.

Usage
=====

.. code-block:: gor

	gor <left_source> | JOIN <condition> [options] <right_source>

Conditions
==========
A special set of arguments specify the matching conditions for the key columns in the left and right sources (i.e. ``snp``, ``seg`` and ``var``).

+-------------------+-----------------------------------------------------------------------------------------------------------------+
| Type of join      | Description                                                                                                     |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-snpsnp``       | The overlap condition is an exact match between the first two columns (chr,pos) if no fuzz-factor is specified. |
|                   | Otherwise the overlap condition is snpB.pos-fuzzfac <= snpA.pos and snpA.pos <= snpB.pos+fuzzfac                |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-snpseg``       | The overlap condition is seg.start-fuzzfac < snp.pos and snp.pos <= seg.stop+fuzzfac.                           |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-segseg``       | The overlap condition is segA.start-fuzzfac < segB.stop && segA.stop+fuzzfac > segB.start.                      |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-segsnp``       | The overlap condition is seg.start-fuzzfac < snp.pos and snp.pos <= seg.stop+fuzzfac.                           |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-varseg``       | Overlap of (ref,alt) variant with segment.                                                                      |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-segvar``       | Overlap of segment with (ref,alt) variant.                                                                      |
+-------------------+-----------------------------------------------------------------------------------------------------------------+

Options
=======
Further options on joins are specified in the table below.

+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-lstop col``    | The column with the stop position of the segments in the left-source. Defaults to #3.                           |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-rstop col``    | The column with the stop position of the segments in the right-source. Defaults to #3.                          |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-xl cols``      | The columns to perform additional equi-join in the left-source.                                                 |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-xr cols``      | The columns to perform additional equi-join in the right-source                                                 |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-xcis``         | Use case insensitive equi-joins.                                                                                |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-maxseg size``  | The maximum segment size. The default is 3MB for -segseg  and -snpseg join.                                     |
|                   | The default for the ``-maxseg`` value is set to 1 for ``-segsnp``, ``-varsnp``, ``-snpsnp`` joins.              |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-l``            | Left-join style overlap, shows all rows in the left-source.                                                     |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-t``            | To-List style, single output line per left-source row.  Comma separation of multiple overlap values.            |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-c``            | Count, returns a column with the number of overlaps.  Only used with the -t option.                             |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-f fuzzfac``    | A base pair proximity integer value, e.g. 0, 1000 or 10000000.                                                  |
|                   | Note that a large fuzz factor can increase the running time and memory usage.                                   |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-n``            | Return only rows from the left-stream that do NOT overlap with rightfile.gor.                                   |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-i``            | Return only rows from leftfile.gor that are included in the overlap (without the columns from the right-source).|
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-ic``           | Return the rows in left-source and a column named overlap.                                                      |
|                   | Count which shows how many rows in right-source overlap.                                                        |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-ir``           | Return the rows from right-source that overlap with the left-source.                                            |
|                   | Depending on the nature of the left-source and the overlap condition,                                           |
|                   | the join may have to be followed with SORT step to ensure GOR.                                                  |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-m``            | Return only overlaps with the smallest distance (one or many). Only makes sense when used with the -f option.   |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-o n``          | Return only the closest n overlaps row from rightfile per row in the left input source.                         |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-rprefix name`` | A prefix to be added to the columns in the right-source.                                                        |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-r``            | Reduced output.  Do not include distances and position columns from the right source.                           |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-s colname``    | Source column name (for GOR dictionary database file).                                                          |
+-------------------+-----------------------------------------------------------------------------------------------------------------+
| ``-e char``       | Character to denote empty field. Defaults to empty string, i.e. of length 0.                                    |
+-------------------+-----------------------------------------------------------------------------------------------------------------+

For ``-varseg`` and ``-segvar`` joins, there are a few extra options possible:

+-----------+---------------------------------------------------------------------------+
| ``-ref``  | The column denoting the reference seq. (for both left- and right-source). |
+-----------+---------------------------------------------------------------------------+
| ``-refl`` | The column denoting the reference seq. in left-source.                    |
+-----------+---------------------------------------------------------------------------+
| ``-refr`` | The column denoting the reference seq. right-source.                      |
+-----------+---------------------------------------------------------------------------+

Examples
========
The following join command joins a ``snp`` left source (the ``#dbSNP#`` table) to a right source containing segments (``#genes#``).

.. code-block:: gor

   gor #dbSNP# | join -snpseg #genes#

In this next example, the ``#genes#`` table is joined as a nested query, which is necessary if there will be some calculations or other modifications done to the right source. Here, the size of the genes is being calculated and they are then ranked in relation to all other genes on the same chromosome. Furthermore, the ``-maxseg`` is setting the maximum segment size to 1B bases to cover the length of the longest chromosome, since otherwise the result would be incorrect.

.. code-block:: gor

   gor #dbSNP# | join -snpseg -maxseg 1000000000 <(gor #genes# | calc size #3-#2 | rank chrom size -o asc | where rank_size < 100)

.. code-block:: gor

   gor #genes# | where gene_symbol ~ 'B*' | join -segvar -ir #dbSNP# | sort 3000000 | verifyorder

.. code-block:: gor

   gor #genes# | where gene_symbol ~ 'B*' | segspan | join -segvar -ir #dbSNP# | verifyorder

.. code-block:: gor

   gor #genes# | where gene_symbol ~ 'B*' | segspan | join -segvar -ir #wgsvars# -s PN

.. code-block:: gor

   gor #genes# | where gene_symbol ~ 'B*' | segspan | join -segvar -ir <(gor #wgsvars# -s PN -f PN1,PN2)

.. code-block:: gor

   gor #wgsvars# -ff myPNs.tsv | join -snpseg -ic -xl PN -xr PN -maxseg 10000 <(gor #goodcov8# -ff myPNs.tsv)

.. code-block:: gor

   gor #dbsnp# | multimap -cartesian myPNs.tsv | varjoin -l -xl PN -xr PN -e '0' -r <(#wgsvars# -ff myPNs.tsv | select 1-4,FILTER,Callcopies)
   | join -snpseg -ic -xl PN -xr PN <(gor #goodcov8# -ff myPNs.tsv) | replace callcopies if(FILTER != 'PASS','NA',if(callcopies='0' and overlapCount=0,'NA',callcopies)
   | sort 1 -c PN | group 1 -gc 3,4,rsID -lis -sc

Equivalent Queries
------------------
The following queries are equivalent and illustrate how to use different commands to achieve the same result.

.. code-block:: gor

   gor #dbsnp# | join -snpseg -i #genes# == gor #dbsnp# | join -snpseg #genes# | group 1 -gc 3-rsids

   gor #dbsnp# | join -snpseg -ic #genes# == gor #dbsnp# | join -snpseg -l <(gor #genes# | calc overlap 1) | group 1 -gc 3-rsids -sum -ic overlap | rename sum_overlap overlapcount

   gor #dbsnp# | join -snpseg -ir #genes# | sort 3000000 == gor #dbsnp# | join -snpseg  #genes#  | select rsids[+1]- | sort 3000000
