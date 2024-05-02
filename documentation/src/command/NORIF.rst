.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _NORIF:

=====
NORIF
=====

The :ref:`NORIF` command is designed to verify the existence of the source file and also checks to ensure that the content of the file is not empty before executing the :ref:`NOR` command.

This ensures that the `NOR` command only runs if the specified source files are present, preventing errors due to missing data or empty files.

All NORIF queries begin with the NORIF command followed by the source file that you will be reading from.

Usage
=====

.. code-block:: gor

	norif file.tsv | ...


Options
=======

+---------------------------+---------------------------------------------------------------------------------------------------+
| ``-dh col1\tcol2``        | Use the default header ( tab-separated ) if the source file doesn't exist.                        |
+---------------------------+---------------------------------------------------------------------------------------------------+

For the other available options, please refer to the :ref:`Nor` documentation.

Examples
========

.. code-block:: gor

    norif file.tsv -dh column1\tcolumn2\tcolumn3 | top 1

.. code-blocks:: gor

    nor <(norif file.tsv | ... )
