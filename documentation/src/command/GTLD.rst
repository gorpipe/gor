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

To represent the variant pairs the output columns are chrom,pos,ref,alt,distance,posx,refx,altx,
and the genotype pair counts LD_g00,LD_g10,LD_g20,LD_g01,LD_g11,LD_g21,LD_g02,LD_g12,LD_g22
and from the genotype counts the -calc option uses the EM algorithm to phase the haplotypes and estimate
LD_D,LD_Dp,LD_r,De, using the following standard formulas:

      Nh = h00+h01+h10+h11
      p1 = (h00+h01)/Nh
      q1 = (h00+h10)/Nh
      LD_D = h00*h11-h10*h01
      d = if (LD_D<0.0) max((p1*q1),(1.0-p1)*(1.0-q1)) else min((p1*(1-q1)),(1.0-p1)*q1)
      rd = sqrt(p1*(1.0-p1)*q1*(1.0-q1))
      LD_Dp = LD_D/d
      LD_r = LD_D/rd

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
| ``-sum``            | Aggregate the correlation counts LD_g00,LD_g10,LD_g20,LD_g01,LD_g11,LD_g21,LD_g02,LD_g12 and LD_g22|
|                     | from single genotype character values column.                                                      |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-calc``           | Calculate Dprime and the correlation coefficient, r, from LD_g00,..,LD_g22.                        |
+---------------------+----------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

    /* An example calculating LD from a stream of (chr,pos,ref,alt,bucket,values) */

    pgor -split 10000000:100000 [#buckethorvars#] | GTLD -sum -calc -f 100000
    | where posx != pos and (abs(LD_Dp) > 0.2 or abs(LD_r)>0.2)
    | where ##WHERE_SPLIT_WINDOW##

The above query example calculates LD using genomic range parallelism from all the variants in all the buckets.
The following query calculate LD in a single gene:

.. code-block:: gor

    gor #genes# | where gene_symbol = 'BRCA2' | join -segsnp -f 1000000 -ir <(gor [#buckethorvars#])
    | GTLD -sum -calc -f 100000 | where posx != pos and (abs(LD_Dp) > 0.2 or abs(LD_r)>0.2)

Finally, the following query uses sample parallelism to calculate the LD, allowing for bigger range with less memory usage:

.. code-block:: gor

    create xxx = partgor -dict horvars.gord <(pgor horvars.gord -f #{tags} -nf
    | csvsel -gc reference,call -u 3 horvarsbuckets.tsv <(nor horvarsbuckets.tsv | select #1 | where listhasany(PN,'#{tags}'))
    | varjoin -r <(gor #VEP# | where max_impact in ('HIGH','MODERATE') | select 1-4,max_consequence,max_impact)
    | GTLD -sum -f 1000000 | where posx != pos;

    pgor [xxx] | GTLD -calc | where (abs(LD_Dp) > 0.2 or abs(LD_r)>0.2) | hide LD_g00-LD_g22 | sort 1 -gc reference,call,LD_r:n

Note that it is possible to have asymmetry in the LD calculation pairs. I.e. you can specify which variants in the input stream are only used as the left-variant in the pair.

An example of this is shown below:

.. code-block:: gor

	gor  varsofinterest.gor
	| varjoin -ir #gtfreeze#
	| varjoin -r -e 0 <(gor myleftvars.gor | select 1-4 | calc useonlyasleftvar 1)
	| GTLD -sum -calc -f #maxlddist#


This scripts demonstrates how to test the caluclation of D from first principles by
simulating haplotypes of biallelic SNPs in disequilibrium and genotypes based on HWE

.. code-block:: gor

    def #pns# = 100000;
    def #p# = 0.3;
    def #q# = 0.02;
    def #D# = 0.1; /* Should ideally be smaller than all of: p*q, (1-p)*(1-q), (1-p)*q, p*(1-q) */

    nor <(gor <(norrows #pns#  | calc p #p# | calc q #q# | calc D #D#
    | calc h1 if(random()<p,if(random()<(p*q+D)/p,'0_0','0_1'),if(random()<((1-p)*q-D)/(1-p),'1_0','1_1'))
    | calc h2 if(random()<p,if(random()<(p*q+D)/p,'0_0','0_1'),if(random()<((1-p)*q-D)/(1-p),'1_0','1_1'))
    | colsplit h1 2 snp_f -s '_'
    | colsplit h2 2 snp_m -s '_'
    | calc gt1 decode(snp_f_1+'_'+snp_m_1,'0_0,0,0_1,1,1_0,1,1_1,2')
    | calc gt2 decode(snp_f_2+'_'+snp_m_2,'0_0,0,0_1,1,1_0,1,1_1,2')
    | group -lis -sc gt1,gt2 -s '' -len 1000000
    | calc values lis_gt1+','+lis_gt2
    | hide lis_*
    | split values
    | rownum
    | rename rownum pos
    | calc chrom 'chr1'
    | calc ref 'A'
    | calc alt 'T'
    )
    | select chrom,pos,ref,alt,values
    | gtld -sum -f 100 | gtld -calc /* can also be written as single GTLD -sum -calc */
    )
    | multimap -cartesian <(norrows #pns# | calc p #p# | calc q #q# | calc D #D#
    | calc h1 if(random()<p,if(random()<(p*q+D)/p,'0_0','0_1'),if(random()<((1-p)*q-D)/(1-p),'1_0','1_1'))
    | group -gc h1 -count
    | replace allcount float(allcount)/#pns#
    | pivot h1 -v 0_0,0_1,1_0,1_1 -e 0
    | calc De #1*#4-#2*#3
    | select De)
    | select distance,LD_D-LD_r,De
    | throwif distance = 0 and abs(ld_Dp-1.0)>0.01
    | throwif abs(distance) = 1 and abs(LD_D - De)>0.01

Related commands
----------------

:ref:`CSVCC` :ref:`CSVSEL` :ref:`GTGEN`