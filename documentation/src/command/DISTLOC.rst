.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _DISTLOC:

=======
DISTLOC
=======
The **DISTLOC** command is similar to the TOP command, except that it counts different :term:`loci<locus>` instead of rows. It may be more convenient to use DISTLOC in cases where there are multiple rows per locus. The command terminates the upstream process once all the loci have been passed through.

To take an example, we may be opening a BAM file that has multiple rows that begin from a single position on the chromosome each representing a different sequence read. The DISTLOC command would count all of those individual reads as a single locus.

The command takes one parameter, namely the number of loci to allow through.

Usage
=====

.. code-block:: gor

	gor ... | DISTLOC <number>