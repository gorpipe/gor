.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _CMD:

===
CMD
===
This manual entry pertains to the :ref:`CMD` command as it is used as a pipe step. CMD allows you to execute operating system commands that return data that you assume is tabular data within GOR queries.

If you are using external command as a source for your data, then it is better to use :ref:`GORCMD`, for genomic-ordered data, and :ref:`NORCMD`, for non-ordered data.

The CMD analysis step supports nested queries such as: gor my.gor | cmd {python my.py <(gor #genes# | where gene_start-gene_stop < 10000)}.

External commands need to be encapsulated between curly brackets, e.g. cmd {command}.

Usage
=====

.. code-block:: gor

   gor ... | CMD {command}

Options
=======

+-----------------+----------------------------------------------------------------------+
|  ``-n``         | Execute the command within a NOR query.                              |
+-----------------+----------------------------------------------------------------------+
|  ``-s <type>``  | Specifies that type of source that will be read [BAM, GOR, or VCF]   |
+-----------------+----------------------------------------------------------------------+
|  ``-h``         | Tells the command to not send the header.                            |
+-----------------+----------------------------------------------------------------------+

Examples
========

.. code-block:: gor

   gor ... | CMD {<myscript.py>}

