.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _MAP:

===
MAP
===

The :ref:`MAP` command is used to join tables in GOR and NOR queries based on a one-to-one relation between columns
other than chromosome and position. The column specified must exist in both sources, although the match on column name
is not case-sensitive.

If ``-c`` specifies multiple columns, the map is based on a lookup from the first columns in the map file. If ``-n``
is not specified, all the columns in the map file which are not used for lookup are in the output.

As an example, a map file with (Col1, Col2, Col3, Col4, Col5) and a map command with -c Colx,Coly the output will
include 3 additional column. By default they will be named mVal1, mVal2 and mVal3, but if the ``-n`` option is used
to select a subset of columns from the map file, their name will be as specified in the header. If the ``-m`` option
is not specified, rows which do not map are suppressed, e.g. -m 'missing data' puts the value "missing data" in each
map column where no lookup is found.

If the ``-h`` option is used, the columns will be named after the header in the map file.

The mapfile must be a tab-delimited file, preferably with a header, or a nested NOR query definition,
e.g | map <(nor phenotypes.tsv | where code ~ 'ICD9.*' | select subject,code) -c PN

Usage
=====

.. code-block:: gor

	gor ... | map mapfile.tsv -c cols [ Options ]

Options
=======

+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-c columns``  | The columns (comma separated) on which the lookup is based.                                                   |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-cis``        | Case-insensitive column data lookup.                                                                          |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-e``          | Empty values are not included in the comma separated result list                                              |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-l``          | The mapfile may have multiple values per lookup-value.                                                        |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-m missing``  | The column output value used when lookup is unsuccessful.                                                     |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-n colnames`` | Select which columns in the map file are used in the output (based on the header in the map file).            |
|                 | Override the default value output column names (comma separated and no space).                                |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-not``        | Negate condition, i.e. NOTINSET.                                                                              |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-b``          | For single column inset, add column with true (1) or false (0).                                               |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-h``          | Use the header in the map file to define default output column names (implicitly set when,-n is used).        |
|                 |                                                                                                               |
|                 | The command adds a map value (one or more columns) to the output based on the mapfile                         |
|                 | and the lookup value in the columns specified with the -c option.                                             |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-cartesian``  | Perform a :term:`Cartesian join`.                                                                             |
+-----------------+---------------------------------------------------------------------------------------------------------------+
| ``-ordered``    | Assume the data is ordered for the columns on which the lookup is based. The order must be alphabetical and   |
|                 | ascending.                                                                                                    |
+-----------------+---------------------------------------------------------------------------------------------------------------+

The -ordered flag can reduce the memory usage significantly, especially when the number of lines in the map file is
high.

