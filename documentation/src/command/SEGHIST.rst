.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SEGHIST:

=======
SEGHIST
=======
The **SEGHIST** command turns a stream of annotations into a stream of non-overlapping segments.
The output is Chrom, bpStart, bpStop, and Count where count is the number of annotations in the input.

Usage
=====

.. code-block:: gor

	gor ... | seghist max

Examples
========

.. code-block:: gor

    gor #exons# | group 10000 -count | seghist  1000

    create #temp# = pgor #dbsnp# | group 1000 -count;
    gor [#temp#] | seghist 1000000

The above command returns a segments containing max number of annotation.