.. _listsortasc:

===========
LISTSORTASC
===========

The **LISTSORTASC** function sorts a list in ascending alphabetical order. The list is assumed to be comma-separated, but an optional second parameter can specify a different separator. The list can be explicitly stated or it can be a reference to a column (either by name of the column or by its position in the input stream).

The function accepts two parameters:

* source of the list in string format,
* the separator (if the list is not comma-separated).

The function returns the result in string format.

Usage
=====

.. code-block:: gor

	listsortasc(str,sep) : str

Examples
========

An example of using the **LISTSORTASC** function with an explicitly stated list is shown below. The calculated column "listresult" here will have a value of "b,c,d" for every output row.

.. code-block:: gor

   gor example.vcf | CALC listresult listsortascl('a,d,c,b')

If the list is not separated by commas, as in the following example, the separator must be included (in quotation marks - single or double) as a second parameter.

.. code-block:: gor

   gor example.vcf | CALC listresult listsortasc('a:b:c:d', ':')

In the following example vcf excerpt, we can retrieve the first item in the FORMAT column by referring to that column by name or by its position in the stream. Note that it is better practice to refer to the name of the column rather than the position, since the positions can get switched around when working in queries and referring to columns by name makes the query more understandable to the reader:

.. code-block:: bash

   #CHROM POS    ID           REF   ALT  QUAL   FILTER   INFO                     FORMAT           BCLOIDZ
   chr1   10180  rs201694901  T     C    16.86  LowQual  AC=1;AF=0.500;AN=2;...   GT:AD:DP:GQ:PL   0/1:129,12:142:45:45,0,1082

.. code-block:: gor

   gor example.vcf | CALC listresult listtail(FORMAT, ':')

The example here would return the last four items (all but the first item) in the format list, or "AD:DP:GQ:PL".