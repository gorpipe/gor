.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _GTLD:

====
GTLD
====
The **GTLD** command is used to calculate linkage disequilibrium between genotypes in different locations.
The command takes in an input stream and uses a self-join style way to calculate the LD in the surrounding region
as specified with the ``-f`` option.  Note that large span requires large memory buffers.  The command can be
used in parallel, however, then one must be careful that the genomic range partitions overlap by the amount
specified with the ``-f`` option.  Similarly, it is possibly to use partition parallelism via **PARTGOR**
and separate the sum step and the calculation steps for Dprime and r.

The input stream must have ``VALUES`` column storing the genotypes (as 0,1,2, or 3), with or without a ``BUCKET``
column.

Usage
=====

.. code-block:: gor

    gor ... | GTLD [ attributes ]


Options
=======

+---------------------+----------------------------------------------------------------------------------------------------+
| ``-f distance``     | The maximum span for LD calculation in base pairs.                                                 |
|                     | Note very large span can require time and memory                                                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-sum``            | Aggregate the correlation counts LD_x11,LD_x12,LD_x21, and LD_x22,                                 |
|                     | from single genotype character values column.                                                      |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-calc``           | Calculate Dprime and the correlation coefficient r from LD_x11,LD_x12,LD_x21, and LD_x22.          |
+---------------------+----------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

    /* An example calculating LD from a stream of (chr,pos,ref,alt,bucket,values) */

    pgor -split 10000000:100000 [#buckethorvars#] | GTLD -sum -calc -f 100000
    | where posx != pos and (LD_Dp > 0.2 or LD_r)
    | where ##WHERE_SPLIT_WINDOW##

The above query example calculates LD using genomic range parallelism from all the variants in all the buckets.
The following query calculate LD in a single gene:

.. code-block:: gor

    gor #genes# | where gene_symbol = 'BRCA2' | join -segsnp -f 1000000 -ir <(gor [#buckethorvars#])
    | GTLD -sum -calc -f 100000 | where posx != pos and (LD_Dp > 0.2 or LD_r)

Finally, the following query uses sample parallelism to calculate the LD, allowing for bigger range with less memory usage:

.. code-block:: gor

    create xxx = partgor -dict horvars.gord <(pgor horvars.gord -f #{tags} -nf
    | csvsel -gc reference,call -u 3 horvarsbuckets.tsv <(nor horvarsbuckets.tsv | select #1 | where listhasany(PN,'#{tags}'))
    | varjoin -r <(gor #VEP# | where max_impact in ('HIGH','MODERATE') | select 1-4,max_consequence,max_impact)
    | GTLD -sum -f 1000000 | where posx != pos;

    pgor [xxx] | GTLD -calc | where LD_Dp > 0.2 or LD_r | hide LD_x11,LD_x12,LD_x21,LD_x22 | sort 1 -gc reference,call,LD_r:n

Note that it is possible to have asymmetry in the LD calculation pairs. I.e. you can specify which variants in the input stream are only used as the left-variant in the pair.

An example of this is shown below:

.. code-block:: gor

	gor  varsofinterest.gor
	| varjoin -ir #gtfreeze#
	| varjoin -r -e 0 <(gor myleftvars.gor | select 1-4 | calc useonlyasleftvar 1)
	| GTLD -sum -calc -f #maxlddist#

Related commands
----------------

:ref:`CSVCC` :ref:`CSVSEL` :ref:`GTGEN`