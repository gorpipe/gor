.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: nor</span>

.. _TSVAPPEND:

=====
TSVAPPEND
=====
The :ref:`TSVAPPEND` command can be used to append a stream to a file.

Usage
=====

.. code-block:: gor

	nor ... | tsvappend <file name> [-noheader] [-prefix <prefix>] [-link <link path>]

Options
=======

+-------------------+-----------------------------------------------------------------+
| ``-prefix <hf>``  | Takes in a text source containing prefix to be prepended to the |
|                   | file written. Also support string in single quotes              |
+-------------------+-----------------------------------------------------------------+
| ``-noheader``     | Don't write a header lines.  Not valid with gor/gorz/nor/norz.  |
+-------------------+-----------------------------------------------------------------+
| ``-link[v{0,1}] <link>`` | Writes a link file pointing to the the <file name>.      |
|                   | -link and -linkv1 will write versioned link, in which case the  |
|                   | the <file name> can not be overwritten.                         |
|                   | -linkv0 will write non-versioned link file.                     |
+-------------------+-----------------------------------------------------------------+

Examples
========

.. code-block:: gor

    nor fileA.nor | tsvappend fileB.nor

The query above will apppend the contents of ``fileA.gor`` to the file ``fileB.gor``.
