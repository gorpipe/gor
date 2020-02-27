==============
Variation Data
==============
Genetic variation is the result of individuals having a unique blend of variations in their genomes. The most common of these is a Single Nucleotide Polymorphism, or SNP, which is a change in a single nucleotide in the sequence in comparison with a reference genome. Other types of variation may be in the form of insertions of any number of bases or deletions of a string of bases, which are referred to collectively as Indels.

The variation data for an individual can be stored in a number of ways. Variant Call Format, or VCF, files can be created that include only Indel variant or only SNP variants. The sample data that you will be working with in the Sequence Miner contains both Whole Genome Sequence and Whole Exon Sequence data, the latter of which only includes variation data at those areas of the genome that take place on an exon (in other words, excluding non-gene coding areas of the genome).

There is a certain amount of ambiguity involved in the representation of equivalent variations in genomic data. The variation columns might not have the same representation for the same variants or indels might have a slightly different notation in a call for individual A than for individual B. For example, a variant may be represented as (chr1, 9, 'GA', 'GC') in one patient and (chr1, 10, 'A', 'C') in another. Likewise, the annotated variants that we keep in the VEP table may not be notated in the same manner as in the file for the individual.

The following chapters describe commands in the GOR Query Language that deal specifically with this representation issue and other issues that arise when working with variation data.

.. _joiningVariationData:

Joining Variation Data
======================
The GOR query language has a special **VARJOIN** command to join relations with variation data or ":term:`variants`" (for example, VCF-like formats with columns for chromosome, position, reference, and call) and resolve this ambiguity on the fly.

To understand this command better, we will take the example of two separate tables, the ``#wgsvars#`` table again which contains the variation data for all the subjects in the example study and the Variation Effect Predictor table, which has the alias ``#VEP#`` and contains annotations for variants.

First of all, let's take a look at the variation data for a single individual on chromosome 21. In this example we are only interested in the first four columns of the table, so we can select only those columns. Also, we can use the ``-f`` option to filter the output of the query on the ``PN`` column. This would give us about 80.658 rows.

.. code-block:: gor

   gor -p chr21 #wgsVars# -f C416TO_INDEX | SELECT 1-4

.. list-table:: #wgsvars# for a single individual (C416TO_INDEX)
   :widths: 10  15  5 5
   :header-rows: 1

   * - CHROM
     - POS
     - Reference
     - Call
   * - chr21
     - 9411318
     - C
     - T
   * - chr21
     - 9411410
     - C
     - T
   * - chr21
     - 9411500
     - G
     - T
   * - chr21
     - 9411602
     - T
     - C
   * - chr21
     - 9411609
     - G
     - T
   * - ...
     - ...
     - ...
     - ...

For our other data set, let's take only the entries from the ``#VEP#`` table that have a Max_Impact of 'HIGH' as shown in the partial table below, which would give us a stream of about 5.412 rows.

.. code-block:: gor

   gor -p chr21 #VEP# | WHERE Max_Impact = 'HIGH' | SELECT 1-7

.. list-table:: #wgsvars# for a single individual (C416TO_INDEX)
   :widths: 10 10 5  5  5 25  10
   :header-rows: 1

   * - Chrom
     - Pos
     - Reference
     - Call
     - Max_Impact
     - Biotype
     - Gene_Symbol
   * - chr21
     - 9909279
     - T
     - A
     - HIGH
     - processed_transcript_KNOWN,transcribed_unprocessed_pseudogene_KNOWN
     - TEKT4P2
   * - chr21
     - 9911927
     - C
     - A
     - HIGH
     - transcribed_unprocessed_pseudogene_KNOWN
     - TEKT4P2
   * - chr21
     - 10862623
     - T
     - C
     - HIGH
     - IG_V_gene_KNOWN
     - IGHV1OR21-1
   * - chr21
     - 10862624
     - G
     - T
     - HIGH
     - IG_V_gene_KNOWN
     - IGHV1OR21-1
   * - chr21
     - 10862629
     - G
     - A
     - HIGH
     - IG_V_gene_KNOWN
     - IGHV1OR21-1
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...
     - ...

If we were to perform a basic join of these two tables with the standard -snpsnp join, we would get 37 rows. However, this join is only comparing the position columns in the two tables. It is not taking into account the variation data in these two columns. A quick scan of the output stream shows that there are many joined rows that are not actually the same variants.

To take the variants into account, we use the **VARJOIN** command. We don't need to specify the type of join in this case.

.. code-block:: gor

   gor -p chr21 #wgsVars# -f C416TO_INDEX | SELECT 1-4
   | VARJOIN #VEP# | WHERE (Max_Impact = 'HIGH') AND Reference != Referencex | SELECT 1-10

.. list-table:: #wgsvars# for a single individual (C416TO_INDEX)
   :widths: 10 10 5  5  10 5  5  5 25  10
   :header-rows: 1

   * - Chrom
     - Pos
     - Reference
     - Call
     - Posx
     - Referencex
     - Callx
     - Max_Impact
     - Biotype
     - Gene_Symbol
   * - chr21
     - 28215836
     - GC
     - G
     - 28215837
     - CC
     - C
     - HIGH
     - protein_coding_PUTATIVE
     - ADAMTS1
   * - chr21
     - 34948684
     - G
     - GA
     - 34948685
     - A
     - AA
     - HIGH
     - protein_coding_KNOWN,protein_coding_PUTATIVE
     - SON
   * - chr21
     - 38092247
     - GA
     - G
     - 38092257
     - AA
     - A
     - HIGH
     - protein_coding_PUTATIVE
     - SIM2
   * - chr21
     - 47545369
     - A
     - AC
     - 47545376
     - C
     - CC
     - HIGH
     - protein_coding_NOVEL
     - COL6A2


.. _varjoinTypes:

Different Types of VARJOIN
--------------------------
As with basic **JOIN** commands, the **VARJOIN** command has flags for ``-l``, ``-i``, and ``-n``, which allow you to perform left, intersect, and negative joins respectively.

A full description of VARJOIN can be found on the :ref:`VARJOIN reference page<VARJOIN>`.


.. _mergingVariants:

Merging Variants
================
The :ref:`VARMERGE` command ensures that overlapping variants are denoted in an equivalent manner.

If a stream with two variants such as and (chr1,9,'GA','GC',PN2) and (chr1,10,'A','C',PN1) is analyzed for the frequency of variants with "gor ... | group 1 -gc ref,alt -count" the result would be incorrect, i.e. two rows with (chr1,9,'GA','GC',1) and (chr1,10,'A','C',1) since the group command can make no assumptions about the variant data.

The proper way to do the above analysis is

.. code-block:: gor

   gor ... | VARMERGE ref alt -nonorm | GROUP 1 -gc ref,alt -count",

resulting in the output (chr1,9,'GA','GC',2).  Without the ``-nonorm`` option the output is (chr1,10,'A','C',2)

Note that if the variants are represented as segments, the ``-seg`` option must be specified such that :ref:`VARMERGE` modifies both the start and the end coordinate, in case the reference sequence representation of a row is changed.


.. _simpleVariationCaller:

Creating a Simple Variation Caller
==================================
In the following example, we are using several of the sequence read commands from :ref:`the previous chapter<sequenceReads>` along with :ref:`VARMERGE` to create a simple variation caller.

.. code-block:: gor

   gor -p chr2:10000- sampleA.bam | TOP 1000000 | WHERE MapQ >= 15
   | BAMFLAG | VARIANTS -gc qStrand | VARMERGE Ref Alt
   | GROUP 1 -gc ref,alt,qStrand -count
   | PIVOT qStrand -v ’0’,’1’ -gc Ref,Alt -e 0
   | RENAME 1_allCount F_allCount
   | RENAME 0_allCount R_allCount
   | WHERE F_allcount > 0 and R_allCount > 0
   | CALC varReadCount F_allcount + R_allcount
   | JOIN -snpsnp -r -maxseg 250
      <(gor sampleA.bam | WHERE MapQ >= 15 | PILEUP -depth)
   | SELECT #1,#2,Ref,Alt,varReadCount,Depth

The above query calls variants from one milion sequence reads on chromosome 2 where the read is supported by both forward and reverse aligned sequence reads with a mapping quality (``MapQ``) larger than 15 on a :term:`Phred scale`. Next, the :ref:`BAMFLAG` command expands the Flag "bit-mask BAM column" into regular Boolean columns. For each sequence read, the :ref:`VARIANTS` command then outputs every delta from the reference genome in a VCF-like Ref,Alt form. This step with the :ref:`VARIANTS` command also returns the variants from overlapping read in genomic order due to its use of a sliding window sort.

Next, we use the :ref:`VARMERGE` command to right-normalise the variations in different rows into a coherent form. This is done because most aligners do not guarantee consistent representations of InDels for sequence reads in repeat regions. This may involve reordering the rows to ensure genomic order, since there can be changes to the Pos, Ref and Alt columns in this normalising step. A :ref:`GROUP` command is then used to count how many copies we have of each variant per strand type.

In the :ref:`PIVOT` step, we take the grouped data and pivot per genomic base position, and also per group formed by the values of the ``Ref`` and ``Alt`` columns, for each of the two possible ``qStrand`` values, i.e. 0 and 1. We then rename the columns from 1 and 0 to F and R to have the column names more meaningful and filter out any variants that do not have support from both the forward and reverse alignment.

Finally, we :ref:`JOIN` to the the BAM file in a nested query. This nested query is filtered once again on the mapping quality and a :ref:`PILEUP` is used to calculate the depth of the base read at each position. Note that the :ref:`JOIN` here has a maximum segment size set to 250 base-pairs, since the features are generated from sequence read segments, which are up to 250bp long.

.. _normalisingVariationData:

Normalising Variation Data
==========================
The VARJOIN query shown above that joined the patient sample data to the ``#VEP#`` table illustrated how a single variation can be represented in different ways. In the case of the first entry in the joined table from chromosome 21 above, the resulting variation is the same in the left and the right source (a deletion of a single cytosine), but in the case of the left source, the variation is "normalised" to the left, whereas the right source is "right-normalised".

The **VARNORM** command can be used to normalise sample data to the left or right using the ``-left`` and ``-right`` options for that command respectively.


