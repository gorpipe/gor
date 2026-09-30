.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VARNORM_WITH_BUILD:

==================
VARNORM_WITH_BUILD
==================
The :ref:`VARNORM_WITH_BUILD` command is the same as :ref:`VARNORM` except that the variants are normalised against the
reference build given as the first argument instead of the default reference build of the project.  This is useful when
the stream holds variants in a build different from the project default, e.g. after a liftover.

The build is the path to a chromSeq folder (one ``<chrom>.txt`` file per chromosome), quoted or unquoted.  Relative paths
are resolved against the project root.  The query fails if the build does not exist.

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

   gor hg19_variants.gorz | VARNORM_WITH_BUILD config/chromSeqhg19 #3 #4 -left

Related commands
----------------

:ref:`VARNORM` :ref:`VARJOIN` :ref:`VARMERGE`
