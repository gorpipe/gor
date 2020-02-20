.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _LIFTOVER:

========
LIFTOVER
========

The **LIFTOVER** command is used to convert GOR data from one reference genome build to another.  It provides similar functionality as the LiftOver functionality at UCSC, however, executes much faster.  The output of a LIFTOVER command is therefore in different coordinates than the input and therefore it is for instance meaningless to join such data with the source file or other sources in the original genome build.

The mapping between the genome builds for the LIFTOVER command is stored in GOR files, e.g. config/liftover/hg19tohg38.gor, which are typically generated from chain files from UCSC.

The LIFTOVER command must know the nature of the data, e.g. variants, BAM sequence reads or segments, in order to reverse complement sequence columns where appropriate.  Note, that the LIFTOVER command must re-order the entire output and is therefore a blocking operation similar to SORT genome.

Usage
=====

.. code-block:: gor

	gor ... | liftover config/liftover/hg18tohg19.gor [ -snp | -seg | -var | bam ]

Options
=======

+------------------+-------------------------------------------------------------------------+
| ``-snp``         | Single nucleotide positions.                                            |
+------------------+-------------------------------------------------------------------------+
| ``-seg``         | Segment data with start and stop (default).                             |
+------------------+-------------------------------------------------------------------------+
| ``-var``         | Variation format (chrom,pos,ref,alt).                                   |
+------------------+-------------------------------------------------------------------------+
| ``-bam``         | BAM sequence read format (chrom,pos,end,Qname,..).                      |
+------------------+-------------------------------------------------------------------------+
| ``-ref col``     | The column denoting the reference seq. (default #3,reference,ref).      |
+------------------+-------------------------------------------------------------------------+
| ``-alt col``     | The column denoting the alternate (alleles) seq. (default #4,call,alt). |
+------------------+-------------------------------------------------------------------------+
| ``-build value`` | Source build prefix (default hgOld)                                     |
+------------------+-------------------------------------------------------------------------+
| ``-all``         | Include all mappings, not just the one with best score (default).       |
+------------------+-------------------------------------------------------------------------+

Examples
========
When doing a liftover on segments, we can use the approach below to minimize the unmapped segments.  The occurrence of unmapped segments is because of partial overlap with the segments that come from the UCSC chain-files.

The query below maps the individual overlaps and then merges them (given they are not too far apart, e.g. 1000bp).  The 20Mbp is the maxseg in the liftover segments.  Rownum and liftsplits is just calculated to be able to see how each gene is split up.

.. code-block:: gor

   gor #genes# | ROWNUM | JOIN -segseg -maxseg 20000000 -rprefix lift
      <(gor config/liftover/hg19tohg38.gor | SELECT 1-3)
   | GRANNO 1 -gc 3-rownum -count | RENAME allcount liftSplits
   | REPLACE #2 if(lift_tStart>#2,lift_tStart,#2) | REPLACE #3 if(lift_tEnd<#3,lift_tEnd,#3)
   | SELECT 1-4,rownum,liftSplits
   | SORT 20000000
   | LIFTOVER config/liftover/hg19tohg38.gor -seg -build hg19
   | REPLACE #2 #2-1000 | REPLACE #3 #3+1000
   | SEGSPAN -gc 4-rownum,liftsplits
   | REPLACE #2 #2+1000 | REPLACE #3 #3-1000
   | GREP CNTNAP3B | GRANNO chrom -sum -ic segcount


Relevant links
==============

https://genome.ucsc.edu/cgi-bin/hgLiftOver
https://genome.ucsc.edu/goldenpath/help/chain.html
http://hgdownload.cse.ucsc.edu/goldenpath/hg19/liftOver/
