.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SQL:

===
SQL
===
The SQL command allows you to run arbitrary SQL commands against "the database" (the database here being defined by the content of a file called gor.sql.credentials in the config directory).

SQL statements need to be encapsulated between curly brackets, e.g. sql {sql_query}.

See also :ref:`GORSQL` and :ref:`NORSQL`

Usage
=====

.. code-block:: gor

    SQL <options> {sql_query} | ...

Options
=======

+----------------------+---------------------------------------------------------------------------------------------------+
| ``-n``               | If used we run in NOR context, otherwise we are on GOR context.   If this option is not set then  |
|                      | the defined query must return Chrom-POS information as first two columns.                         |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-p chr:pos1-pos2`` | Choose a specific chromosome and position or a range to read. Both pos1 and pos2 are included.    |
|                      | The following place holders are provided: #{CHROM}, #{BPSTART}, #{BPSTOP}.                        |
|                      | Only applicable if in GOR context (-n not set).                                                   |
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

   sql {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE Phenotype = 'Cancer' order by Chrom,Pos} | ...

.. code-block:: gor

   sql -p chr3:1000- {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE chrom = #{CHROM} and POS >= #{BPSTART} order by Chrom,Pos} | ...

.. code-block:: gor

   sql -f AAA,TTT {select Chrom, Pos, Reference, Call from GWAS_Subset WHERE Call in (#{TAGS}) order by Chrom,Pos} | ...

.. code-block:: gor

   sql -n {select * from Case_Controls order by PN} | ...

.. code-block:: gor

   sql -n {select * from Case_Controls WHERE PN = 'ABC' } | ...

.. code-block:: gor

   sql -n -f AAA,TTT {select * from Case_Controls WHERE PN in (#{TAGS}) } | ...