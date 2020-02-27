.. _listreverse:

===========
LISTREVERSE
===========

The **LISTREVERSE** function retrieves the contents of a list in reverse order. The list is assumed to be comma-separated, but an optional second parameter can specify a different separator. The list can be explicitly stated or it can be a reference to a column (either by name of the column or by its position in the input stream).

The function accepts two parameters:

* source of the list in string format,
* the separator (if the list is not comma-separated).

The function returns the result in string format.

Usage
=====

.. code-block:: gor

	listreverse(str,sep) : str

Examples
========

An example of using the **LISTREVERSE** function with an explicitly stated list is shown below. The calculated column "listresult" here will have a value of "d,c,b,a" for every output row.

.. code-block:: gor

   gorrow 1,1,1 | CALC listresult LISTREVERSE('a,b,c,d')

If the list is not separated by commas, as in the following example, the separator must be included (in quotation marks - single or double) as a second parameter.

.. code-block:: gor

   gor example.vcf | CALC listresult LISTREVERSE('a:b:c:d', ':')

