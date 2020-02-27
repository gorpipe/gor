.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _NORSQL:

======
NORSQL
======
The :ref:`NORSQL` command allows you to run arbitrary SQL commands against "the database" (the database here being defined by the content of a file called gor.db.credentials in the config directory). This command is not enabled when running against a gor-server and therefore does not work within the Sequence Miner. It is intended to be used with gorpipe only.

:ref:`NORSQL` can be run against any database table. In the case of :ref:`GORSQL`, the defined query must access a table have Chrom-POS information as first columns.

SQL statemens need to be encapsulated between curly brackets, e.g. norsql {sql_query}.

Usage
=====

.. code-block:: gor

   norsql {<sql_query>} | ...

Examples
========

.. code-block:: gor

   norsql {select * from Case_Controls order by PN} | ...

