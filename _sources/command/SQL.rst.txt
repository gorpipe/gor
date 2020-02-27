.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SQL:

===
SQL
===
The SQL command allows you to run arbitrary SQL commands against "the database" (the database here being defined by the content of a file called gor.db.credentials in the config directory). This command is not enabled when running against a gor-server and therefore does not work within the Sequence Miner. It is intended to be used with gorpipe only.

SQL statements need to be encapsulated between curly brackets, e.g. sql {sql_query}.

Usage
=====

.. code-block:: gor

   gor ... | SQL {sql_query}

Examples
========

.. code-block:: gor

   gor ... | SQL {select * from sometable}