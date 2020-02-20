.. _listhascount:

============
LISTHASCOUNT
============

**LISTHASCOUNT** counts the number of items from a literal list that appear in the defined string. It does not count the instances of the items, but only tallies the number of positive matches from the literal list that are found.


Usage
=====

.. code-block:: gor

	listhascount(str,lit-list) : int

Examples
========

The following example shows a list of fruits in a string, matches the items ('orange' and 'banana') to the string. Note that the result is ``2`` and not ``3`` even thought 'banana' is found in the list twice. Only a single match is counted per item in the literal list.

.. code-block:: gor

   gorrow 1,1,1 | CALC test listhascount('apple,banana,banana,orange,pineapple','orange','banana')

If an item is included twice in the literal list, it will be counted twice. The result of the query below will be ``3`` because 'banana' appears twice in the literal list.

.. code-block:: gor

   gorrow 1,1,1 | CALC test listhascount('apple,banana,banana,orange,pineapple','orange','banana','banana')