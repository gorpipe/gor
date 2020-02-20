.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _PREFIX:

======
PREFIX
======
The :ref:`PREFIX` command adds a specified prefix to the columns specified. The columns that you wish to have the prefix added to are defined in the same way as with the :doc:`SELECT` command. Note that name collisions in column name are resolved automatically by the GOR system by adding an "x" as a postfix to the column name.

Usage
=====

.. code-block:: gor

    gor ... | PREFIX 1,2,#3-#5,Cola,7-Colb,Info*,Cola[+1]-Colg[-1],Colm- prefix

Examples
========
.. code-block:: gor

    gor ... | PREFIX 1,2 position

In the above example, the first two columns will have the prefix "position" added to the column name.