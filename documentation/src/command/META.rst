.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. META:

====
META
====

**META** is a :term:`source command<source commands>` that displays all meta information associated with a given input source.

To use META, you specify the input source to explore and the META input source will return name and value of all meta properties associated with the given input source.

Usage
=====

.. code-block:: gor

   META <input_source>

Examples
========
The following example creates 10 rows starting from 10 with increment of 10.

.. code-block:: gor

   META samples.bam

Output::

#name	value
source.name	<path>/samples.bam
source.path	<path>/samples.bam
source.data.type	BAM
source.type	FILE
source.protocols
source.remote	false
source.supported	true
source.modified	1673530583578
source.id	3F1D72574FE3B8DA60FDFBA7E0BCDFE0
file.path	<path>/BVL_FATHER_SLC52A2.bam
file.name	BVL_FATHER_SLC52A2.bam
file.type	BAM
file.suffix	.bam
file.size	171379
file.modified	1673530583578
file.modified.utc	Jan 12, 2023, 1:36:23 PM
file.id	3F1D72574FE3B8DA60FDFBA7E0BCDFE0
file.supports.index	true
file.index	<path>>/samples.bam.bai
file.supports.reference	false
file.reference
