.. _sequenceReads:

==============
Sequence Reads
==============
The sequence data that is imported into the WuXi NextCODE system is part of an analysis pipeline that begins with the DNA samples taken from a subject, which are sequenced using next-generation sequencing (NGS). The overlapping sequence reads are then aligned to the reference genome and compiled into a coverage file. Individual sequence reads can be up to 250 base-pairs long.

Finally, this coverage file is compared to known variants that have been annotated with regards to their effects on the individual (i.e. their genetic traits or the likelihood of developing certain diseases). The WXNC tools are developed specifically to work with this last step in the pipeline.

In the following chapter, we will introduce some commands for working with sequence read data in the GOR query language, in particular sequence read data that is stored in BAM files.

Viewing Sequence Reads in Sequence Miner
========================================
Before we show each of the commands that can be performed with sequence read data, we will first show how to open the data in Sequence Miner and describe how the data can be interpreted using the tool. In the following video example, we show how to open the variation and BAM files for participants in a study and view the BAM tracks in the genome browser.

.. raw:: html

   <iframe width="560" height="315" src="https://www.youtube.com/embed/uRps0kZdWOY" frameborder="0" allowfullscreen></iframe>

Pileup
======
The **PILEUP** command describes the base-pair formation at each chromosomal position. It summarizes the base calls of aligned sequence reads to a reference sequence.

Bamflag
=======
The ``FLAG`` column in the sequence reads indicates settings for a number of different parameters. The **BAMFLAG** command expands the FLAG bitmap column into multiple Boolean (1/0) columns, which are easier to read. The table below has a list of the different parameters and their corresponding bit in the flag value.

.. list-table:: The Flag Column as expanded by BAMFLAG
   :widths: 3  6 5  6  15
   :header-rows: 1

   * - #
     - Binary
     - Decimal
     - Hexadecimal
     - Description
   * - 1
     - 1
     - 1
     - 0x1
     - Read paired
   * - 2
     - 10
     - 2
     - 0x2
     - Read mapped in proper pair
   * - 3
     - 100
     - 4
     - 0x4
     - Read unmapped
   * - 4
     - 1000
     - 8
     - 0x8
     - Mate unmapped
   * - 5
     - 10000
     - 16
     - 0x10
     - Read reverse strand
   * - 6
     - 100000
     - 32
     - 0x20
     - Mate reverse strand
   * - 7
     - 1000000
     - 64
     - 0x40
     - First in pair
   * - 8
     - 10000000
     - 128
     - 0x80
     - Second in pair
   * - 9
     - 100000000
     - 256
     - 0x100
     - Not primary alignment
   * - 10
     - 1000000000
     - 512
     - 0x200
     - Read fails platform/vendor quality checks
   * - 11
     - 10000000000
     - 1024
     - 0x400
     - Read is PCR or optical duplicate
   * - 12
     - 100000000000
     - 2048
     - 0x800
     - Supplementary alignment

Cigarsegs
=========
The :ref:`CIGARSEGS` command takes sequence reads from a BAM-like stream and splits them into multiple reads based on the ``CIGAR`` column in the stream.

Bases
=====
The :ref:`BASES` command splits the ``SEQ`` column in the BAM file into the individual bases, showing the relative position within the read and the :term:`base-quality`.

Variants
========
The **VARIANTS** command returns the variants found in sequence reads and their associated quality. The variant quality is simply the :term:`base-quality` of the first position in the :term:`variants`. The VARIANTS command uses sliding window sort so that variants from overlapping reads are returned in genomic order.

Liftover
========
The **LIFTOVER** command is used to convert GOR data from one reference genome build to another, for example from hg19 to hg38.
