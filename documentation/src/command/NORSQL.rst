.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _NORSQL:

======
NORSQL
======
The :ref:`NORSQL` command allows you to run arbitrary SQL commands against "the database" (the databases available here are the user connections, defined by the content of a file called gor.db.credentials in the config directory).

:ref:`NORSQL` can be run against any database table.

SQL statements need to be encapsulated between curly brackets, e.g. norsql {sql_query}.

Usage
=====

.. code-block:: gor

   norsql {<sql_query>} | ...

Examples
========

.. code-block:: gor

   norsql {select * from Case_Controls order by PN} | ...

.. code-block:: gor

   norsql {select * from Case_Controls WHERE PN = 'ABC' } | ...

.. code-block:: gor

   gorsql -f AAA,TTT {select * from Case_Controls WHERE PN in (#{TAGS}) } | ...

