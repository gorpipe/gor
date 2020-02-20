.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GORSQL:

======
GORSQL
======
The GORSQL command allows you to run arbitrary SQL commands against "the database" (the database here being defined by the content of a file called gor.db.credentials in the config directory). This command is not enabled when running against a gor-server and therefore does not work within the Sequence Miner. It is intended to be used with gorpipe only.

For GORSQL to work properly, the defined query must access a table have Chrom-POS information as first columns.

SQL statements need to be encapsulated between curly brackets, e.g. gorsql {sql_query}.

Usage
=====

.. code-block:: gor

   gorsql {<sql_query>} | ...

Examples
========

.. code-block:: gor

   gorsql {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE Phenotype = 'Cancer' order by Chrom,Pos} | ...

