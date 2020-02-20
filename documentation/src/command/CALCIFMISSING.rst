.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _CALCIFMISSING:

=============
CALCIFMISSING
=============
The :ref:`CALCIFMISSING` is similar to :ref:`CALC`, except it only adds columns if they do not already exist.

Usage
=====

.. code-block:: gor

	gor ... | CALCIFMISSING colname expression

	gor ... | CALCIFMISSING colname1,colname2 expression1,expression2