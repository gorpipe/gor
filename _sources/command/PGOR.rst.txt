.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _PGOR:

====
PGOR
====
The :ref:`PGOR` command is essentially a syntactic sugar for a query with CREATE statement.  The PGOR statement *cannot* be used within nested-queries.  In the current implementation, the optional attributes must also appear before the file source or nested query.

When no split attribute is provided, the PGOR command uses the build split configuration file to specify the split partitions.  Typically, such configuration files partition the larger chromosomes into two partitions, separated on the centromeres.  If any of the commands GROUP, GRANNO, RANK, or SORT are present in the query using binsize other than 1 base, only whole chromosomes (contigs) are used, avoiding boundary effect issues arising from partition of query range.

Usage
=====

.. code-block:: gor

   pgor [attributes] source

or

.. code-block:: gor

   create #tempfile# = pgor [attributes] source;
   gor [#tempfile#]

Options
=======

.. tabularcolumns:: |L|J|

+------------------------------+---------------------------------------------------------------------------------+
| ``-split value[:overlap]``   |  If the split value is smaller or equal to 1000, it represents the number of    |
|                              |  partitions into which the genome is split.  If the split value is larger,      |
|                              |  it is the size of each split in sequence bases.                                |
|                              |                                                                                 |
|                              |  The overlap number can be provided optionally to allow genome partitions       |
|                              |  to overlap. In such cases, the variable ##WHERE_SPLIT_WINDOW## can be used     |
|                              |  to set filtering on the output.                                                |
|                              |                                                                                 |
|                              |  Note, when partitions are represented in bases, no more                        |
|                              |  than 100 partitions can be per chromosome.                                     |
+------------------------------+---------------------------------------------------------------------------------+


Examples
--------
A full discussion of PGOR commands and examples of how to use them can be found in the manual chapter on :ref:`Parallelization in GOR<PGORexamples>`.


See also: :doc:`PARTGOR`