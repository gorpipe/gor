.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _BUCKETSPLIT:

===========
BUCKETSPLIT
===========

The **BUCKETSPLIT** command splits the content of a column into multiple rows and adds a bucket column.  Other columns will be unchanged.

Usage
=====

.. code-block:: gor

	gor ... | bucketsplit splitCol bucketSize (-s sepVal | -vs valueSize) [-b bucketPrefix] [-cs]


Notes:
1. Empty value lines will not return any results.

Options
=======

+---------------------+----------------------------------------------------------------------------------------+
| ``-s sepVal``       | Split separator character.  If separator is used the output will also use it.          |
|                     | By default it is the empty string ''.                                                  |
+---------------------+----------------------------------------------------------------------------------------+
| ``-vs size``        | Use a fixed character size for values, e.g. rather than variable length separated      |
|                     | with a separator.|                                                                     |
|                     |                                                                                        |
|                     | If colLength != x * size, where x is an integer an error is thrown.                    |
|                     | If size > length(splitCol > 0 then an error is thrown.                                 |
+---------------------+----------------------------------------------------------------------------------------+
| ``-b bucketPrefix`` | Prefix to use for the bucket names.  Defaults to 'b/_'.                                |
+---------------------+----------------------------------------------------------------------------------------+
| ``-cs``             | If set then validation checks are skipped. These include checks for that all lines     |
|                     | should have same number of values in splitCol and checks to disallow empty splitCol    |
|                     | lines.                                                                                 |
+---------------------+----------------------------------------------------------------------------------------+

Examples
========

The following example will take the values column in the specified bgen file and split each line into bucket lines, each with bucket of size 10000.  Each value is 4 characters long so each bucket will contain 40000 characters.

.. code-block:: gor

	gor file.bgen | BUCKETSPLIT values 10000 -vs 4

The following example will take the values column in the specified gor file and split each line into bucket lines, each with bucket of size 300.  Each bucket line will contain 300 values but the character size will depend on the size of each value.

.. code-block:: gor

	gor file.gor | BUCKETSPLIT values 300 -s ':' -b 'bucket_'

