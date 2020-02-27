.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _NORCMD:

======
NORCMD
======
The NORCMD command allows you to execute operating system commands that return data that you assume is tabular data within GOR queries. In the case of the NORCMD command then it is an external command that returns data that we assume is not genomically ordered.

External commands need to be encapsulated between curly brackets, e.g. norcmd {command}.

Usage
=====

.. code-block:: gor

   norcmd {<OS_command>} | ...

Options
=======

+-----------------+----------------------------------------------------------------------+
|  ``-s <type>``  | Specifies that type of source that will be read [BAM, GOR, or VCF]   |
+-----------------+----------------------------------------------------------------------+

Examples
========

.. code-block:: gor

   norcmd {cat file.tsv} | ...
