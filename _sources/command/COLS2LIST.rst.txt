.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLS2LIST:

=========
COLS2LIST
=========

The **COLS2LIST** command collapses columns into a single column, with a separator between the values.
The separator defaults to ",", but can be set to anything.

The column values can be mapped with an expression if some simple transformation is needed
on each value. The expression assumes all columns are strings. The current value being processed
is referred to as 'x'.

Usage
=====

.. code-block:: gor

    gor ... | COLS2LIST columnsToCollapse outputColumn -sep ':' -gc groupingColumns -map expr(x)

Options
=======

+---------------------+------------------------------------------------------------------------------+
| ``-sep separator``  | Separator between values                                                     |
+---------------------+------------------------------------------------------------------------------+
| ``-gc columns``     | Grouping columns - columns to keep unchanged                                 |
+---------------------+------------------------------------------------------------------------------+
| ``-map expr(x)``    | Mapping expression, referring to the column being added as 'x'               |
+---------------------+------------------------------------------------------------------------------+

Examples
========
The following example will collapse the columns in the column range to a single column
named val3-12, with the values separated by commas:

.. code-block:: gor

    gor file.gor | cols2list col00003-col00012 val3-12

If there are grouping columns in the file, i.e. columns that should be kept unchanged
you can include them like this:

.. code-block:: gor

    gor file.gor | cols2list col00003-col00012 val3-12 -gc col00001,col00002

Simple mapping can also be applied to each value as it is gathered to the list. For example,
to put each value in lower case:

.. code-block:: gor

    gor file.gor | cols2list col00003-col00012 val3-12 -gc col00001,col00002 -map lower(x)

The list of columns to collapse can also come from a file:

.. code-block:: gor

    gor file.gor | cols2list columnslist.tsv collapsed

Or a nested query:

.. code-block:: gor

    gor file.gor | cols2list <(nor columnslist.tsv | top 100) collapsed

Referring to other columns can be used for extracting information from the field using a format tag in vcf:

.. code-block:: gor

    gor file.vcf | cols2list 10- values -map vcfformattag(format,x,'GT')
