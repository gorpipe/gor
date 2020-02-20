.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _TRYSELECT:

=========
TRYSELECT
=========
The **TRYSELECT** command works the same as SELECT, but ignores error on non-existing columns.

Usage
=====

.. code-block:: gor

	gor ... | TRYSELECT 1,2,#3-#5,Cola,7-Colb,Info*,Cola[+1]-Colg[-1],Colm-



