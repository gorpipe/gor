.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _DAGMAP:

======
DAGMAP
======

The **DAGMAP** command behaves similarly as the :doc:`MULTIMAP` command, however, rather than taking in a regular mapping relation, with input columns on the left and output columns on the right, it uses a parent-child relation representing either a hierarchy or DAG.  The output columns from the DAGMAP are DAG_node and DAG_dist.

Usage
=====

.. code-block:: gor

	gor ... | DAGMAP mapfile.txt -c columns [ Attributes ]

Options
=======

+-----------------+------------------------------------------------------------------------------------------------------+
| ``-c columns``  | The columns on which the lookup is based.                                                            |
+-----------------+------------------------------------------------------------------------------------------------------+
| ``-cis``        | Case-insensitive column lookup.                                                                      |
+-----------------+------------------------------------------------------------------------------------------------------+
| ``-m missing``  | The column output value used when lookup is unsuccessful.                                            |
+-----------------+------------------------------------------------------------------------------------------------------+
| ``-dp``         | Enables the DAG_path column which shows the full relationship path, e.g. index->mother->grandfather. |
+-----------------+------------------------------------------------------------------------------------------------------+
| ``-ps``         | Specifies the path separator. By default **DAGMAP** uses -> as its separator.                        |
+-----------------+------------------------------------------------------------------------------------------------------+
| ``-dl``         | Specify the maximum DAG distance. By default the DAG distance can never go over 20                   |
|                 | but it can be specied to an arbitrary value below 20.                                                |
+-----------------+------------------------------------------------------------------------------------------------------+


Examples
========
This example shows a create statement that forms the parent-child relation (parent_id,hpo_code) from hpo.tsv.  The NOR query then finds all the descendants of the node 'HP:0000001'.

.. code-block:: gor

   create #hpo# = nor ref/disgenes/hpo.tsv  | SELECT hpo_code,parent_ids | SPLIT #2 -s ';'
   | RENAME #2 parent_id | SELECT parent_id,hpo_code;

.. code-block:: gor

   nor [#hpo#] | WHERE parent_id = 'HP:0000001' | SELECT #1 | DISTINCT
   | DAGMAP -c parent_id [#hpo#] -dp | WHERE dag_dist < 3

The query below finds the ancestors of the code 'HP:0003647' and lists them, separated by semicolon as lis_parent_id.

.. code-block:: gor

   nor [#hpo#] | SELECT #1 | DISTINCT | DAGMAP -c parent_id [#hpo#]
   | WHERE DAG_node = 'HP:0003647' | SORT -c dag_dist:nr
   | GROUP -gc dag_node -lis -sc parent_id,dag_dist -s ';'


Related commands
----------------

:ref:`MAP` | :ref:`MULTIMAP`