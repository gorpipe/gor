.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _GORCMD:

======
GORCMD
======
The :ref:`GORCMD` command allows you to execute operating system commands that return data that you assume is tabular data within GOR queries. When we run GORCMD, we assume that the external command returns data that is genomic-ordered.

The GORCMD command supports nested queries such as: gorcmd python my.py <(gor #genes# | where gene_start-gene_stop < 10000)

External commands need to be encapsulated between curly brackets, e.g. gorcmd {command}.

Usage
=====

.. code-block:: gor

   gorcmd {<OS_command>} | ...

Options
=======

+-----------------+----------------------------------------------------------------------+
|  ``-s <type>``  | Specifies that type of source that will be read [BAM, GOR, or VCF]   |
+-----------------+----------------------------------------------------------------------+

Examples
========

.. code-block:: gor

   gorcmd {myscript.py} | ...
