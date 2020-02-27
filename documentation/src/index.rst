.. _index:

==============
Welcome to GOR
==============

This documentation covers all aspects of the `GOR open source project`_, which can be used to perform powerful data mining with your genomic data.

.. image:: images/GOR-BLACK.png
  :width: 300
  :class: gor-gorilla
  :align: center
  :alt: The GOR Logo
  :target: #

About GORpipe
=============

The GORpipe analysis tool is developed by WuXi Nextcode Genomics. It originates from the pioneers of population based genomic analysis, deCODE genetics, headquartered in Reykjavik, Iceland.

GORpipe is a tool based on a genomic ordered relational architecture and allows analysis of large sets of genomic and phenotypic tabular data using declarative query language, in a parallel execution engine. It is very efficient in a wide range of use-cases, including genome wide batch analysis, range-queries, genomic table joins of variants and segments, filtering, aggregation etc. The query language combines ideas from SQL and Unix shell pipe syntax, supporting seek-able nested queries, materialized views, and a rich set of commands and functions. For more information see the `paper in Bioinformatics`_.

About this document
===================

This documentation is organized in two separate ways. The chapters can be read from end to end as a walkthrough of the GOR Query Language, with plenty of examples of each.  The walkthrough is organized in such a way as to introduce the more basic concepts first. We will then work our way up to the more complicated commands that are possible with GOR queries.

Alternatively, you can use this manual as a reference. :ref:`Each command in GOR has its own page<quickReference>` with examples and a comprehensive list of functions with a description of their usage can also be found here. This manual assumes that you have some background in using databases and a working knowledge of writing SQL queries is certainly an asset when working with GOR queries.

This documentation covers a range of topics related to the usage of the GOR query language both in the Sequence Miner and on the command line with GorPipe. The :ref:`Quick Reference<quickReference>` contains a comprehensive list of GOR commands with all of the attributes that can be used with each.

.. toctree::
   :maxdepth: 1
   :hidden:

   installation
   tutorials
   ref_commands
   ref_functions

.. _GOR open source project: https://github.com/gorpipe/GOR
.. _paper in Bioinformatics: https://dx.doi.org/10.1093%2Fbioinformatics%2Fbtw199

