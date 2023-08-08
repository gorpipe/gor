.. _materializedViews:

==================
Materialized Views
==================

The GOR Query Language has a :ref:`DEF` command to define simple C-like preprocessing macros and aliases and a :ref:`CREATE` command that allows query expressions to be materialized into intermediate tables. These can be useful to build queries or *make queries* in stages and can reduce computation time if they are often referred to, especially when computation to disk-space ratio is high.

In other words, GOR allows you to define virtual tables and macros for result sets that you may wish to reuse when writing queries.

.. Here we need to decide whether to use virtual relation or intermediate table. Ask Hákon about this.

.. _virtualRelations:

Virtual Relations
=================
To create an intermediate table a special notation is often used wrapping the name of the intermediate table like ``##Name##``. However, any alphanumeric string can be used. The whole statement must end in a semi-colon to separate it from the rest of the query. A :ref:`CREATE` statement can be written for any valid gor query. For example, the following:

.. code-block:: gor

   create ##VEP_Relevant## = gor #VEP# | WHERE Max_Impact IN ('HIGH','MODERATE');

will create an intermediate table (called ``##VEP_Relevant##``) of variant effect predictions that only includes high and moderate impact results, which can then be used in a statement such as:

.. code-block:: gor

   gor #wesVars# | JOIN -snpsnp [##VEP_Relevant##]

As you can see above, the name of the intermediate table must be referred to in square brackets containing the name of table.


.. This is referred to as Macros in the paper, but this causes some confusion (input from Heiðdís) with freemarker macros.

.. _definingMacros:

Defining Macros
===============
The :ref:`DEF` command allows you to define macros with any number of input variables, which can be used and reused in gor queries. The general syntax of defining a macros using :ref:`DEF` is shown in the following example where we are creating a macro called ``macro_name`` with two variables.

.. code-block:: gor

   def macro_name($1,$2) = macro_definition;

.. tip:: As with CREATE commands, the DEF command must be terminated with a semi-colon.

The macro shown below which we call "prefixes", takes the two variables (``$1`` and ``$2``) and uses them as prefixes for columns #3 and #4. As we can see in the second line, the macro is then given two values for the variables ``A`` and ``B``, which are then prepended onto the Reference and Call columns of the single row returned from the ``#dbsnp#`` table.

.. code-block:: gor

   def prefixes($1,$2) = PREFIX #3 $1 | PREFIX #4 $2;
   gor #dbsnp# | TOP 1 | prefixes(A,B)

As can be seen in the following sections, you can use virtual relations within your macro to retrieve data sets in an elegant manner.

.. note:: A list of pre-defined functions can be found on the :ref:`chapter on Functions<predefinedFunctions>`.


.. _reusingVirtualRelations:

Reusing Virtual Relations
=========================
Intermediate relations are most useful when referring to a result set multiple times within a query. In the following example, we are only interested in GWAS results of high significance and for a subset of phenotypes, e.g. related to cancer. We can inspect if SNPs associated with cancer are closely spaced in the genome, as in the following example:

.. code-block:: gor

   create ##GWASsubset## = gor snp_gwas_results.gorz | WHERE pval < 1.0e-4 and contains(phenotype,’cancer’);

   gor [##gwassubset##] | WHERE pval < 1.0e-10
   | PREFIX #2- A | JOIN -snpsnp -f 100000 -rprefix B [##gwassubset##]
   | WHERE NOT (A_phenotype = B_phenotype and A_pos = B_pos)
   | RANK 1 B_pval -o asc | WHERE rank_B_pval <= 10

In this example, we form a join beween all the strongly significant cancer associations and other weakly significant associations within a 100kb distance. For each locus from the left-source, we rank the associations from the right-source and return only the top-ten most interesting associations. If we change for instance the allowed overlap distance, the virtual relation [##GWASsubset##] will not have to be re-executed, unless the source file snp_gwas_results.gorz is modified or the create definition changed.


Using Virtual Relations Within Functions
========================================
For the next step, let's say we had already calculated the linkage disequilibrium between every marker in a 10Mbase window and stored this in one very large relation (approx. 50 billion rows) of the form (chrom,pos1,maker1,pos2,marker2,rsquare). If we assume that this relation is represented with the alias ``#LD#``, an LD version of previous example can be written as:

.. code-block:: gor

   def LDjoin($1,$2) = PREFIX #2- A | JOIN -snpsnp #LD# | WHERE A_mrkName = marker1
   | WHERE abs(pos2-A_pos) < $2/2 | SELECT #1,pos2,2- | SORT $2 | JOIN -snpsnp -rprefix B $1
   | WHERE B_mrkName = marker2 | CALC bpDist B_pos-A_pos | HIDE pos2x-marker2
   | SELECT #1,A_*,B_* | SORT $2;

   create ##GWASsubset## = gor snp_gwas_results.gorz | WHERE pval < 1.0e-4 and contains(phenotype,’cancer’);

   gor [##gwassubset##] | WHERE pval < 1.0e-10
   | LDjoin([##gwassubset##],100000)
   | WHERE NOT (A_phenotype = B_phenotype and A_pos = B_pos)
   | RANK 1 B_pval -o asc | WHERE rank_B_pval <= 10

The above **LDjoin** definition, which takes two parameters, can be considered as a parameterised SQL view definition. Notice how the LDjoin uses the :ref:`SELECT` command to move pos2 column into the "GOR position column", because the :ref:`JOIN` command only performs spatial joins on the first columns. Since this operation cannot guarantee genomic order, where there are multiple rows in the left-stream, we have to apply the :ref:`SORT` command. The sort window takes into account the maximum deviation from genomic order. This deviation is governed by the #LD# relation and the filtering "abs(pos2-A_pos)".

Again, the fact that the :ref:`SORT` command has a very efficient sliding window implementation makes it possible to perform the sort without saving data into temporary files and with minimum memory usage.

.. _tableFunctions:

Table Functions
===============
In certain case, for example when testing report builder modules, it may be necessary to access .yml files from the command line using **gorpipe**. In this case you can use the following syntax:

.. code-block:: gor

   gorpipe " gor /<path>/<to>/<report_builder.yml>
   (arg1=val1,arg2=val2,ref_path=/mnt/csa/volumes/ref01/ref/versions/hg19/HG19-85-5-4)"

or for a more concrete example:

.. code-block:: gor

   gorpipe " gor ./nextcode/data_man/queries/report_builders/genes/pathways_to_genes.yml
   (format=line_per_gene,pathways=REACTOME||Interleukin-1_processing,running_time=1,long_running_query=No,
   ref_path=/mnt/csa/volumes/ref01/ref/versions/hg19/HG19-85-5-4) "