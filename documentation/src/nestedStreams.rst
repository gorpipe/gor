.. _nestedStreams:

==============
Nested Streams
==============
As the GOR framework is multi-threaded, it is possible for us to create efficient queries with many pipe steps and to create declarative and nested streams which are seekable. When a nested stream is included in a GOR query, the nested stream is run before the rest of the query and any data that is generated can be utilised in the rest of the GOR query.


Special Notation
================
There is a special notation used for nested streams. It begins with a left angled bracket and the query in the nested stream is contained within parentheses or "round brackets", as shown in the example below:

.. code-block:: gor

   gor left_stream | ... <(gor right_stream)


.. _mergeTables:

Merging Streams
===============
As stated in the section on the use of :ref:`multiple files<multiple-files>` with the :ref:`GOR` command, we are inherently merging data by specifying any number of files after the :ref:`GOR` command. For example, if we wanted to merge the variation data from two subjects, we might have a command like the following:

.. code-block:: gor

   gor SUBJECT_A.wgs.genotypes.gorz SUBJECT_B.wgs.genotypes.gorz

However, it is not possible to "GOR" together two files that have a different number of columns from each other. In those cases, we may wish to merge the two independent sources as streams using the :ref:`MERGE` command, which works a bit like a "union" clause in SQL.

.. code-block:: gor

   gor #wgsVars# | MERGE #VEP#

Take note though, that when you are merging two streams like this, any position information that you define in the left source will not apply for the right source. Therefore, if you were to write the following query:

.. code-block:: gor

   gor -p chr21 #wgsVars# | MERGE #VEP#

you would get all of the data from ``#VEP#`` across the genome, and then only the data from ``chr21`` from ``#wgsVars#``. To get around this, we could use a nested stream, as follows:

.. code-block:: gor

   gor -p chr21 <(#wgsVars# | MERGE #VEP#)


Further Examples
================
The main advantage of using nested streams in your GOR queries is that the streams are fully seekable, meaning that you can specify commands for the right source in the cases of joins or merges that you would not be able to do without nesting the queries. In the following sections we will discuss some useful examples of this.


Joining to a GOR stream
-----------------------
As with the :ref:`MERGE` command, the right-source in a GOR join can be specified as a nested GOR stream, as shown in the example below:

.. code-block:: gor

    gor left_source.gor | JOIN -snpsnp <(gor right_source.gor)


.. _nested-NOR:

Nested NOR queries
------------------
Nested queries can also be used in both NOR and GOR queries. As the next example shows, we are able to bring the file "genes.gor" into a NOR context and merge it with the NOR stream from the right source, which is a tab-delimited file:

.. code-block:: gor

   nor <(gor -p genes.gor) | MERGE <(nor file2.tsv)