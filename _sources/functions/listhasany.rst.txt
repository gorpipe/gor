.. _listhasany:

==========
LISTHASANY
==========

**LISTHASANY** is a Boolean expression that can be used to compare the contents of a string, which is in the form of a comma-separated list, to a :term:`literal list` and return true if *any of the items* in the literal list match. CONTAINSANY and LISTHASANY can be used interchangeably. A related expression is :ref:`CONTAINS`, in which all elements of the literal list need to match in the string.

**LISTHASANY** is most often used as part of an IF statement in GOR.


Usage
=====

.. code-block:: gor

	listhasany(str,lit-list) : Boolean

Examples
========

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(listhasany('apple,banana','apple'),'true','false')

returns TRUE (all the elements in the literal list are contained in the string list, although not vice versa)

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(listhasany('apple,banana','apple','banana'),'true','false')

returns TRUE (all the elements in the literal list are contained in the string list, and vice versa)

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(listhasany('apple,banana','orange','banana'),'true','false')

returns TRUE (there is at least one element in common between the string list and the literal list)

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(listhasany('apple,banana','orange','pineapple'),'true','false')

returns FALSE (there are no elements in common between the string list and the literal list)