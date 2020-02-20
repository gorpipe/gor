.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GOR:

===
GOR
===
The :ref:`GOR` command loads a source with genomic ordered relational data with some optional parameters.

For users of command line interfaces in Linux or Unix systems, the GOR command can be thought of as being similar to the ``cat`` commands.
It basically outputs data from one or more file or a table.  Unlike the ``cat`` command that concatenates multiple files together,
the :ref:`GOR` command :ref:`merges the data<multiple-files>` from multiple sources into a genomic ordered stream.

All GOR queries begin with the GOR command followed by the source file (or files) that you will be reading from.
A valid GOR source has chromosome and position in the first two columns, the chromosome being lexicographically ordered and the position numerically ordered within each chromosome.

A ``GOR`` file is essentially a tab-delimited file (``TSV``) with a header and genomic ordered rows.  Other examples of
genomic ordered files are block-compressed ``GORZ`` files, ``BAM`` and ``Tabix`` files.  Note however, that in most cases it
is meaningless to compare or work with GOR sources with data from different genomic builds, unless the :ref:`LIFTOVER` command is being used.

Usage
=====

.. code-block:: gor

	gor -p -f -ff -fs -nf -s -w -ref -idx file1 file2 file3 ... fileN

Options
=======

+----------------------+---------------------------------------------------------------------------------------------------+
| ``-p chr:pos1-pos2`` | Choose a specific chromosome and position or a range to read. Both pos1 and pos2 are included.    |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-f A[,B]*``        | List of tags to filter files and file contents on.                                                |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-ff File``         | Read tags from a tag file and filter files and file contents on. Also accepts a nested query.     |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-fs``              | Silent filter, filter tags don't need to be in the dictionary table.                              |
|                      | Only applicable with the -f or -ff options.                                                       |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-nf``              | No row filter.  Filter tags are only used to select files from dictionaries tables,               |
|                      | but not for row filtering.  Only applicable with the -f or -ff options                            |
|                      | when those are used on dictionaries tables.                                                       |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-s [name]``        | Write source name into output (alias if present in dictionary table, else filename).              |
|                      | Column name will be ``name`` but ``Source`` if no name is specified.                              |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-idx File``        | Path to an index file associated with the input source. Normally index files are automatically    |
|                      | loaded from the input source path but a custom index file can be supplied with this option.       |
+----------------------+---------------------------------------------------------------------------------------------------+
| ``-ref File``        | Path to a reference file or directory associated with the input source. Supplying a directory     |
|                      | assumes the reference to be chromSeq reference. Supplying no reference will try to use the current|
|                      | reference path defined in te active project.                                                      |
+----------------------+---------------------------------------------------------------------------------------------------+
