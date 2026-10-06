.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _THROWIF:

=======
THROWIF
=======
The **THROWIF** command is a very simple tool which throws an exception if the contained statement is true.

By default the error message is ``Gor throw on: <condition>``. Use ``-m`` to give a custom message.

Usage
=====

.. code-block:: gor

   gor ... | THROWIF [<options>] <condition>

Options
=======

+--------------------+---------------------------------------------------------------------------------------+
| ``-retriable``     | If set an retriable exception is thrown.                                              |
+--------------------+---------------------------------------------------------------------------------------+
| ``-m <message>``   | Use ``<message>`` as the error message instead of ``Gor throw on: <condition>``.      |
|                    | Quote the message if it contains spaces.                                              |

Examples
========
Fail with a custom message:

.. code-block:: gor

   gorrow chr1,1 | calc status 'unmapped' | THROWIF -m 'liftover failed' status != 'mapped'

The error message is ``liftover failed``.