.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GORSQL:

======
GORSQL
======
The :ref:`GORSQL` command allows you to run arbitrary SQL commands against "the database" (the database here being defined by the content of a file called gor.sql.credentials in the config directory).

For :ref:`GORSQL` to work properly, the defined query must return Chrom-POS information as first two columns.

SQL statements need to be encapsulated between curly brackets, e.g. gorsql {sql_query}.

Usage
=====

.. code-block:: gor

   gorsql <options> {<sql_query>} | ...

Options
=======

+----------------------+---------------------------------------------------------------------------------------------------+
| ``-p chr:pos1-pos2`` | Choose a specific chromosome and position or a range to read. Both pos1 and pos2 are included.    |
|                      | The following place holders are provided: #{CHROM}, #{BPSTART}, #{BPSTOP}.                        |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-f A[,B]*``        | List of tags to filter files and file contents on.                                                |
|                      | The following place holders are provided: #{TAGS}.   The necessary quoting is done automatically. |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-ff File``         | Read tags from a tag file and filter files and file contents on. Also accepts a nested query.     |
|                      | The following place holders are provided: #{TAGS}.   The necessary quoting is done automatically. |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-db database``     | Database alias as defied in ``gor.sql.credentials``.                                              |
+----------------------+---------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

   gorsql {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE Phenotype = 'Cancer' order by Chrom,Pos} | ...

.. code-block:: gor

   gorsql -p chr3:1000- {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE chrom = #{CHROM} and POS >= #{BPSTART} order by Chrom,Pos} | ...

.. code-block:: gor

   gorsql -f AAA,TTT {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE Call in (#{TAGS}) order by Chrom,Pos} | ...