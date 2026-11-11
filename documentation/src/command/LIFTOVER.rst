.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _LIFTOVER:

========
LIFTOVER
========

The **LIFTOVER** command is used to convert GOR data from one reference genome build to another.  It provides similar functionality as the LiftOver functionality at UCSC, however, executes much faster.  The output of a LIFTOVER command is therefore in different coordinates than the input and therefore it is for instance meaningless to join such data with the source file or other sources in the original genome build.

The mapping between the genome builds for the LIFTOVER command is stored in GOR files, e.g. config/liftover/hg19tohg38.gor, which are typically generated from chain files from UCSC.  The chain must be sorted genomically (by contig, then ``tStart``) with each contig in one run.  A chain file is validated one contig at a time, when a row on that contig first needs it, so a contig that is out of order or an error in a block on a contig that no row uses is not reported.  A chain given as a nested query is read and validated as a whole.  The full chain format is described under :ref:`LIFTOVER_ANNOTATE`.

The LIFTOVER command must know the nature of the data, e.g. variants, BAM sequence reads or segments, in order to reverse complement sequence columns where appropriate.  Note, that the LIFTOVER command must re-order the entire output and is therefore a blocking operation similar to SORT genome.

.. versionchanged:: 5.17.0

   * Every mode (``-snp``, ``-var``, ``-seg``, ``-bam``, ``-all``, nested-query chains) runs natively, on the same engine as :ref:`LIFTOVER_ANNOTATE`, and gives the same lift as LIFTOVER_ANNOTATE with ``-overlap best`` for the same input.
   * Identical consecutive unmapped rows are no longer collapsed.
   * Without ``-all``, equal-score overlaps give one row with status ``ambiguous`` instead of duplicate ``mapped`` rows; ``-all`` still lists every containing block.
   * ``-var`` reverse-complements each allele separately, keeping multi-allelic order and symbolic alleles.
   * ``-bam`` treats reads as 1-based inclusive: ``-`` strand reads move by one base, a read starting on a block's first base no longer maps through that block, and MPOS is lifted rather than shifted (0 when the mate lands elsewhere, when MRNM is ``*`` or names another contig, or when the read does not map). An MPOS that is not an integer greater than 0 is not a mate position and is kept as it is.
   * A zero-length interval maps only strictly inside a block (unchanged). An invalid interval is now an error: ``end < start`` for ``-seg``, and ``End < Pos - 1`` for ``-bam`` (a junction, ``End == Pos - 1``, is valid).
   * Without ``-all``, a ``-seg`` or ``-bam`` row that no block contains, but whose first or last base lies in an exact score tie, is now ``ambiguous`` instead of ``unmapped``. This happens only on overlapping chains.
   * A 1-base ``-bam`` read on a block's last base (``Pos == End == tEnd``) now maps through that block.
   * A position in a gap of equal length on both sides between two consecutive blocks of one chain is lifted by its
     offset from the block before it, instead of being ``unmapped`` (see the Equal gaps section of
     :ref:`LIFTOVER_ANNOTATE`).
   * Input columns named ``liftover_*`` are no longer renamed.
   * An output-name collision is a ``GorParsingException`` naming the column, not a ``GorDataException`` from RENAME.
   * The chain is validated on load (sorted, ungapped, ``tEnd > tStart``, ``qStrand`` ``+`` or ``-``), and it needs a ``qEnd`` column.
   * A chain with no blocks is an error (it used to leave every row unmapped).
   * ``-ref`` sets the span used for overlap as well as the length.
   * The previous implementation is available, for comparison only, as ``LIFTOVER_LEGACY`` with the same arguments. It will be removed in a later release.

.. versionchanged:: 5.17.0

   ``-mapped``, ``-passthrough``, ``-failon`` and ``-tolerance``, with the same meaning, parsing and error messages as in
   :ref:`LIFTOVER_ANNOTATE`. With any of them the status column reports the full set of liftover statuses (see
   `Status values`_). Calls without them are unchanged.

Usage
=====

.. code-block:: gor

	gor ... | liftover config/liftover/hg18tohg19.gor [ -snp | -seg | -var | -bam ] [ -all ]
	    [ -build <build> ] [ -mapped <contigs> ] [ -passthrough <contigs> | none ] [ -failon <statuses> ]
	    [ -tolerance <t> ]

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
| ``-build value`` | Source build prefix (default hgold)                                     |
+------------------+-------------------------------------------------------------------------+
| ``-all``         | Include all mappings, not just the one with best score (default).       |
|                  | Cannot be combined with ``-mapped``, ``-passthrough``, ``-failon`` or   |
|                  | ``-tolerance``.                                                         |
+------------------+-------------------------------------------------------------------------+

Contig policy and failures (as in :ref:`LIFTOVER_ANNOTATE`):

+-----------------------+------------------------------------------------------------------------------------------+
| ``-passthrough <c>``  | Comma separated list of source contigs that are copied through unchanged, with the       |
|                       | status ``passthrough``, without using the chain: the row keeps its location (and MPOS    |
|                       | for ``-bam``) and the strand is empty. ``-passthrough none`` means an empty list.        |
|                       | Contigs are matched exactly and are case-sensitive.                                      |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-mapped <c>``       | Comma separated list of the contigs that are accepted, both as source contigs and as     |
|                       | target contigs. A row on another source contig, or one that lifts to another contig,     |
|                       | gets the status ``unsupported_contig``. By default all contigs are accepted. A contig    |
|                       | cannot be in both ``-passthrough`` and ``-mapped``.                                      |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-failon <s>``       | Comma separated list of statuses. The query fails on the first row that gets one of      |
|                       | them, with ``LIFTOVER: status '<s>' at <chrom>:<pos>``. The statuses are checked         |
|                       | against the statuses of the input type when the query is parsed: ``-snp`` and ``-var``   |
|                       | have the point statuses, ``-seg`` and ``-bam`` the segment statuses. The statuses are    |
|                       | the full ones (see `Status values`_), so with ``-seg`` and ``-bam`` ``-failon unmapped`` |
|                       | does not fail on ``start_unmapped``, ``stop_unmapped``, ``split_contig``, ``inverted``,  |
|                       | ``mapped_across_gap`` or ``size_changed`` rows, which a call without these options       |
|                       | reports as ``unmapped``. List every status that should fail.                             |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-tolerance <t>``    | Only valid with ``-seg`` and ``-bam``. A segment that no single block contains, but      |
|                       | whose ends lift to the same contig and strand, is ``mapped_across_gap`` when its lifted  |
|                       | size is within ``t`` (relative) of its source size, otherwise ``size_changed``. A number |
|                       | in [0,1). Default is 0.05.                                                               |
+-----------------------+------------------------------------------------------------------------------------------+

Each of these options may be given once. Unlike LIFTOVER_ANNOTATE, LIFTOVER has no ``-prefix``: the names of the
added columns are set by ``-build``.

Output columns
==============

The output row is the lifted location (``Chrom``, ``Pos`` and, for ``-seg`` and ``-bam``, the end column), the
remaining input columns, the source location as ``<build>_<column>`` (for example ``hgold_Chrom`` and ``hgold_Pos``),
``<build>_qStrand`` (``+`` or ``-``, empty when the row is not lifted) and ``<build>_liftoverStatus``, and with
``-all`` ``<build>_score``. The rows are sorted on the lifted location.

A row that is not lifted is placed at (source contig, 0), and for ``-seg`` and ``-bam`` with end 1, so it sorts first
on its source contig.

Status values
=============

Without ``-mapped``, ``-passthrough`` and ``-failon`` the status is ``mapped``, ``ambiguous`` (two best blocks have
exactly the same score) or ``unmapped`` (everything else). Only ``mapped`` rows are moved.

With any of ``-mapped``, ``-passthrough``, ``-failon`` or ``-tolerance`` (``-passthrough none`` is enough) the status
is the one :ref:`LIFTOVER_ANNOTATE` reports with ``-overlap best`` and the same ``-tolerance``, see its status values:

* ``-snp`` and ``-var``: ``passthrough``, ``unsupported_contig``, ``ambiguous``, ``unmapped``, ``mapped``.
* ``-seg`` and ``-bam``: ``passthrough``, ``unsupported_contig``, ``ambiguous``, ``unmapped``, ``start_unmapped``,
  ``stop_unmapped``, ``split_contig``, ``inverted``, ``mapped``, ``mapped_across_gap``, ``size_changed``.

``mapped`` rows, and for ``-seg`` also ``mapped_across_gap`` and ``size_changed`` rows, are moved to their lifted
location. ``passthrough`` rows keep their location. Rows with any other status are placed at (source contig, 0) as
above. This includes ``-bam`` reads that are ``mapped_across_gap`` or ``size_changed``: their SEQ, QUAL and CIGAR
would not fit the lifted span.

Examples
========
Lift variants from hg19 to hg38, keep chrM as it is, and fail if a variant does not lift:

.. code-block:: gor

   gor variants.gor | LIFTOVER hg19tohg38.gor -var -build hg19 -mapped chr1,chr2,chrX -passthrough chrM
      -failon unmapped,ambiguous

Rows on chr1, chr2 and chrX are lifted (a row that lifts to another contig is ``unsupported_contig``), chrM rows are
returned unchanged with the status ``passthrough``, and rows on any other contig are ``unsupported_contig``.

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
