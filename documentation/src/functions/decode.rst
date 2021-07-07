.. _decode:

======
DECODE
======

The **DECODE** function decodes/maps the value of the first argument using key-value mapping pairs provided by the second argument.

The key-value mapping pairs are provided as a comma-separated list of key-value pairs.  The list of pairs can end with an optional default value:

``'key1,value1[,key2,value2]...[,default]'``

Usage
=====

.. code-block:: gor

   decode(string, string) : string

Example
=======


.. code-block:: gor

   gorrow 1,1 | calc chrom_decimal decode(chrom, 'chr1,1,chr2,2,0')

decodes the chromosome name for the chr1 and chr2 into a number, maps other chromosomes as 0.

