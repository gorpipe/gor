.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _THROWIF:

=======
THROWIF
=======
The **THROWIF** command is a very simple tool which throws an exception if the contained statement is true.

Usage
=====

.. code-block:: gor

   gor ... | THROWIF [<options>] <condition>

Options
=======

+--------------------+---------------------------------------------------------------------------------------+
| ``-retriable``     | If set an retriable exception is thrown.                                              |
+--------------------+---------------------------------------------------------------------------------------+