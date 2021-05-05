.. _parallelGOR:

======================
Parallelization in GOR
======================
The ability to run queries in parallel is an important aspect of the GOR architecture, making it possible to divide the workload across many processors and decrease the execution time of the query.

The GOR query language allows you to run queries in parallel using the :ref:`PGOR` and :ref:`PARTGOR` commands. :ref:`PGOR` partitions the query execution along the genomic axis, whereas :ref:`PARTGOR` allows for partition along the tag-partition axis of a GOR dictionary table, typically sample IDs. The maximum level of parallelism depends on the number of cores in the machines being used to run the query.


Basic Parallel Queries
======================
You will recall that we previously covered an introduction to parallel GOR queries in the chapter on :ref:`groupingAggregation`, which we will review here. In the following query, we are fetching the number of mutations per individual chromosomes by accessing the ``#dbsnp#`` table, as shown below:

.. code-block:: gor

   gor #dbsnp# | GROUP chrom -count

As we mentioned before, a more efficient way to process this information is to write a parallel GOR query, or :ref:`PGOR`, as follows:

.. code-block:: gor

   pgor #dbsnp# | GROUP chrom -count

.. _PARALLELexamples:

PARALLEL Examples
=================

Creating query with custom split regions
----------------------------------------
The example shown below can be used to split any gor query with custom split ranges.

.. code-block:: gor

   create #splits# = gor #genes# | top 10;
   parallel -parts [#splits#] <(gor -p #{col:chrom}:#{col:start}-#{col:stop} #dbsnp# | group chrom -count)

Using a split from the gene list, taking the 10 first genes in the list. Creating parallel execution where for each gene we count the number of snips using group -count.

The following sections will show more examples of how to use PGOR and PARTGOR to improve the speed of your queries.

.. _PGORexamples:

PGOR Examples
=============

Calculating the transition transversion ratio
---------------------------------------------
The example shown below can be used to calculate the transition-transversion ratio.

.. code-block:: gor

   create #temp# = pgor -split 300 #dbsnp# | where len(ref)=1 and len(alt)=1
   | calc transition = if(ref+’>’+alt in (’A>G’,’G>A’,’C>T’,’T>C’),1,0)
   | calc transversion = 1 - transition
   | group chrom -gc PID -sum -ic transition,transversion;

   gor [#temp#] | group genome -sum -ic sum_* | calc TiTv_ratio float(sum_sum_transition)/sum_sum_transversion

Using a split of 300 causes approximately 300 rows with chromosomal segments to be generated, representing the count within each partition.  The final step sum up these counts over the genome and calculates the tt-ratio.  If no split would be provided, by default, the query would generate one partition per chromosome.

Using Hash Strings in WHERE statements
--------------------------------------
### This notation needs more of an introduction. Ask about this. ###

In the above query, the partitions are 10Mb in size, overlapping by 2000 bases to ensure that all the sequence reads from the BAM file that provide coverage for the corresponding genome partition are read.

.. code-block:: gor

   create #temp# = pgor -split 10000000:2000 file.bam | pileup -span 2000 | where ##WHERE_SPLIT_WINDOW##;

   gor [#temp#] | write mypileup.gorz

The PGOR logic recognizes the special string in the WHERE command and replaces it with the appropriate filtering condition to avoid overlap of data in the temporary files stored in #temp#.

.. code-block:: gor

   create #temp# = pgor -split 10000000:2000 file.bam | pileup -span 2000 | where Chrom = '#{CHROM}' and pos < #{bpstop};

   gor [#temp#] | write mypileup.gorz

.. _PARTGORexamples:

PARTGOR Examples
================

Bi-Dimensional Parallelism
--------------------------
The following example shows how a :ref:`PGOR` command can be used inside the :ref:`PARTGOR` command expression.

.. note:: Using PGOR in a nested query is allowed inside a PARTGOR expression.

.. code-block:: gor

   create #temp# = partgor -dict #wgsvars# -parts 10 <(pgor #wgsvars# -f #{tags} | GROUP chrom -gc PN -count);
   gor [#temp#] | GROUP chrom -avg -ic allcount -count | RENAME avg_allcount variantsPerPN | RENAME allCount PNcount

The parallelism is therefore manifested in two dimensions; along the tag-partitions and along the genomic axis.

Calculating Case-Control Statistics
-----------------------------------
The query shown below calculates case-control statistics for PNs stored in the phenotype file pn2casectrl.tsv.  It does so using variants stored in a sparse manner in the table ``#wesvars#`` and coverage segments in ``#goodcov8#``, representing good coverage with sequence read depth of 8 or more.

.. code-block:: gor

   create ##allvariantsparts## = partgor #wesvars# -ff
   <(nor mypns.tsv | select PN | distinct)
   <(gor #wesvars# -f #{tags} | group 1 -gc reference,call );

   create ##allvariants## = pgor [##allvariantsparts##] | distinct;

   create ##ccvarsparts## = partgor -dict #wesvars# -ff <(nor pn2casectrl.tsv | SELECT PN | DISTINCT)
   <(gor [##allvariants##]
   | CALC pn '#{tags}' | SPLIT pn
   | VARJOIN -l -xl PN -xr PN -r -e 0 <(gor #wesvars# -f #{tags} | SELECT 1,2,reference,call,PN )
   | JOIN -snpseg -xl PN -xr PN -ic -maxseg 10000 <(gor #goodcov8# -f #{tags} ) | HIDE PNx
   | CALC het if(CallCopies = '1',1,0)
   | CALC hom if(CallCopies = '2',1,0)
   | CALC alleles het+2*hom
   | CALC present het+hom
   | CALC unknown if(present = 0 and overlapCount = 0,1,0)
   | CALC absent if(present = 0 and unknown = 0,1,0)
   | CALC absent_hom if(hom = 0 and unknown = 0,1,0)
   | CALC absent_alleles if(unknown = 0,2-alleles,0)
   | MULTIMAP -c PN -h pn2casectrlpheno.tsv
   | GROUP 1 -gc reference,call,casectrlpheno -sum -ic het-absent_alleles );

   pgor [##ccvarsparts##] | GROUP 1 -gc reference,call,casectrlpheno -sum -ic sum_* | RENAME sum_(.*) #{1}
   | PIVOT casectrlpheno -v CASE,CTRL -gc reference,call -e 0

First, the query finds all the exonic variants present in the samples listed in mypns.tsv.  This is done in two phases: first we use the GROUP command to find a distinct list of variants in each partition split generated by the PARTGOR command.

Then we create the ``##allvariants##`` relation by collapsing the variants from each partition using the **DISTINCT** command (here we could also have used GROUP 1 -gc 3,4).

Then we calculate a left-join from each possible variant and the sparse variants stored in ``#wesvars#``.  For the left-join to generate one row per PN, we must expand each variant with each possible PN.  Within each parallel partition, this is done by calculating the PN column as a comma-separated list of all the tags and then using the **SPLIT** command to generate one row per PN.  Then we perform the left-varjoin, using additional equi-join on PN and similarly we use a left-join into the segments (which maximum length is 10k bases).

Notice that we use the same filter option for both nested queries, accessing only data from the appropriate PNs in ``#wesvars#`` and ``#goodcov8#``.  Also, notice that the :ref:`GROUP` command aggregates the data to variant levels within each partition, thus the temporary files generated by the **CREATE** statements will not be prohibitively large, i.e. (#variants)x(#phenotypes)x(#partgor partitions).

Finally, we sum up the information from each :ref:`PARTGOR` partition, using the fact that the sum aggregation is distributive in nature, and we pivot the results to show all the counts in a single row per variant.

Using PARTGOR to Find deNovo Variants
-------------------------------------
The above query shows the use of :ref:`PARTGOR` to find deNovo variants in PNs for which the family relationships are defined in (PN,FN,MN) pedigree relation.

.. code-block:: gor

   def ##PNs## = nor pedigree.tsv | CALC x PN+','+FN+','+MN | SELECT x | RENAME x PN | SPLIT PN | SELECT PN;

   def ##PNsAndParents## = nor pedigree.tsv | WHERE listhasany('#{tags}',PN)
   | CALC x PN+','+FN+','+MN | SELECT x | RENAME x PN | SPLIT PN;

   create ##ccvarsparts## = partgor -dict #wesvars# -ff <(##PNs##) <(gor #wesvars# -s PN -ff <(##PNsAndParents##)
   | SELECT 1-4 | DISTINCT
   | VARJOIN -l -r -xl pn -xr pn <(gor #wesvars# -s PN -ff <(##PNsAndParents##) | SELECT 1-4,callcopies,PN )
   | JOIN -snpseg -xl PN -xr PN -ic -maxseg 10000 <(gor #goodcov8# -ff <(##PNsAndParents##) )
   | REPLACE callcopies if (callcopies='',if (overlapCount > 1,0,'NA'),callcopies)
   | HIDE PNx
   | PEDPIVOT PN pedigree.tsv -gc reference,call -e NA
   | INSET -c P_PN <(nor pedigree.tsv | WHERE FN != '' and MN != '' |  select PN )
   | CALC denovo if(P_callcopies in ('1','2') and F_callcopies = '0' and M_callcopies = '0',1,0)
   | WHERE denovo != 0 );

   gor [##ccvarsparts##]

Here we use the file-filtering option with the GOR command, through the definition ``##PNsAndParents##``  which uses the #{tag} variable to filter the pedigree relation such that we get not only the children in the given tag-partition but also the parents.  Thus, the same parent PNs may show up in multiple partition queries if they have children that don't cluster together in partition (something which is not possible to guarantee for arbitrary family structure).