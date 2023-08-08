=========
Tutorials
=========

.. toctree::
   :maxdepth: 1
   :hidden:

   basicGORqueries
   filteringStreams
   columnModifications
   groupingAndAggregation
   nestedStreams
   joiningTables
   mapMultimap
   pivoting
   sequenceReads
   variantData
   materializedViews
   parallelGOR
   notebooks

What is GOR?
============
GOR is a :term:`genomic ordered<genomic ordered data>` relational database architecture, which was developed for working with large volumes of DNA sequence data.

The GOR system uses a declarative query language which combines features from SQL and shell pipe syntax. The GOR system can be used, for example, to annotate sequence variants, to find genomic spatial overlap between various types of genomic features, and to filter and aggregate them in various ways.

The GOR architecture is at the foundation of WuXi NextCODE's genome analysis and discovery tools. For an in-depth discussion of the GOR architecture, please refer to:

.. image:: images/paper.png
   :target: https://academic.oup.com/bioinformatics/article/32/20/3081/2196339/GorPipe-a-query-tool-for-working-with-sequence

*GORpipe: a query tool for working with sequence data based on a Genomic Ordered Relational (GOR) architecture*

The target audience of this manual is a :term:`bioinformatician` or other support personnel who will be creating their own GOR queries using either the Sequence Miner or the command line interfaces, GORpipe and GORdb.

This manual will provide an overview of the GOR language and a comprehensive list of commands that are available both through GORdb, GORpipe and the Sequence Miner (SM) with use cases input of an attribute, for example a number, range, table column name, filename or a nested query from the GOR database.  These options will be defined in the manual page for the GOR command.

Writing GOR queries
===================
The best way to learn to write GOR queries is to try it for yourself. Get your hands on some data and an environment from which you can run queries. As we describe each command and concept, we will post example of their usage.

In the example below, we can see the conventions used in each example, which may be familiar to those who have used shell pipe syntax, but may require more explanation for others.

GOR queries have the following structure:

.. code-block:: gor

	gor relation [options/attributes/flags] | command_1 [options/attributes/flags] | command_2 [options/attributes/flags]

As shown above, many commands in GOR can be linked together in a pipeline of commands with each individual command separated by a vertical bar (or "pipe") character. A combination of piped commands allow for specialised operations that individual commands can perform on their own. The rows from the file or table referred in the :ref:`GOR` source command are streamed from left to right.  Depending on the nature of the pipe step commands, they may output their input unchanged, rows with different output columns, fewer rows (e.g. aggregation) or more rows than in the input (e.g. split up of rows).  In every case, commands in a GOR stream must output rows with genomic position (chr,pos) whereas in a NOR stream, this is not necessary.

Whenever possible, we will show example queries for each of the GOR commands that will help to illustrate the usage of the GOR syntax.


Options, Attributes, Flags
--------------------------
Each of the commands in a GOR query may have one or more options and flags that are specific to that command. We will define options and flags for each command on the manual page for each command. An option may in some cases require the input of an attribute, for example a number, range, table column name, filename or a nested query from the GOR database.  These options will be defined in the manual page for the GOR command.


Reference Data
==============
There are a wide variety of source of data that are used and the best way to get familiar with the WuXi NextCODE reference data package is to explore it for yourself. Try viewing existing queries and report builders to see what reference sources are commonly used.

An installation of the Sequence Miner tool will make a set of reference data available to you along with any studies that you have access to. To make the examples in this manual easier to follow, we will be using this reference data in each of the examples. Although the data is stored as a physical GOR file, each of these sets of reference data can be thought of as tables in the GOR database and in this document they are referred to as such.

Reference Data used in example queries are introduced in the table below:

.. list-table:: Reference Data used in Examples
   :widths: 5  10 25
   :header-rows: 1

   * - Alias
     - File Location
     - Description
   * - ``#genes#``
     - ref/genes.gorz
     - Ensembl gene table with only one entry per (Gene_Symbol,Chrom)
   * - ``#exons#``
     - ref/ensgenes/ensgenes_exons.gorz
     - Ensembl exons
   * - ``#VEP#``
     - source/anno/vep_v85/vep_single_wgs.gord
     - Variant Effect Predictor summary annotations (only the max transcript consequence per gene)
   * - ``#dbSNP#``
     - ref/dbsnp/dbsnp.gorz
     - A database of all registered small variants (SNPs and InDel) at NCBI.
   * - ``#wgsVars#``
     - source/var/wgs_varcalls.gord -s PN
     - Whole Genome Sequence Variants - a table with variants from all the samples (PNs) in the project.
   * - ``#wesVars#``
     - source/var/wes_varcalls.gord -s PN
     - Whole Exome Sequence Variants - a subset of variants in #wgsVars# that are overlapping or close to exon boundaries.
