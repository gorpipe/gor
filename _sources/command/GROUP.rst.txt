.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _GROUP:

=====
GROUP
=====
The :ref:`GROUP` command allows you to group the output of your query according to a number of different criteria. The **GROUP** command is very similar to the "group by" function in SQL.

This command groups a set of rows into a set of summary row by values of columns. It returns one row for each group. We often use the GROUP command with aggregate functions such as ``-count``, ``-cdist``, ``-min``, ``-med``, ``-max``, ``-dis``, ``-set``, ``-lis``, ``-avg``, ``-std``, and ``-sum``.

The aggregate functions allow us to perform a calculation of a set of rows while grouping.

Using **GROUP** in a nor query will give an error if ``binsize`` input parameter is used.

Usage
=====

.. code-block:: gor

	gor ... | GROUP binsize [ attributes ]

.. code-block:: gor

	nor ... | GROUP [ attributes ]

Options
=======

+-------------------+----------------------------------------------------------------------+
| ``-count``        | Return the count for each bin.                                       |
+-------------------+----------------------------------------------------------------------+
| ``-cdist``        | Return the number of distinct rows for each bin.                     |
+-------------------+----------------------------------------------------------------------+
| ``-gc cols``      | Grouping columns (other than bin).                                   |
+-------------------+----------------------------------------------------------------------+
| ``-sc cols``      | String columns (-ac has been deprecated).                            |
+-------------------+----------------------------------------------------------------------+
| ``-ic cols``      | Integer columns.                                                     |
+-------------------+----------------------------------------------------------------------+
| ``-fc cols``      | Floating valued columns.                                             |
+-------------------+----------------------------------------------------------------------+
| ``-min``          | Calculate the min for any type of column.                            |
+-------------------+----------------------------------------------------------------------+
| ``-med``          | Calculate the median for any type of column.                         |
+-------------------+----------------------------------------------------------------------+
| ``-max``          | Calculate the max for any type of column.                            |
+-------------------+----------------------------------------------------------------------+
| ``-dis``          | Calculate the number of distinct values for any type of column.      |
+-------------------+----------------------------------------------------------------------+
| ``-set``          | Return a comma separated set with the distinct values in the column. |
+-------------------+----------------------------------------------------------------------+
| ``-lis``          | Return a comma separated list with the values in the column.         |
+-------------------+----------------------------------------------------------------------+
| ``-len number``   | Specify the maximum column length of a set and a list.               |
|                   | Defaults to 10000 chars.                                             |
+-------------------+----------------------------------------------------------------------+
| ``-avg``          | Calculate the avg of all numeric columns.                            |
+-------------------+----------------------------------------------------------------------+
| ``-std``          | Calculate the std of all numeric columns.                            |
+-------------------+----------------------------------------------------------------------+
| ``-sum``          | Calculate the sum of all numeric columns.                            |
+-------------------+----------------------------------------------------------------------+
| ``-steps number`` | The number of sliding steps per group window.                        |
+-------------------+----------------------------------------------------------------------+
| ``-s 'sep'``      | The separator for elements in lists and sets.                        |
+-------------------+----------------------------------------------------------------------+
| ``-ordered``      | Assume the grouping columns are ordered.                             |
+-------------------+----------------------------------------------------------------------+

Attributes ``-ic`` and ``-fc`` explicitly define columns of either integer type or floating point type, while the ``-sc``
attribute defines it as a string type. Any column defined with the ``-gc`` option and is a part of the following aggregation
options ``-sc``, ``-ic`` or ``-fc`` will result in an error

+-------------------+----------------------------------------------------------------------+
| ``-sc``           | Works with -max, -min and -med.                                      |
+-------------------+----------------------------------------------------------------------+
| ``-ic``           | Works with -max, -min, -med, -avg, -std and -sum.                    |
+-------------------+----------------------------------------------------------------------+
| ``-fc``           | Works with -max, -min, -med, -avg, -std and -sum.                    |
+-------------------+----------------------------------------------------------------------+

Ensure that there are no spaces between the numbers. Use SELECT to pick a subset of columns from the output.

Use ``binsize = chrom`` to aggregate for a whole chromosome and ``binsize = genome`` to aggregate for the entire genome.

When using GROUP in a NOR context, the ordered flag can both speed up the operation and reduce the memory usage
significantly. Note that there are no checks to see if the order is correct - only use this option if the input
stream is correctly ordered.

Examples
--------
Group columns a and b on each chromosome and counting the number of groups.

.. code-block:: gor

	gor ... | group chrom -gc a,b -count

Group column a for the whole genome and calculate the sum and average for columns b and c (which are defined as floating point columns).

.. code-block:: gor

	gor ... | group genome -gc a -fc b,c -ave -sum

Group ``#dnsnp#`` on reference for the whole genome and return it as a set with -set attribute. This query should return a set
of ``A,C,G,T`` (in undefined order)

.. code-block:: gor

    gor #dbsnp# | where len(reference) = 1 | group genome -sc reference -set

In some cases, we may wish to know which individual rows have been grouped together. In that case, the ``-lis`` argument comes in handy:

.. code-block:: gor

    gor source/var/wgs_varcalls.gord -s PN
        | group 1 -gc reference,call -lis -sc PN
        | top 100

The query above will output a list on each variant showing which PN carried that variant.