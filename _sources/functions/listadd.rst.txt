.. _listadd:

=======
LISTADD
=======

The **LISTADD** function adds an item to the end of a list.

The function accepts two parameters:

* source of the list in string format,
* the item to add.

An optional third parameter specifies the separator to use (by default, a comma is used).

The function returns the result in string format.

Usage
=====

.. code-block:: gor

	listadd(str, value[, sep]) : str

Examples
========

An example of using the **LISTADD** function with an explicitly stated list is shown below.

.. code-block:: gor

   gorrow 1,1,1 | CALC listresult LISTADD('a,b,c,d', 'e')

The result is the list 'a,b,c,d,e'.

If the list is not separated by commas, as in the following example, the separator must be included (in quotation marks - single or double) as a second parameter.

.. code-block:: gor

	gor example.vcf | CALC listresult LISTREVERSE('a:b:c:d', 'e', ':')

Here the result is the list 'a:b:c:d:e'.