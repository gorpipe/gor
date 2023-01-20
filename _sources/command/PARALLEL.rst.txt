.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _PARALLEL:

========
PARALLEL
========
The :ref:`PARALLEL` command, like :ref:`PGOR`, can be used to run queries in parallel.  Unlike the PGOR command which partitions the query execution along the genomic axis, PARALLEL allows for fully customizable parallelism.

The current implementation, the query expression must be provided like a nested-query expression with the column replacement operator #{col:[column_name]} to inject values from the input parts source into the newly generated gor query as shown in the examples.

Like PGOR, PARALLEL cannot appear in a regular nested query.

Usage
=====

.. code-block:: gor

   create pnlist = nor pns.tsv;
   create #tempfile# = parallel -parts [pnlist]  <(nor #{col:pn_file} | where pn = '#{col:pn}' );
   nor [#tempfile#]

Options
=======

+---------------------+----------------------------------------------------------------------------------------------+
| ``-parts``          | Required option. Input source which contains the split source. Each row in the input parts   |
|                     | source represents the input for a single split. Columns in the parts input source can be     |
|                     | referenced with a #{col:[columnname]} operator.                                              |
+---------------------+----------------------------------------------------------------------------------------------+
| ``-limit [number]`` | Set a maximum number of parallel tasks. Defaults to 1000                                     |
+---------------------+----------------------------------------------------------------------------------------------+


Examples
========
A full discussion of PARALLEL commands and examples of how to use them can be found in the manual chapter on :ref:`Parallelization in GOR<PARALLELexamples>`.


See also: :doc:`PGOR`
