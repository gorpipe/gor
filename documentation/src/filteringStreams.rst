=================
Filtering Streams
=================

Once you have defined a source table for your data, you can start to structure the way you want to have the data processed and filtered. You can define commands for data processing and filtering in any number of pipe steps. Note that data flows through the pipes from left to right so that, most often, the order of the steps matters.

TOP and SKIP
============
There are several commands that filter streams in the GOR Query Language. The most basic are the **TOP** and **SKIP** commands, which are used to limit the number and section of rows in the stream.

.. code-block:: gor

   gor #dbsnp# | top 100

The above example will only show the first 100 rows of the ``##dbsnp##`` table and terminate the reading of the data.

.. code-block:: gor

   gor -p chr2 #dbsnp# | skip 100 | top 100

will likewise only show rows from 101 to 200 on chr2 from the ``##dbsnp##`` table.

WHERE
=====
The main function for filtering streams is the **WHERE** command. We will now go over some of the different ways that a WHERE command can be used.

Matching Patterns
-----------------
To start with a simple example of a **WHERE** command, we can consider the gene table, which is included in the reference data for the Sequence Miner. If you were to run the following GOR query:

.. code-block:: gor

   gor #genes# | WHERE Gene_Symbol IN ('BRCA1', 'BRCA2')

you would expect the following output from the query showing only the two rows:

.. code-block:: bash

   Chrom    gene_start     gene_end    Gene_Symbol
   chr13    32889610       32973805    BRCA1
   chr17    41196311       41277500    BRCA2

Multiple match conditions can be linked together in a single **WHERE** command such in the following case:

.. code-block:: gor

   gor #dbsnp# | WHERE reference+’>’+allele IN (’A>G’,’G>A’,’C>T’,’T>C’) OR (len(#3)=len(#4) AND (len(#3)=2 OR len(#3)=3))

As shown above, ``AND`` and ``OR`` statements can be nested in curved brackets to include multiple match conditions.

.. note:: Clicking the ``TAB`` key while writing queries in the Sequence Miner brings up a context-sensitive dialog that allows you to select columns that can be added to the query.

Using Wildcard Notation
-----------------------
It is also possible to use wildcard notation in **WHERE** clauses. The previous example with the ``##genes##`` table could be accomplished as follows by using a wildcard:

.. code-block:: gor

   gor #genes# | WHERE Gene_Symbol LIKE 'BRCA*'

Another example is shown with wildcards below, this time using **LIKE** and **RLIKE** which accepts regular expressions:

.. code-block:: gor

   gor #dbsnp# | top 1000 | WHERE rsIDs ~ 'rs22*' and NOT( rsIDs LIKE 'rs?23*' or rsIDs RLIKE 'rs(3|4)*2.*')

.. note:: Using ``~`` is equivalent to using **LIKE**. ``?`` can be used for any character.

Base-pair Notation
------------------
There is a special syntax for base-pair positions, i.e. integers can be represented with commas every 3 digits.
They must however be followed by the letters bp, e.g. not( pos > 123,345,789bp ).

Using Functions in WHERE commmands
==================================
It is possible to use Functions in **WHERE** commands and also in **CALC** commands, which are discussed in :ref:`a later chapter<addingCalcColumns>`.

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


In the above example, we are using the **LEN** function, which calculates the length of a string, to filter out any results from the ``#dbsnp#`` table that do not have reference or allele base lengths longer than 4 bases. You can find more information on different formulas in GOR queries :ref:`here<functions>`. More information on the **CALC** command can be found :ref:`here<addingCalcColumns>`


Using SELECT to choose columns
==============================

The :ref:`SELECT` command allows you to choose which columns to show from the source by entering a comma-delimited list.

When selecting in GOR, genomic ordered relational data is expected, so it is mandatory to have a ``CHROM`` and ``POS`` as the first two columns. However, when the SELECT statement is used in NOR, you can choose whatever columns you want.  If columns other than the first two (e.g. ``#1`` and ``#2``) are selected to become the first two columns, care must be taken to ensure that proper genomic order is preserved (see discussion on the ``-ir`` option in the :ref:`JOIN` command).


Showing and Hiding Columns
==========================
In the next example, let's take a table that has many different columns such as the whole exon sequence variants from the subject project, which can be loaded using the alias ``#wesVars#``:

.. list-table:: #wesVars#
   :widths: 5  5  5  5  5  5  5  5  15  5  15  5
   :header-rows: 1

   * - CHROM
     - POS
     - Reference
     - Call
     - CallCopies
     - CallRatio
     - Depth
     - GL_Call
     - FILTER
     - FS
     - FormatZip
     - PN
   * - chr1
     - 13273
     - G
     - C
     - 1
     - 0.2
     - 85
     - 318
     - VQSRTrancheSNP99.90to100.00
     - 0.0
     - Alt=C:GT=0/1,AD=68,17,DP=85,GQ=99,PL=318,0,826
     - C416TO_ANDERSON_CHILD_2005_M
   * - chr1
     - 13273
     - G
     - C
     - 1
     - 0.239
     - 88
     - 408
     - VQSRTrancheSNP99.90to100.00
     - 0.0
     - Alt=C:GT=0/1,AD=67,21,DP=88,GQ=99,PL=408,0,723
     - C416TO_ANDERSON_FATHER_1967_M
   * - chr1
     - 13273
     - G
     - C
     - 1
     - 0.206
     - 68
     - 231
     - VQSRTrancheSNP99.90to100.00
     - 12.71
     - Alt=C:GT=0/1,AD=54,14,DP=68,GQ=99,PL=231,0,687
     - C416TO_ANDERSON_CHILD_2000_F
   * - chr1
     - 13302
     - C
     - T
     - 1
     - 0.614
     - 44
     - 50
     - LowQual
     - 0.0
     - Alt=T:GT=0/1,AD=17,27,DP=44,GQ=50,PL=50,0,239
     - C416TO_FATHER

Now let's say that we only want to retrieve the reference and call columns along with quality filter (FILTER column) and the unique patient identifier. To retrieve only those columns, we could execute the following GOR query on the table:

.. code-block:: gor

	gor #wesVars# | SELECT 1,2,Reference,Call,FILTER,PN | TOP 4

.. note:: When writing comma-separated lists, take care not to have spaces between the items in a list.

This would result in the following result set being returned:

.. list-table:: #wesVars# with 6 columns selected
   :widths: 5  5  5  5  5  5
   :header-rows: 1

   * - CHROM
     - POS
     - Reference
     - Call
     - FILTER
     - PN
   * - chr1
     - 13273
     - G
     - C
     - VQSRTrancheSNP99.90to100.00
     - C416TO_ANDERSON_CHILD_2005_M
   * - chr1
     - 13273
     - G
     - C
     - VQSRTrancheSNP99.90to100.00
     - C416TO_ANDERSON_FATHER_1967_M
   * - chr1
     - 13273
     - G
     - C
     - VQSRTrancheSNP99.90to100.00
     - C416TO_ANDERSON_CHILD_2000_F
   * - chr1
     - 13302
     - C
     - T
     - LowQual
     - C416TO_FATHER

We could accomplish the same result if we were to use the :ref:`HIDE` command to hide the columns we do not wish to see.

.. code-block:: gor

	gor #wesVars# | HIDE 5-8,FS,#11 | TOP 4

In the example above, we are defining a range of columns (i.e. 5 through to 8); the FS column is referred to by its name; column #11 is referred to by its position in the column sequence.
Commands allow reference to columns based on their number and their names, however, numbers must be prefixed by ``#`` must be used in **WHERE** and **CALC** to distinguish
them from numbers in expressions. If the listed columns do not exist, the query will throw an exception.

Note that commands and column names are case insensitive.

See also: :doc:`command/HIDE` and :doc:`command/TRYSELECT`

