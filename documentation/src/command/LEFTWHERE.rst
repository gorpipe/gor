.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _LEFTWHERE:

=========
LEFTWHERE
=========
The **LEFTWHERE** command is used to supply additional join condition into a left-join operation. Based on the grouping of all the columns in the left-relation (as specified by the last column in the left-source, the LEFTWHERE command does alway return a single instance of each row in the left-source, even if the conditional expression is always false.  Such row are the replaced with empty values, for each of the columns in the right-source. This is equivalent to the standard behaviour of a general left-join relational operation.

Usage
=====

.. code-block:: gor

	gor ... | join -snpsnp -l rightfile.gor | leftwhere lastcolinleftsource cond-expression


Examples
========
As an example, this can be used to lookup genotype frequencies:

.. code-block:: gor

    gor genotypes.gor | SELECT 1,2,ref,allele | JOIN IceFreq.gor -f 20 -snpsnp -l
    | LEFTWHERE allele (refx = '' or gtshare(pos,ref,allele,posx,refx,allelex)>0)
    | REPLACE AfIceFreq IF(AfIceFreq='',IF(alt=ref,1.0,0.0),AfIceFreq) | HIDE refx,allelex

The above query, does a lookup in IceFreq based on a fuzzy join (allowing 20bp freedom in the specification of equivalent genotypes, with respect to the reference).  If no loci match is found, the column refx will be empty '' as well as the AfIceFreq column (due to the left-join) and interpreted either as 1.0 or 0.0, depending on whether the reference allele is begin looked-up or alternative. Likewise, in the case there is a match in loci, BUT no match based on gtshare, the LEFTWHERE command will return refx and AfIceFreq columns with empty values ('').  Note that above query is more easily done with VARJOIN,
however the LEFTWHERE command is more versatile.

See the :ref:`WHERE` command for more details on conditional expressions.
