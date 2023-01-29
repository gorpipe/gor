.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _COLLECT:

=======
COLLECT
=======
The :ref:`COLLECT` command allows you collect statistical information over a fixed window. Select a single column and the window size, the command will then generate the sum, average, variance and std (if chosen) for the given window size.

Usage
=====

.. code-block:: gor

	gor ... | COLLECT column window [ options ]


Options
=======

+-------------------+----------------------------------------------------------------------+
| ``-sum``          | Calculates the sum over the defined input window                     |
+-------------------+----------------------------------------------------------------------+
| ``-ave``          | Calculates the average over the defined input window                 |
+-------------------+----------------------------------------------------------------------+
| ``-var``          | Calculates the variance over the defined input window                |
+-------------------+----------------------------------------------------------------------+
| ``-std``          | Calculates the std over the defined input window                     |
+-------------------+----------------------------------------------------------------------+


Examples
========

.. code-block:: gor

	gor ... | collect data 100 -ave -std

Returns a row where the column named `data` is collected and the average and std is calculated over a window of size 100.