.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _PIPESTEPS:

=========
PIPESTEPS
=========
The :ref:`PIPESTEPS` command reads a number of analysis steps from a .yml file (gor dialog/report).

Usage
=====

.. code-block:: gor

	gor ... | PIPESTEPS filename.yml(stepentry,key=value)

The PIPESTEPS command reads one or more gor analysis steps from a .yml file

Examples
========

.. code-block:: yml

   DialogName:
    Description: |
        This is an example of a yml storing gor analysis steps
    stepentry: |
        calc a b+c | where a < d | group genome -count
