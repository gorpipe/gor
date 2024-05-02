.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GORIF:

=====
GORIF
=====

The :ref:`GORIF` command is designed to verify the existence of source files and also checks to ensure that the content of these files is not empty before executing the :ref:`GOR` command.

This ensures that the `GOR` command only runs if the specified source files are present, preventing errors due to missing data or empty files.

All GORIF queries begin with the GORIF command followed by the source file (or files) that you will be reading from.

Usage
=====

.. code-block:: gor

	gorif -dh file1 file2 file3 ... fileN

Options
=======

+----------------------------+---------------------------------------------------------------------------------------------------+
|  ``-dh col1\tcol2``        | Use the default header ( tab-separated ) if none of the source files exist.                       |
+----------------------------+---------------------------------------------------------------------------------------------------+

For the other available options, please refer to the :ref:`Gor` documentation.

Examples
========

.. code-block:: gor

    gorif #dbSNP# -dh chrom\tpos | top 1

.. code-block:: gor

    gor -p chr1:0-100 <(gorif -dh chrom\tpos file1.gor file2.gor)
