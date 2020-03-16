.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _TEE:

===
TEE
===
The TEE command mimics the UNIX command as it splits a GOR stream into two output streams from a single input. This is useful for writing to two separate files concurrently. It can only be used as part of the command line interface, GORpipe.

Usage
=====

.. code-block:: gor

    gor ... | TEE >(output-pipe) | ... commands

.. code-block:: gor

    nor ... | TEE >(output-pipe) | ... commands

Options
=======

+--------+---------------------------------------------------------+
| ``-h`` | Include the header of the TEE output-pip in the stdout. |
+--------+---------------------------------------------------------+

Examples
========

.. code-block:: gor

    gor file1.gor | tee >( where #1 != 'chr1' | write file1_nonchr1.gor) | where #1 = 'chr1' | write file1_chr1.gor
