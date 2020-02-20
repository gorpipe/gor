.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _REPLACE:

=======
REPLACE
=======
The :ref:`REPLACE` command is similar to the :ref:`CALC` command, except that the column specifier (``col``) in the expression indicates an existing column (or columns) that will be replaced with the expression value. It is possible to refer to the column being changed via ``#rc``, which denotes the “column at hand/in context” when a multicolumn expression is done in replace.

Note ``#rc`` is always a string so if you want to compare with size then use something like: if(isfloat(#rc),if(float(#rc)>2.0,2.0,float(#rc)),‘NaN’)

It can be preferable to use this command instead of the **SED** command in many cases.

Usage
=====

.. code-block:: gor

	gor ... | REPLACE col(s) expression


Examples
========

.. code-block:: gor

    ... | REPLACE colx,coly,sum_* IF(#rc in ('.',';'),'',#rc).

