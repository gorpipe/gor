.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SORT:

====
SORT
====
The **SORT** command sorts the rows based on the position in cases where the GOR condition has been violated. The SORT command takes one attribute, the binsize, which specifies (in a number of basepairs) the maximum deviation in the order of the stream. If no binsize is provided, or if the provided binsize is illegal, a binsize of 1 is used.

Sorting is often performed as a pipestep after a columnsort, since it is during a columnsort that the genomic order most often will be violated.

The SORT command can be memory intensive. Note that the larger the binsize used, the more memory the sort requires. Using **SORT** in a nor query will give an error if ``binsize`` input parameter is used.

Usage
=====

.. code-block:: gor

	gor ... | SORT binsize [-c cols[:r|:n|:rn]]

.. code-block:: gor

	nor ... | SORT [-c cols[:r|:n|:rn]]

Options
=======

+-------------------+---------------------------------------------------------------+
| ``-c cols:order`` | Additional sorting with r (reverse) and n (number) qualifier. |
+-------------------+---------------------------------------------------------------+

Use ``SORT chrom`` to sort over entire chromosome and ``SORT genome`` to sort over entire genome.

Use ``-c`` option to specify additional sorting, e.g. ``-c #4:n,#3:r,#5-#7:rn``. If you add the ``r`` flag to the additional sorting, the sort will be in reverse. The ``n`` option instructs the command to do a numerical (as opposed to alphabetical) sort.

Examples
========
The following example will take the ``#genes#`` table, group the rows together based on the name of the gene (from the Gene_Symbol column) and then sort alphabetically in descending order by that name.

.. code-block:: gor

    gor #genes# | GROUP genome -gc Gene_Symbol -count | SORT genome -c Gene_Symbol:r
                
In a similar example, the following gor query takes the same result as above, but sorts in reverse numerical order based on the count.

.. code-block:: gor

    gor #genes# | GROUP genome -gc Gene_Symbol -count | SORT genome -c allCount:rn