.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _MULTIMAP:

========
MULTIMAP
========
The :ref:`MULTIMAP` command is similar to the :ref:`MAP` command, however in this case, the map file may contain a one-to-many mapping, which results in multiple output rows per input row.

Usage
=====

.. code-block:: gor

	gor ... | MULTIMAP mapfile.txt -c cols [ Attributes ]