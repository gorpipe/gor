.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _ANNO:

====
ANNO
====
The :ref:`ANNO` command left-joins the stream with all the annotation files based on chrom and position. ANNO is a deprecated command that is equivalent to a :ref:`JOIN` with ``-snpsnp``, ``-l`` and ``-t`` options. That method should be used instead.

The ANNO command was created to make annotations based on multiple files with SNP data easy. When the join is not only based on a single nucleotide location it is less useful, e.g. for Ref/Alt based variations.

Usage
=====

.. code-block:: gor

	gor ... | ANNO file1.gor [ ... [ fileN.gor ] ... ]  [ attributes ]

Options
=======

+-----------------+--------------------------------------------------------------------------------------------+
| ``-h``          | Eliminate the header from the output.                                                      |
+-----------------+--------------------------------------------------------------------------------------------+
| ``-e char``     | Character to denote empty field.  Defaults to an empty string, i.e. string of length 0.    |
+-----------------+--------------------------------------------------------------------------------------------+

Examples
========
These two queries are equivalent:

.. code-block:: gor

    gor left.gorz | anno right1.gorz right2.gorz

and the following

.. code-block:: gor

    gor left.gorz | join -snpsnp -l -t right1.gorz | join -snpsnp -l -t right2.gorz