.. _vcfformattag:

============
vcfformattag
============

The **vcfformattag** function gets a value from a vcf file.

The function takes three parameters in string format:

* name of the format column that contains the format of the data column,
* the name of the data column that contains data for a given PN, the column name is the same as the pn,
* the tag to get value for.

The function returns the result in string format.

Usage
=====

.. code-block:: gor

	vcfformattag(str,str,str) : str

Examples
========

For the following example vcf excerpt:

.. code-block:: bash

   #CHROM POS    ID        REF   ALT  QUAL  FILTER  INFO                     FORMAT       NA00001         NA00002         NA00003
   20     14370  rs6054257 G     A    29    PASS    NS=3;DP=14;AF=0.5;DB;H2  GT:GQ:DP:HQ  0|0:48:1:51,51  1|0:48:8:51,51  1/1:43:5:.,.

* ``vcfformattag(format, NA0002, ´HQ´)`` would return 51,51
* ``vcfformattag(format, NA0003, ´DP´)`` would return 5