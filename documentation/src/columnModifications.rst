.. _columnModifications:

====================
Column Modifications
====================
In this chapter, we will be going through several of the commands in the GOR query language that allow us to add columns to the GOR stream by calculating data and manipulating the columns' titles in various ways to organize the data better.

To show examples of column modifications, we will be taking the example of the ``#dbsnp#`` table shown below:

.. list-table:: #dbsnp# (showing different Indel examples)
   :widths: 5  10 5  5  15
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | TOP 6


As can be seen above, the ``#dbsnp#`` table shows different variants that can occur at various positions on each chromosome. In the example above, we have selected several rows that illustrate five different deletions and a single insertion, which will provide us with a good example for our next topics.


.. _addingCalcColumns:

Calculated Columns
==================
Using the :ref:`CALC` command in a GOR query, we can add columns to the GOR output. For example, we can add a column that shows the length of the reference string using the same **len** function that we used in the :ref:`WHERE` from the above query.

.. list-table:: #dbsnp# (with a calculated column showing the reference length)
   :widths: 5  10 5  5  15 5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - refLength
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
     - 28
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
     - 25
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
     - 22
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
     - 5
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
     - 1
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816
     - 22

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | CALC refLength len(reference) | TOP 6

As you can see above, a *refLength* column has been added to the end of the output with the base length of the reference column.

Replacing Columns with Calculated Columns
=========================================
You may also choose to replace an existing column with some calculated data. The :ref:`REPLACE` command can be used to replace any column in the output (other than the Chrom and POS columns, which must always be present in the GOR stream). It works exactly like the :ref:`CALC` command, but instead of adding a column and specifying the name of the column, you specify the name of the column that you wish to replace.

For example, the following query replaces the rsIDs in the ``#dbsnp`` table with the listsize of the content of the rsIDs cells.

.. code-block:: gor

   gor #dbsnp# | REPLACE rsIDs listsize(rsIDs) | TOP 10

.. note:: Care should be taken when replacing columns with calculated content that the name of the column still makes sense.

Prefix and Rename
=================
The column names and column order can be managed with commands such as :ref:`RENAME`, :ref:`PREFIX` and :ref:`COLUMNSORT`. As an example, in the following gor query, :

.. code-block:: gor

   gor #dbsnp# | CALC splitprefix_cols listsize(rsIDs) | CALC splitprefix_1 listfilter(rsIDs,'i=1')
   | CALC splitprefix_2 listfilter(rsIDs,'i=2') | RENAME splitprefix_(.*) #{1}
   | PREFIX rsIDs[+1]- differentprefix | COLUMNSORT 1-4,different* | top 10

The :ref:`RENAME` command supports regular expression match on columns and the binding variable are referred to as #{n}. In the example above, the rename eliminates ``splitprefix_`` from all columns having the corresponding prefix. The :ref:`PREFIX` command adds a new prefix, "differentprefix", to all the columns to the right of the rsIDs column and, finally, the :ref:`COLUMNSORT` command lists the new output order of columns. Note that since all the columns are in the output, those who are not listed will be placed in alphabetical order. Thus, the :ref:`COLUMNSORT` command makes it easy to emphasize certain columns as the left-most columns in the output.


More about prefixes
-------------------
It can be useful, particularly when working with :ref:`joins<joiningTables>`, to add prefixes to column names to the columns that come from one of the sources to distinguish between analogous columns from two or more tables. However, the :ref:`PREFIX` command can be used to prefix any column name.

Using our :ref:`CALC` example from above, we could prefix the calculated column with a *calc_* as shown below:

.. list-table:: #dbsnp# (with a calculated column showing the reference length)
   :widths: 5  10 5  5  15 5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - calc_refLength
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
     - 28
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
     - 25
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
     - 22
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
     - 5
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
     - 1
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816
     - 22

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | CALC refLength len(reference) | PREFIX refLength calc

Prefixes can also be useful with :ref:`SELECT` commands. We could, for example, prefix the variation data in a GOR stream with "var-" and then refer to all of the variation data columns in a :ref:`SELECT` command with ``var*``.

.. note:: It is not necessary to add the underscore to the prefix that you specify in the query. The underscore is added automatically.


Adding Row Numbers
==================
It is possible to add a column with row numbers to the GOR output by using the :ref:`ROWNUM` command. Rows in the output will be given unique, sequential row numbers, based on the number of rows that have been passed through the query.

The :ref:`ROWNUM` command takes no extra parameters and can be added to a GOR query as the following example shows:

.. list-table:: #dbsnp# (with a calculated column showing the reference length)
   :widths: 5  10 5  5  15 5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - rownum
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
     - 1
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
     - 2
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
     - 3
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
     - 4
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
     - 5
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816
     - 6

.. code-block:: gor

   gor #dbsnp# | WHERE len(reference) > 4 OR len(allele) > 4 | ROWNUM | TOP 6

The row numbers are added to the output as a column with the title ``rownum``.

Take care when using row numbers with GOR queries containing :ref:`nested queries<nestedStreams>` as the nested queries are also given unique row numbers by this command, which may lead to unexpected results.

If we were to add the :ref:`ROWNUM` command before (and after) the :ref:`WHERE` command above, you can see where the filtered data points occur in relation to the whole variant table, which could be useful for data plots. That example is shown below:

.. list-table:: #dbsnp# (with a calculated column showing the reference length)
   :widths: 5  10 5  5  15 5  5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
     - rownum
     - rownumx
   * - chr1
     - 10233
     - CCTAACCCTAACCCTAAACCCTAAACCC
     - C
     - rs200462216
     - 24
     - 1
   * - chr1
     - 10332
     - CCTAACCCTAACCCTAACCCTACCC
     - C
     - rs201106462
     - 58
     - 2
   * - chr1
     - 10621
     - GTTGCAAAGGCGCGCCGCGCCG
     - G
     - rs376342519
     - 104
     - 3
   * - chr1
     - 12940
     - AAACA
     - A
     - rs756849893
     - 146
     - 4
   * - chr1
     - 13421
     - A
     - AGAGA
     - rs777038595
     - 183
     - 5
   * - chr1
     - 15189
     - CGGGCACTGATGAGACAGCGGC
     - C
     - rs768510816
     - 234
     - 6

.. code-block:: gor

   gor #dbsnp# | ROWNUM | WHERE len(reference) > 4 OR len(allele) > 4 | ROWNUM | TOP 6
