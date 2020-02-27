.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _TRYHIDE:

=======
TRYHIDE
=======

The **TRYHIDE** removes the listed columns from the output if they are present. If they are not present, the command ignores
the error. Note that column 1 and 2 cannot be hidden when using TRYHIDE with a GOR query.

Usage
=====

.. code-block:: gor

	gor ... | TRYHIDE 3-5,7-

See also the :ref:`SELECT` and :ref:`RENAME` commands.
