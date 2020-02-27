.. _mapMultimap:

================
Map and Multimap
================
When we are joining table data that is not genomic ordered, it is necessary to use the :ref:`MAP` and :ref:`MULTIMAP` commands, which we will now discuss in the following chapter.

Using MAP to Annotate
=====================

Occasionally, we will have annotation data that is not genome-ordered, but is based on some other column data. One example of this would be with the Ensemble annotation data for genes, which can be found in a :term:`mapfile` called ``ensgenes.map`` in the reference data. If we wanted to join the information in this annotation file to the ``#genes#`` table we would need to use the MAP command.

.. note:: A map file in the reference data typically has a ``.map`` file extension, although any tab-separated value file can be used.

As you can see in the following example, we use the map command to join the two tables by setting the column (using the ``-c`` option) that we want to use for the mapping. The column specified must exist in both sources, although the match on column name is not case-sensitive. To get the headers from the mapped table in the output stream, you can use the ``-h`` option.

.. code-block:: gor

   gor #genes# | MAP ref/ensgenes/ensgenes.map -c Gene_Symbol -h

Mapping using Nested Streams
============================
The :term:`mapfile` can also be a nested stream, which is particularly useful if you wish to perform some filtering on the mapfile before mapping. An example of this is shown below:

.. code-block:: gor

   gor #genes# | MAP <(nor -h ref/ensgenes/ensgenes.map | WHERE OMIM_Descriptions != '.')
   -c Gene_Symbol

Note that in the above example, we only get output for the rows where there is a Gene_Symbol found in the table. If we use the ``-m``, as shown in the following example:

.. code-block:: gor

   gor #genes# | MAP <(nor -h ref/ensgenes/ensgenes.map | WHERE OMIM_Descriptions != '.')
   -c Gene_Symbol -h -m ?

then we can get use the ``-m`` option to retrieve output for all the rows in the left source and fill any empty cells with the specified symbol (in this example, a ``?``). In this way, we are guaranteed to have a line in the output for every line in the left source.

If we only wish to get certain columns from the mapped table, then we can specify those columns using the ``-n`` flag followed by a list of the columns we want to let through. This is shown in the example below:

.. code-block:: gor

   gor #genes# | MAP <(nor -h ref/ensgenes/ensgenes.map | WHERE OMIM_Descriptions != '.')
   -c Gene_Symbol -h -n OMIM_Descriptions

Mapping in a NOR context
========================
We can also base our mapping on more than one column. For example, we could use the :ref:`MAP` command to link two separate mapfiles together. Notice here that since neither of files contain genome-ordered data, we must perform this mapping in the nor context:

.. code-block:: gor

   nor ref/ensgenes/ensgenes.map | MAP <(nor -h ref/ensgenes/ensgenes_alias.map
   | GROUP -gc Gene_Symbol,gene_stable_id -count) -h -c Gene_Symbol,gene_stable_id

In the example above, a :ref:`GROUP` command was used to ensure that there was only result per combination of Gene_Symbol and gene_stable_id in the second source file.

Using Multimap
==============
The :ref:`MAP` command assumes that each gene_symbol is only going to be mapped to one output row. However, there are scenarios, such as with the mapping of genes to pathways, where the relation is one-to-many. In these cases, it is better to use the :ref:`MULTIMAP` command.

.. note:: While it is not necessary to use a specific file type with this, the reference data does use the file extension ``.mmap`` to indicate a mapfile with a one-to-many relation.

.. code-block:: gor

   gor #genes# | MULTIMAP ref/ensgenes/ensgenes_gene2pathway.mmap -h -c Gene_Symbol

The main difference with the :ref:`MULTIMAP` command is that each of the rows in the ``#genes#`` table can be mapped to multiple rows based on the Gene_Symbol column.

The options for :ref:`MULTIMAP` are similar to the :ref:`MAP` command.

Using mapfiles to include phenotype data
----------------------------------------
One example of how we may want to use mapfiles is in including phenotype data, which can be found in a report file for the subjects in our studies. In the example below, we are joining the variants from our subject with the ``#VEP#`` table and then mapping this data to the phenotypes found in the phenotypes subject report.

.. code-block:: gor

   gor #wesVars# | VARJOIN #VEP# | TOP 10000 | SELECT 1-Call,Max_Consequence,Max_Impact,PN
   | WHERE Max_Impact IN ('HIGH','MODERATE') | MAP SubjectReports/Phenotypes.rep.link -c PN -h

This can also be accomplished using a grid from another open table in the Sequence Miner.

