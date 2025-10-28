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

	nor ... | tsvappend <file name> [-noheader] [-prefix <prefix>] [-link <link path> [-lmeta <link meta>]

Options
=======

+-------------------+-----------------------------------------------------------------+
| ``-prefix <hf>``  | Takes in a text source containing prefix to be prepended to the |
|                   | file written. Also support string in single quotes              |
+-------------------+-----------------------------------------------------------------+
| ``-noheader``     | Don't write a header lines.  Not valid with gor/gorz/nor/norz.  |
+-------------------+-----------------------------------------------------------------+
| ``-link <link>``  | Writes a versioned link file pointing to the the <file name>.   |
|                   | The <file name> should not be overwritten if it has previously  |
|                   | been used in a link file.                                       |
+-------------------+-----------------------------------------------------------------+
| ``-linkmeta <m>`` | Writes <meta> as meta data to the <link>.  <meta> is string for |
|                   | of comma separated key=value elements.                          |
+-------------------+-----------------------------------------------------------------+

Examples
========

.. code-block:: gor

    nor fileA.nor | tsvappend fileB.nor

The query above will apppend the contents of ``fileA.gor`` to the file ``fileB.gor``.
