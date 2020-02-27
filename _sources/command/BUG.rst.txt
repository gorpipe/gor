.. _BUG:

===
BUG
===

Usage
=====

.. code-block:: gor

	gor ... | bug [setup:probability | process:probability | finish:probability]

This command is really just for testing the system to see how tolerant it is in handling exceptions arising in different phases of the query process, i.e. setup construction phase, row processing phase, or the query cleanup finish phase.

This command should not be part of the regular user-manual.

Example
=======

.. code-block:: gor

   gor #dbSNP# | bug process:0.001 | where random() < 0.00001 | join -snpseg <(gor #genes# | bug setup:0.1 )

The above query causes a "BUG process" error with the probability of 1/1000 rows and a "BUG setup" error with 10 chance in the setup of nested query.