.. _joiningTables:

==============
Joining Tables
==============
Joins are a key operation when working in relational databases and this is also the case in the GOR system. For whole genome query analysis, it is often necessary to be able to perform genomically spatial joins on a large number of features quickly. It is implicit when performing joins in the GOR system to do spatial proximity joins according to the genomic position of the data.

There are many different types of joins that can be performed using the GOR language. A full reference for the JOIN command can be found :ref:`here<JOIN>`, but this chapter will go into several of the most useful operations that can be performed with GOR joins.


Join conditions
===============
We must first consider whether we are joining based on single positions (for example, if we are working with a file that has only the chromosome and the position defined, as is the case with the ``#dbsnp#`` table) or based on segments, where the start and stop position are defined in each row (as is the case in a table such as ``#genes#``).

The table below shows a list of the different types of joins that can be performed along with a description of each.

.. list-table:: Types of joins
   :widths: 5  10
   :header-rows: 1

   * - Type
     - Description
   * - ``-snpsnp``
     - Matches single positions in one table to single positions in another table.
   * - ``-snpseg``
     - Matches single positions in one table to segments in another table.
   * - ``-segseg``
     - Matches segments in two tables that overlap.
   * - ``-varseg``
     - Matches the overlap between one table (based on ref,alt) with a segment.
   * - ``-segvar``
     - Matches a segment to a variant (ref,alt) in the right source.

Join options
============
The first type of join (``-snpsnp``) matches single positions in one table to single position in another table. In this most basic example, we could join ``wesVars`` to a :term:`VEP` dictionary (in this case the vep_single_wgs.gord) to annotate the variants in a particular set of samples with :term:`VEP` data.

.. code-block:: gor

	gor source/var/wes_varcalls.gor
   | TOP 10
   | JOIN -snpsnp -xl reference,call -xr reference,call source/anno/vep_v85/vep_single_wgs_gord

Here we have introduced two extra options for having an exact match also on the reference and call columns in the left source (``-xl``) and in the right source (``-xr``). This is also referred to in this manual as an :term:`equi-join`.

.. _negativeJoins:

In the next example, we are using a negative join to return the variants in our samples minus the variants in dbSNP, which would tell us if there are any novel variants in our sample data:

.. code-block:: gor

   gor #wesvars#
   | JOIN -n #dbsnp# -snpsnp -xl reference,call -xr reference,allele

Note that, in this case, the *reference* column from the left must match the *reference* column on the right, but the *call* must match to *allele*.

.. _intersectJoins:

We can perform a basic intersect join by using the ``-i`` option. An intersect join will return only the overlap between two tables. In the following example, we are finding variants overlapping with a gene list (defined here as ``[my_gene_coordinates]``). Since the gene list will be defined in terms of segments, we must use a ``-snpseg`` join condition here.

.. code-block:: gor

   gor #wesVars#
   | JOIN -snpseg -i [my_gene_coordinates]

.. note:: Segment ranges in GOR are zero-based UCSC style, e.g. (start,stop)=(100,200) denotes a genomic segment including bases 101-200, i.e. of length 100bp.

.. _leftJoins:

Left joins
==========
When we talk about *left joins* in GOR, we are talking about joins that are known in the SQL world as *left outer joins*. In the following example, we are joining our sample variants to the frequency table, but we do not want to drop the row that fail to match. In this case, if we encounter rows that do not match, we use the ``-e`` option to fill empty results with "0.0".

.. code-block:: gor

   gor #wesvars#
   | join -l -snpsnp -e 0.0 #freqmax#


Stop positions
==============
When you are joining using any of the options that use segments (``-snpseg``, ``-segseg``, or ``-segsnp``), the stop position for the segments is assumed to be the third column (column #3) in the table. However, this may not be the case for some reason, such as when you are joining to a nested stream and the order of the columns has been changed.

In these cases, it can be useful to use the options ``-lstop`` and ``-rstop`` which both take a single parameter of the column that should be used as the stop position for the segment (in the left and right sources respectively).


.. _fuzzyJoins:

Fuzzy joins
===========
Fuzzy joins are created in GOR queries by using the ``-f`` option on the query followed by the degree (an integer value) of the fuzziness.

.. code-block:: gor

   gor #dbsnp# | TOP 1000 | JOIN -snpseg #exons# -f 10 | SELECT 1-10

.. list-table:: Joining #dbsnp# and #exons#
   :widths: 5  10 5  5  15 5  10 10 5  15
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - distance
     - chromstart
     - chromend
     - gene_symbol
     - exon
   * - chr16
     - 89978525
     - C
     - T
     - rs908913173
     - 2
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523
   * - chr16
     - 89978529
     - A
     - T
     - rs187669455
     - 0
     - 89978526
     - 89979053
     - MC1R
     - ENSE00002231523


In the example shown above, we have set the join to be fuzzy with a degree of 10, which allows us to widen our result set.

Notice that all of the rows from the original ``#dbsnp#`` table are included this time, but in the case of the first row, the distance is now equal to 2, which is how far outside of the exact match that row is.

Verifying genomic order
========================
If we are joining multiple tables and rearranging the columns in our result set, it can sometimes occur that the genomic order is violated in the GOR output. In these cases, the :ref:`VERIFYORDER` command can be used to check the genomic order of the GOR output. If the command is added to the end of a GOR query, it will throw an exception if the genomic order of the GOR stream is violated.

If there is a problem with the genomic order of a GOR stream, it can be useful to use the :ref:`SORT` command to correct the violated order.


.. _joins-NOR:

Joins in NOR
------------
As we discussed at the beginning of this manual, when we are working with files in a :ref:`NOR` context, we cannot rely on genomic-ordered data to join tables. Joins in NOR queries are performed using the :ref:`MAP` and :ref:`MULTIMAP` commands, which are discussed in the following chapter on :ref:`Map and Multimap<mapMultimap>`.