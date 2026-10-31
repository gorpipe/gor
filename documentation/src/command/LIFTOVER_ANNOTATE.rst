.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _LIFTOVER_ANNOTATE:

=================
LIFTOVER_ANNOTATE
=================

The **LIFTOVER_ANNOTATE** command looks up where each input row lands in another genome build and appends the result as
extra columns. Unlike :ref:`LIFTOVER` it does not move the rows: every input row produces exactly one output row, in the
same place and in the same order, with its original coordinates, followed by a liftover status and the lifted-over
location. No row is dropped, merged or reordered.

Because the rows stay in place the output is still sorted by the input coordinates, no ``sort genome`` is needed and
the command is safe to use under ``pgor``. The lifted-over coordinates are ordinary columns, so you can filter on the
status, keep only the mapped rows, or relocate the rows yourself (see the notes below).

The mapping between the builds is read from a chain file, a GOR file with the header
``Chrom tStart tEnd qChrom qStart qEnd qStrand qScore`` (see `Chain format`_), typically generated from a UCSC chain
file. The chain can also be a nested query, ``<(gor ...)``.

The command must know the type of the data, and exactly one of ``-snp``, ``-var``, ``-seg`` and ``-seg1`` is required.

Usage
=====

.. code-block:: gor

   gor ... | LIFTOVER_ANNOTATE <chain> ( -snp | -var | -seg | -seg1 ) [<options>]

   nor ... | LIFTOVER_ANNOTATE <chain> ( -snp | -var ) -chrom <col> -pos <col> [<options>]

   nor ... | LIFTOVER_ANNOTATE <chain> ( -seg | -seg1 ) -chrom <col> -pos <col> -end <col> [<options>]

``<chain>`` is a chain file or a nested query ``<(gor ...)``.

Options
=======

Input type (exactly one is required):

+-----------------------+------------------------------------------------------------------------------------------+
| ``-snp``              | A single position. ``-pos`` is the 1-based position.                                     |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-var``              | A variant. The position is ``-pos``, and the REF and ALT columns are used to get the     |
|                       | length of the variant and to produce the lifted-over alleles.                            |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-seg``              | A segment in GOR coordinates, half-open: ``[start,end)`` with 0-based start. This is     |
|                       | the format of a normal GOR row where start is column 2 and end is column 3.              |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-seg1``             | A segment in 1-based inclusive coordinates: ``[start,end]``.                             |
+-----------------------+------------------------------------------------------------------------------------------+

Input columns:

+-----------------------+------------------------------------------------------------------------------------------+
| ``-chrom <col>``      | The chromosome column. Default is column 1 (``#1``) in gor. Required in nor.             |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-pos <col>``        | The position column (the start for ``-seg`` and ``-seg1``). Default is column 2 (``#2``) |
|                       | in gor. Required in nor.                                                                 |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-end <col>``        | The end column. Only valid with ``-seg`` and ``-seg1``. Default is column 3 (``#3``) in  |
|                       | gor. Required in nor. The input must have such a column.                                 |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-ref <col>``        | The reference allele column. Only valid with ``-var``. Default is the column named       |
|                       | ``REF`` or ``REFERENCE``. There is no fallback to column 3.                              |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-alt <col>``        | The alternate allele column. Only valid with ``-var``. Default is the column named       |
|                       | ``ALT``, ``CALL`` or ``ALLELE``. There is no fallback to column 4.                       |
+-----------------------+------------------------------------------------------------------------------------------+

``-var`` requires both a REF and an ALT column. Columns can be given by name or by number. Column names are matched
case-insensitively.

Output and behaviour:

+-----------------------+------------------------------------------------------------------------------------------+
| ``-prefix <p>``       | Prefix of the appended column names. Default is ``liftover_``. The prefix cannot         |
|                       | contain any of ``\ / * + - ' $ ; ,``.                                                    |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-passthrough <c>``  | Comma separated list of source contigs that are copied through unchanged, with the       |
|                       | status ``passthrough``, without using the chain. There is no default: without the        |
|                       | option no contig is passed through. ``-passthrough none`` means an empty list. Contigs   |
|                       | are matched exactly and are case-sensitive.                                              |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-mapped <c>``       | Comma separated list of the contigs that are accepted, both as source contigs and as     |
|                       | target contigs. A row on another source contig, or one that lifts to another contig,     |
|                       | gets the status ``unsupported_contig``. By default all contigs are accepted. A contig    |
|                       | cannot be in both ``-passthrough`` and ``-mapped``.                                      |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-overlap <policy>`` | What to do when blocks of the chain overlap on the source side. ``error`` (the default)  |
|                       | rejects such a chain with an error. ``best`` uses the overlapping block with the highest |
|                       | score, and if two such blocks have exactly the same score the status is ``ambiguous``.   |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-failon <s>``       | Comma separated list of statuses. The query fails on the first row that gets one of      |
|                       | them. The statuses are checked against the statuses of the input type (see               |
|                       | `Status values`_) when the query is parsed.                                              |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-tolerance <t>``    | Only valid with ``-seg`` and ``-seg1``. A segment whose lifted-over size is within       |
|                       | ``t`` (relative) of its source size is ``mapped_across_gap``, otherwise ``size_changed`` |
|                       | (see `Status values`_). A number in [0,1). Default is 0.05.                              |
+-----------------------+------------------------------------------------------------------------------------------+
| ``-diag``             | Only valid with ``-seg`` and ``-seg1``. Adds diagnostic columns with the sizes and the   |
|                       | lifted-over endpoints (see `Output columns`_).                                           |
+-----------------------+------------------------------------------------------------------------------------------+

If an appended column has the same name as a column of the input the query fails with an error. Use ``-prefix`` to
choose other names.

Output columns
==============

The columns below are appended to the input columns, in this order. The names are shown with the default prefix
``liftover_``. A row with a status that has no target location (for example ``unmapped``) gets an empty location: the
empty string in the target contig and strand columns (and in ``liftover_ref`` and ``liftover_alt`` for ``-var``) and
``0`` in the position columns (``liftover_pos``, or ``liftover_start`` and ``liftover_stop``). The other columns
(``liftover_status``, and ``liftover_chain_blocks`` and the ``-diag`` columns of ``-seg`` and ``-seg1``) always have
values.

``-snp``: ``liftover_status``, ``liftover_chrom``, ``liftover_pos`` (I, the 1-based target position),
``liftover_strand`` (S).

``-var``: ``liftover_status``, ``liftover_chrom``, ``liftover_pos`` (I, the 1-based target position of the start of
the variant), ``liftover_ref`` (S), ``liftover_alt`` (S), ``liftover_strand`` (S). When the target strand is ``-``
each allele is reverse complemented separately (the order of a comma separated list of alleles is kept). Symbolic
alleles (starting with ``<``, such as ``<DEL>``), ``*``, ``.`` and breakend alleles (containing ``[`` or ``]``) are left
as they are. The alleles are empty for rows without a target.

``-seg`` and ``-seg1``: ``liftover_status``, ``liftover_chrom``, ``liftover_start`` (I), ``liftover_stop`` (I),
``liftover_strand`` (S), ``liftover_chain_blocks`` (I, the number of chain blocks that overlap the segment, including
blocks that lose under ``-overlap best``). The start and stop are in the same convention as the input: ``-seg``
gives GOR half-open coordinates and ``-seg1`` gives 1-based inclusive coordinates.

The ``liftover_strand`` column is ``+`` or ``-`` for the strand of the target relative to the source, and empty when
there is no target, and for ``passthrough`` rows.

With ``-diag`` the following columns are added after ``liftover_chain_blocks``:

+----------------------------+------+-------------------------------------------------------------------------------+
| Column                     | Type | Description                                                                   |
+============================+======+===============================================================================+
| ``liftover_source_size``   | I    | The size of the source segment, last base minus first base, so a 100 base     |
|                            |      | segment gives 99.                                                             |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_target_size``   | I    | The distance between the two lifted-over endpoints when both endpoints lift   |
|                            |      | to the same target contig, otherwise 0. A segment that is contained in one    |
|                            |      | chain block has the same target size as source size.                          |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_size_ratio``    | D    | ``target_size / source_size``: 1.0 for a contained segment, 0.0 when the      |
|                            |      | endpoints do not lift to one contig. Printed as a Java ``double``.            |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_start_chrom``   | S    | The contig of the lifted-over first base of the segment. Empty when the       |
|                            |      | endpoint does not lift.                                                       |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_start_pos``     | I    | The 1-based position of the lifted-over first base. 0 when it does not lift.  |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_start_strand``  | S    | The strand of the lift of the first base. Empty when it does not lift.        |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_stop_chrom``    | S    | The same as ``liftover_start_chrom``, for the last base of the segment.       |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_stop_pos``      | I    | The same as ``liftover_start_pos``, for the last base.                        |
+----------------------------+------+-------------------------------------------------------------------------------+
| ``liftover_stop_strand``   | S    | The same as ``liftover_start_strand``, for the last base.                     |
+----------------------------+------+-------------------------------------------------------------------------------+

A segment that one block contains reports its diagnostics through that block: the target size equals the source size,
the size ratio is 1.0 and the endpoints are lifted through the block, whether the status is ``mapped`` or
``unsupported_contig`` (the block lifts to a contig that is not in ``-mapped``). A ``passthrough`` row has the target size
equal to the source size, a ratio of 1.0 and empty endpoints (``''``, ``0``, ``''``). A junction (see `Notes`_) has
empty endpoints. Endpoints that do not lift are also ``''``, ``0``, ``''``.

The diagnostic columns are useful to see why a segment did not map, for example the size ratio of a
``size_changed`` row, or which end of an ``inverted`` row went where.

Status values
=============

Every row gets exactly one status. The statuses are tested in the order below, and the first one that applies is
used.

``-snp`` and ``-var``:

1. ``passthrough``: the source contig is in ``-passthrough``. The row is copied through unchanged (chrom and pos are the
   input values, the strand is empty).
2. ``unsupported_contig``: the source contig is not in ``-mapped``.
3. ``ambiguous``: with ``-overlap best``, two best blocks have exactly the same score.
4. ``unmapped``: no block contains the position. For ``-var`` the block must contain the whole of REF. An empty REF is
   a junction (see `Notes`_).
5. ``unsupported_contig``: the block lifts to a contig that is not in ``-mapped``.
6. ``mapped``.

``-seg`` and ``-seg1``:

1. ``passthrough``: the source contig is in ``-passthrough``. The row is copied through unchanged.
2. ``unsupported_contig``: the source contig is not in ``-mapped``.
3. If one block (the best block under ``-overlap best``) contains the entire segment:

   * ``ambiguous``: two best blocks have exactly the same score.
   * ``unsupported_contig``: the block lifts to a contig that is not in ``-mapped``.
   * ``mapped``.

   This is the case for almost every segment, and the endpoint statuses below are then not considered.
4. Otherwise no block contains the segment as a whole, and the first and last base are lifted on their own:

   * ``unmapped``: a segment that is a single base or a junction.
   * ``ambiguous``: the first or the last base is ambiguous, even when the other base does not lift.
   * ``unmapped``: neither the first nor the last base lifts.
   * ``start_unmapped``: only the first base does not lift.
   * ``stop_unmapped``: only the last base does not lift.
   * ``split_contig``: the two bases lift to different contigs.
   * ``unsupported_contig``: both lift to a contig that is not in ``-mapped``.
   * ``inverted``: the two bases lift to opposite strands, or to the wrong order on the target.
   * ``mapped_across_gap``: both bases lift to the same contig and strand, in the same order, and the lifted size is
     within the tolerance of the source size, that is ``1 - tolerance < size_ratio < 1 + tolerance``. The segment spans
     a gap between blocks that is small.
   * ``size_changed``: as above, but the size ratio is outside the tolerance.

Rows with a status that has a target (``mapped``, ``mapped_across_gap``, ``size_changed`` and ``passthrough``) have the
target location filled in. For the other statuses the location is empty (``''``, ``0``, ``0``, ``''``).

Coordinate conventions
======================

The input is converted to 0-based half-open coordinates before it is lifted, so a position ``p`` is ``[p-1,p)``, a
variant is ``[pos-1, pos-1+len(REF))`` and a segment is ``[start,end)`` for ``-seg`` and ``[start-1,end)`` for
``-seg1``. This is the one place where ``-seg`` and ``-seg1`` differ: the first is the GOR convention used for the
start and end columns of gor rows, the second a 1-based inclusive range as used by VCF positions, BED-like tools that
report ``start+1`` and so on. The output uses the same convention as the input in each case, so with ``-seg1`` the
lifted-over start is also 1-based and inclusive.

A segment is ``mapped`` only when a single chain block contains all of it. Adjacent blocks that together cover the
segment can give ``mapped_across_gap`` or ``size_changed`` (see the example below), and also ``inverted`` or
``split_contig`` when the blocks lead to opposite strands or to different contigs.

Chain format
------------

The chain is a GOR file (or nested query) with the columns ``Chrom``, ``tStart``, ``tEnd``, ``qChrom``, ``qStart``,
``qEnd``, ``qStrand`` and ``qScore``. The first two columns are the source contig and the 0-based start, so the file
must be sorted as a GOR file. Column names are matched case-insensitively.

* The blocks are ungapped, 0-based half-open intervals: ``tEnd - tStart`` must equal ``qEnd - qStart``.
* The target interval is always given on the forward strand of ``qChrom``, and ``qStrand`` is ``+`` or ``-``, where
  ``-`` means that the source is reverse complemented relative to the target.
* ``qScore`` is a number (it can be larger than an int). It is used to choose between overlapping blocks with
  ``-overlap best``.
* The chain is checked when it is loaded. A file chain is loaded one source contig at a time, the first time a row on
  that contig needs it, so an error in a block on a contig that no row uses is never reported (a nested chain is read
  and checked as a whole). A chain that is not sorted by ``tStart``, has a block with ``tEnd <= tStart``,
  a gapped block, a ``qStrand`` other than ``+`` or ``-``, an invalid score, or (with the default ``-overlap error``)
  overlapping blocks is rejected with an error that starts with ``Liftover chain <path>``.

Examples
========

The segment examples use this chain. It has two adjacent blocks on chr1, a block on the reverse strand, a block that
moves to another contig, two overlapping blocks (``chr1 800-900`` and ``chr1 850-950``) and a block on chrM. The chain
has overlapping blocks, so ``-overlap best`` is needed to load it.

.. code-block:: text

   Chrom  tStart  tEnd  qChrom  qStart  qEnd  qStrand  qScore
   chr1   100     200   chr1    1100    1200  +        1000
   chr1   200     300   chr1    1200    1300  +        1000
   chr1   400     500   chr1    5000    5100  -        900
   chr1   600     700   chr2    2100    2200  +        800
   chr1   800     900   chr1    8800    8900  +        500
   chr1   850     950   chr1    9850    9950  +        500
   chrM   300     3100  chrM    298     3098  +        100

Segments in GOR coordinates, with ``-seg``:

.. code-block:: gor

   gor segments.gor | LIFTOVER_ANNOTATE chain.gor -seg -overlap best

Input rows ``chr1 120 180``, ``chr1 150 250`` and ``chr1 250 450`` (named g01, g02 and g03) give::

   Chrom   Start   End     Name    liftover_status     liftover_chrom  liftover_start  liftover_stop   liftover_strand liftover_chain_blocks
   chr1    120     180     g01     mapped              chr1            1120            1180            +               1
   chr1    150     250     g02     mapped_across_gap   chr1            1150            1250            +               2
   chr1    250     450     g03     inverted                            0               0                               2

g01 lies inside the first block. g02 crosses from the first block into the second, so it is not contained in one block,
but both ends lift to the same contig and strand and the size is unchanged. g03 starts on the forward strand and ends
inside the reverse strand block, so it is ``inverted`` and has no target location.

Segments in 1-based inclusive coordinates, with ``-seg1``. The same three segments written as ``121-180``,
``151-250`` and ``251-450``:

.. code-block:: gor

   gor segments1.gor | LIFTOVER_ANNOTATE chain.gor -seg1 -overlap best

gives::

   Chrom   Start   End     Name    liftover_status     liftover_chrom  liftover_start  liftover_stop   liftover_strand liftover_chain_blocks
   chr1    121     180     g01     mapped              chr1            1121            1180            +               1
   chr1    151     250     g02     mapped_across_gap   chr1            1151            1250            +               2
   chr1    251     450     g03     inverted                            0               0                               2

The lifted start is 1-based here as well (1121 where ``-seg`` gave 1120).

A segment spanning a gap in the source that is closed in the target, with ``-diag``. The chain has the blocks
``chr1 100-200 -> 1000-1100`` and ``chr1 210-310 -> 1100-1200``, there is a gap of 10 bases between the blocks in the
source, and none in the target::

   gor ... | LIFTOVER_ANNOTATE chain.gor -seg -diag

   Chrom  Start  End  Name  liftover_status  liftover_chrom  liftover_start  liftover_stop  liftover_strand  liftover_chain_blocks  liftover_source_size  liftover_target_size  liftover_size_ratio  liftover_start_chrom  liftover_start_pos  liftover_start_strand  liftover_stop_chrom  liftover_stop_pos  liftover_stop_strand
   chr1   120    180    in    mapped           chr1            1020            1080           +                1                      59                    59                    1.0                  chr1                  1021                +                      chr1                 1080               +
   chr1   150    260    gap   size_changed     chr1            1050            1150           +                2                      109                   99                    0.908256880733945   chr1                  1051                +                      chr1                 1150               +

The gap row has a size ratio of 99/109, outside the default tolerance of 0.05, so it is ``size_changed``. With
``-tolerance 0.1`` it is ``mapped_across_gap``.

A custom prefix for the new columns, with ``-snp``:

.. code-block:: gor

   gor snps.gor | LIFTOVER_ANNOTATE chain.gor -snp -prefix hg38_

adds the columns ``hg38_status``, ``hg38_chrom``, ``hg38_pos`` and ``hg38_strand``. With ``-seg -prefix lifted_ -diag``
the names are ``lifted_status``, ``lifted_chrom``, ``lifted_start``, ``lifted_stop``, ``lifted_strand``,
``lifted_chain_blocks``, ``lifted_source_size``, ``lifted_target_size``, ``lifted_size_ratio``,
``lifted_start_chrom``, ``lifted_start_pos``, ``lifted_start_strand``, ``lifted_stop_chrom``, ``lifted_stop_pos`` and
``lifted_stop_strand``.

Keep the rows that mapped (and the chrM rows that were passed through), and, in the second query, fail on rows that
cannot be lifted:

.. code-block:: gor

   gor variants.gor | LIFTOVER_ANNOTATE hg19tohg38.gor -var -passthrough chrM | WHERE liftover_status = 'mapped' or liftover_status = 'passthrough'

   gor variants.gor | LIFTOVER_ANNOTATE hg19tohg38.gor -var -failon unmapped,ambiguous

Notes
=====

* The rows are not relocated: the output has the same coordinates and order as the input, so the command is safe to
  use with ``pgor``. If you want the rows at their lifted-over positions, move the lifted-over columns into the first
  columns yourself and use ``SORT GENOME`` after that, because the relocated rows are no longer in order.
* The chain can be a nested query, ``<(gor ...)``, for example ``LIFTOVER_ANNOTATE <(gor chain.gor | where qScore > 100) -snp``.
  A nested chain is read once per pipe step and is validated like a file, but it is not cached across queries. A
  chain file is cached for the session, so repeated queries on the same file do not read it again. A nested ``nor``
  query is not accepted as a chain.
* To be cached, a chain file must have a unique source id in its file system or driver. A chain file without one is
  rejected with an error.
* A zero-length interval is a junction between two bases: ``-seg`` with ``end == start``, ``-seg1`` with
  ``end == start - 1``, and ``-var`` with an empty REF. A junction maps only when it lies strictly inside a chain block,
  that is not at the edge of the block. The output of a junction is again a zero-length interval (``-seg``: start =
  stop, ``-seg1``: stop = start - 1). An interval with ``end < start`` (``end < start - 1`` for ``-seg1``) is an error.
* The status of a segment comes from containment first. The endpoint statuses (``start_unmapped``, ``split_contig``,
  ``inverted`` and so on) are only used when no single block contains the whole segment.
* Errors in the data start with ``LIFTOVER_ANNOTATE:`` and contain the contig and position of the row.
* In nor, the chromosome, position and, for segments, end columns must be given with ``-chrom``, ``-pos`` and ``-end``.
