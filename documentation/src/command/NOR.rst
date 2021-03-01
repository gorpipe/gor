.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _NOR:

===
NOR
===

The :ref:`NOR` command reads non-ordered relations from a single file or a nested source. File sources are assumed to be
tab-separated (.tsv, .rep, or .map) and with ``#`` as the first symbol in the first line to indicate that it is a header.
Comma-separated files can also be used if they are named .csv. Likewise, .gor and .gorz files can be read, but are treated as unordered relations.

The ``-h`` option can be used to instruct the query to read the first line as a header, but this option is not necessary
if the first line starts with a ``#`` symbol.

It is important to note that NOR queries on very large files can be memory intensive.

Usage
=====
.. code-block:: gor

	nor file.tsv | ...

Options
=======

+-----------------+----------------------------------------------------------------------------------------------------+
| ``-h``          | Instructs the query to read the first line as a header.                                            |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-asdict``     | Tells the query to return the metadata content of the :ref:`dictionary table<dictionaryTables>`.   |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-i``          | Ignore empty lines in the input stream.                                                            |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-f A[,B]*``   | List of tags to filter files and file contents on. Used with .nord and .gord files.                |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-ff File``    | Read tags from a tag file and filter files and file contents on. Also accepts a nested query. Used |
|                 | with .nord and .gord files.                                                                        |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-fs``         | Silent filter, filter tags don't need to be in the dictionary table.                               |
|                 | Only applicable with the -f or -ff options.                                                        |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-s [name]``   | Write source name into output (alias if present in dictionary table, else filename).               |
|                 | Column name will be ``name`` but ``Source`` if no name is specified.                               |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-d``          | Maximum depth when using nor to list the content of a folder. Defaults to infinite                 |
+-----------------+----------------------------------------------------------------------------------------------------+
| ``-m``          | Show modification date when using nor to list the content of a folder.                             |
+-----------------+----------------------------------------------------------------------------------------------------+

Examples
========

In the following example, we are using NOR to open the Variant Effect Predictor (VEP) impact map table. We are setting the -h flag on the NOR query in order to read the first line as a header and the -i option to ignore all empty lines.

.. code-block:: gor

    nor -h -i ref/VEP_impact.map

We often want to open up the contents of a GOR Dictionary table, such as the WES varcalls dictionary, rather than opening the files it references. To do this we use the ``-asdict`` option:

.. code-block:: gor

   nor -asdict source/var/wes_varcalls.gord

Using custom NOR Dictionary table, with filter and custom source column name:

.. code-block:: gor

   nor  custom.nord -f PN-001,PN-005,PN-010 -s PN

Reserved Metacharacters
=======================

Note that some characters are reserved in the nor file and will be automatically replaced if used in the data rows:

+-----------------------------+
|   Reserved Metacharacters   |
+-----------------------------+
|         .\/*+-'$;,          |
+-----------------------------+