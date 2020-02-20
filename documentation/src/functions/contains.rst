.. _contains:

=====================
CONTAINS, CONTAINSALL
=====================

**CONTAINS** (or **CONTAINSALL**) is a Boolean expression that can be used to compare the contents of a string, which is in the form of a comma-separated list, to a :term:`literal list` and return true if *all of the items* in the literal list match. CONTAINS and CONTAINSALL can be used interchangeably. A related expression is :ref:`CONTAINSANY`, in which not all elements of the literal list need to match in the string.

**CONTAINS** is most often used as part of an IF statement in GOR.


Usage
=====

.. code-block:: gor

	contains(str,lit-list) : Boolean

Examples
========

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(contains('apple,banana','apple'),'true','false')

returns TRUE (all the elements in the literal list are contained in the string list, although not vice versa)

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(contains('apple,banana','apple','banana'),'true','false')

returns TRUE (all the elements in the literal list are contained in the string list, and vice versa)

.. code-block:: gor

   gorrow 1,1,1 | CALC test if(contains('apple,banana','apple','banana','orange'),'true','false')

returns FALSE (not all the elements in the literal list are contained in the string list)