.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _BAMFLAG:

=======
BAMFLAG
=======
The :ref:`BAMFLAG` command expands the FLAG bitmap column into multiple Boolean (1/0) columns. This makes the parameters in the FLAG bitmap easier to read.

The table below shows a list of the different parameters and their corresponding bit in the flag value.

.. list-table:: The Flag Column as expanded by BAMFLAG
   :widths: 3  6 5  6  15
   :header-rows: 1

   * - #
     - Binary
     - Decimal
     - Hexadecimal
     - Description
   * - 1
     - 1
     - 1
     - 0x1
     - Read paired
   * - 2
     - 10
     - 2
     - 0x2
     - Read mapped in proper pair
   * - 3
     - 100
     - 4
     - 0x4
     - Read unmapped
   * - 4
     - 1000
     - 8
     - 0x8
     - Mate unmapped
   * - 5
     - 10000
     - 16
     - 0x10
     - Read reverse strand
   * - 6
     - 100000
     - 32
     - 0x20
     - Mate reverse strand
   * - 7
     - 1000000
     - 64
     - 0x40
     - First in pair
   * - 8
     - 10000000
     - 128
     - 0x80
     - Second in pair
   * - 9
     - 100000000
     - 256
     - 0x100
     - Not primary alignment
   * - 10
     - 1000000000
     - 512
     - 0x200
     - Read fails platform/vendor quality checks
   * - 11
     - 10000000000
     - 1024
     - 0x400
     - Read is PCR or optical duplicate
   * - 12
     - 100000000000
     - 2048
     - 0x800
     - Supplementary alignment

Usage
=====

.. code-block:: gor

	gor file.bam | BAMFLAG [ -v ]

Options
=======

+----------+-----------------------------------------------------------------------+
| ``-v``   | Use verbose column names.                                             |
+----------+-----------------------------------------------------------------------+
