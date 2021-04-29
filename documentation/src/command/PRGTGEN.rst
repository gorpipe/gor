.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _PRGTGEN:

=======
PRGTGEN
=======
The **PRGTGEN** command is a probabilistic version of **GTGEN**, i.e. it does the same but also does a simple joint
variant calling and returns the genotypes in probabilistic form.

The incoming stream is a gor stream containing genotypes, one pn per line. The genotypes can come in the following:
i) Probabilistic: A triplet containing the probabilities of Ref-Ref, Ref-Alt, Alt-Alt. Can either be stored in a column
named GP, in Phred scale in a column named PL or in logarithmic scale in a column named GL.
ii) Raw: Then there should be one column called 'depth' containing the reading depth and another named 'callratio'
containing the call ratio.

As input, there should be one tag-bucket source, a coverage source and optionally a gor source containing prior allele
frequencies estimates and the number of samples behind that estimate.

The coverage file should contain segments, a depth column and a pn column.

The gor source containing the allele frequencies and the number of samples behind the estimates, should contain an AF
column with the allele frequencies and an AN column with the number of samples. It should be interpreted as follows:
The tag list from the tag-bucket file is thought of as coming from a larger collection of pns and the allele frequency
given in the source should be thought of as having been estimated for some number of samples from the cohort, excluding
the ones we are calling now.

Usage
=====

.. code-block:: gor

    gor ... | PRGTGEN tagbucketrelation [optional: prior_gor_source] coverage_gor_source [ attributes ]

Options
=======
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gc cols``        | Grouping columns other than (chrom,pos)                                                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-prgc cols``      | Grouping columns for prior input other than (chrom,pos)                                                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-pn pnColName``   | Override the default PN colunm name used to represent the tag value.                               |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-maxseg bpsize``  | The maximum span size of the coverage segments.  Defaults to 10000 (10kb).                         |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-e error rate``   | The error rate of the reads.                                                                       |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-fpab prior``     | The prior assumed to have been used when prob likelihoods (PL) are used.  Use 0.33 for flat prior. |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-fpbb prior``     | The prior for the alternative homozygot.  Use fpab = fpbb = 0.333 for flat prior.                  |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-maxit mi``       | The maximum number of iterations. Default value is 20.                                             |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-tol tolerance``  | The tolerance. Default value is 1e-5.                                                              |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gp col``         | The GP col (from i)). Is looked up by name if not given.                                           |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-pl col``         | The PL col (from i)). Is looked up by name if not given.                                           |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gl col``         | The GL col (from i)). Is looked up by name if not given.                                           |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-pabc col``       | The probability for heterozygot genotype pAB=2pq                                                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-pbbc col``       | The probability for homozygot alt, pBB = pAltAlt, equal to Af*Af if variant in HWE                 |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-anc col``        | The allele number column in the prior source. Is looked up by name if not given.                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-cc col``         | The call copies column from ii) Looked up by name if not given.                                    |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-crc col``        | The call ratio column from iii) Looked up by name if not given.                                    |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-ld col``         | The depth column from the incoming source in ii). Looked up by name if not given.                  |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-rd col``         | The depth column from the prior source. Lookup up by name if not given.                            |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-th threshold``   | The threshold to use, if the genotypes should be written out as discrete genotypes. Giving the     |
|                     | threshold indicates that the hard calls should be written out.                                     |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-psep separator`` | The separator in the probability triplet. Default value is ';'. Must be a single character.        |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-osep separator`` | The separator to separate the genotypes, in case the triplets should we written out.               |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-combgt``         | Try to combine the het and alt-hom probabilities if no genotype exceeds threshold.  The genotype   |
|                     | corresponding to the one with larger probability is used, i.e. het or hom-alt.  This is to save    |
|                     | genotypes that are between het/hom-alt state from being called unknown.                            |
+---------------------+----------------------------------------------------------------------------------------------------+




Examples
========

.. code-block:: gor

    /* An example simulating genotypes and comparing incremental joint-calling with single step join-calling */

    def #r# = rename #1 PN | replace #1 #1+1;
    def #n# = 999;
    def #bucksize# = 500;

    create #bucket# = norrows #n# | #r# | rownum | calc bucket 'b'+str(1+div(rownum, #bucksize# )) | hide rownum;
    create #pns# = nor [#bucket#] | select pn;

    create #cov# = gor <(nor [#pns#] | calc chrom 'chr1' | calc bpstart 0 | calc bpstop 10 | calc depth 10
    | select chrom-depth,pn);

    create #vars# = gor <(nor [#pns#] | calc chrom 'chr1' | calc pos 10 | calc ref 'A' | calc alt 'C'
    | calc CallCopies if(random()<0.01 or pn > '950' and random()<0.55 or pn = '999' /* or pn = '1' */,1,0)
    | calc Depth 10
    | calc CallRatio form(if(pn='999' /* or pn='1' */,1.0,if(callcopies=1, round(Depth/2.0)+round(Depth*0.9*(-0.5+random())), round(-1+random()+random()+random()) )/Depth),4,4)
    | select chrom-callratio,pn)
    | where callratio>0;

    create #pns1# = nor [#pns#] | top 950;
    create #bucket1# = nor [#bucket#] | inset -c pn [#pns1#];

    create #pns2# = nor [#pns#] | skip 950;
    create #bucket2# = nor [#bucket#] | inset -c pn [#pns2#];

    def #prthr# = 0.9;
    def #e# = 0.001;
    def #skip# = skip -5;

    create #gt# = gor [#vars#] | #skip#
    | inset -c pn [#pns#]
    | prgtgen -gc ref,alt [#bucket#] [#cov#] -e #e#
    | csvsel -gc 3,4,af,an,pab,pbb -vs 2 [#bucket#] [#pns#]  -tag pn
    | calc pr chars2prprpr(value)
    | calc value2 chars2gt(value,#prthr#);

    create #gt1# = gor [#vars#] | #skip#
    | inset -c pn [#pns1#]
    | prgtgen -gc ref,alt [#bucket1#] <(gor [#cov#] | inset -c PN [#pns1#]) -e  #e#
    | csvsel -gc 3,4,af,an,pab,pbb -vs 2 [#bucket1#] [#pns1#]  -tag pn
    | calc pr chars2prprpr(value)
    | calc value2 chars2gt(value,#prthr#);

    create #gt2# = gor [#vars#] | #skip#
    | inset -c pn [#pns2#]
    | prgtgen -gc ref,alt [#bucket2#] <(gor [#cov#] | inset -c PN [#pns2#]) -e  #e#
    | csvsel -gc 3,4,af,an,pab,pbb -vs 2 [#bucket2#] [#pns2#]  -tag pn
    | calc pr chars2prprpr(value)
    | calc value2 chars2gt(value,#prthr#);

    create #af1# = gor [#gt1#] | select 1-pbb | top 1 /* | replace pbb pbb+(1/AN)*(1/AN) */;

    create #gt2af# = gor [#vars#] | #skip#
    | inset -c pn [#pns2#]
    | prgtgen -gc ref,alt [#bucket2#] [#af1#] <(gor [#cov#] | inset -c PN [#pns2#]) -e #e#
    | where len(values)>1
    | csvsel -gc 3,4,af,an,pab,pbb -vs 2 [#bucket2#] [#pns2#]  -tag pn
    | calc pr chars2prprpr(value)
    | calc value2 chars2gt(value,#prthr#);

    /*
    gor [#gt2af#] | varjoin -r -xl pn -xr pn [#gt2#] | varjoin -r -xl pn -xr pn [#gt#] | varjoin -r [ #vars#] -xl pn -xr pn
    | where callcopies = -1 or value2xx = 2 | hide pn,pnx,pnxx,ref,alt| colnum
    */

    gor [#gt2af#] | varjoin -r -xl pn -xr pn [#gt2#] | varjoin -r -xl pn -xr pn [#gt#]
    | calc err_gt2af if(value2!=value2xx,1,0) | calc err_gt2 if(value2x!=value2xx,1,0)
    | group 1 -gc an,af,anx,afx,anxx,afxx,value2xx,value2,value2x -sum -ic err* -count

.. code-block:: gor

    /* An example showing how to inspect in impact of the priors -fpab 0.01 -fpbb 0.001 by looking at the genotypes */
    /* Note that option -combgt does not impact the prob-triplet chars2prprpr(value) */

    gor [#vars#] | inset -c pn <(nor [#bucket#] | top 8)
    | replace depth if(pn=1,4,8)
    | replace callratio if(pn=1,0.25,0.2)
    | hide callcopies
    | calc gp '0.4;0.2;0.2'
    | hide callratio,depth
    | prgtgen -fpab 0.01 -fpbb 0.001  -gc ref,alt <(nor [#bucket#] | top 8)
      <(gor [#cov#] | replace depth if(pn=1,6,8) | inset -c pn <(nor [#bucket#] | top 8) )  -e 0.00001
    | csvsel -gc 3,4,af,an,pab,pbb -vs 2 <(nor [#bucket#] | top 8) <(nor [#bucket#] | top 8| select pn)  -tag pn
    | calc pr chars2prprpr(value)
    | calc value2 chars2gt(value,#prthr#)


Related commands
----------------

:ref:`CSVCC` :ref:`CSVSEL` :ref:`GTLD` :ref:`GTGEN`