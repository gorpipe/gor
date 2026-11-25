.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VARNORM_WITH_BUILD:

==================
VARNORM_WITH_BUILD
==================
The :ref:`VARNORM_WITH_BUILD` command is the same as :ref:`VARNORM` except that the variants are normalised against the
reference build given as the first argument instead of the default reference build of the project.  This is useful when
the stream holds variants in a build different from the project default, e.g. after a liftover.

It takes the same options as :ref:`VARNORM`: if neither ``-left`` nor ``-right`` is specified, then left normalisation is
default, and ``-trim`` deletes the redundant bases in the representations of insertions or deletions (i.e. deletions are
represented by empty cells in Call - or in Reference in the case of insertions).

The build is the path to a chromSeq folder (one ``<chrom>.txt`` file per chromosome), quoted or unquoted, either local or
in an object store (e.g. ``s3://bucket/ref/hg38/chromSeq``).  Relative paths are resolved against the project root.  The
query fails if the build holds neither ``chr1.txt`` nor ``1.txt``.

.. note::
   The chromosome files are looked up by the chromosome names in the stream, so the naming of the build must match the
   stream.  A ``chr1`` stream against a build with ``1.txt`` files (or the other way around) finds no reference, reads it
   as ``N`` and leaves the variants unnormalised, with only a warning in the log.

Usage
=====

.. code-block:: gor

   gor ... | VARNORM_WITH_BUILD build refcol altcol [ -seg | -left | -right | -trim | -span ]

Options
=======

+--------------+------------------------------------------------------------------------------------+
| ``-seg``     | The variant is denoted as segment, e.g. (chr,bpstart,bpstop,ref,call).             |
+--------------+------------------------------------------------------------------------------------+
| ``-left``    | Normalise the variation data to the left.                                          |
+--------------+------------------------------------------------------------------------------------+
| ``-right``   | Normalise the variation data to the right.                                         |
+--------------+------------------------------------------------------------------------------------+
| ``-trim``    | Trims the redundant bases away from the defined columns (ref + alt)                |
+--------------+------------------------------------------------------------------------------------+
| ``-span``    | Max merge span. The default is 1000bp, max 1Mb (crazy high!)                       |
+--------------+------------------------------------------------------------------------------------+

Examples
========
.. code-block:: gor

   gor hg38_variants.gorz | VARNORM_WITH_BUILD ref/hg38/chromSeq #3 #4

.. code-block:: gor

   gor lifted_variants.gorz | VARNORM_WITH_BUILD 's3://bucket/ref/hg38/chromSeq' Reference Call -right -trim

Related commands
----------------

:ref:`VARNORM` :ref:`VARJOIN` :ref:`VARMERGE`
