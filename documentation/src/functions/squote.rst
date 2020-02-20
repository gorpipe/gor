.. _squote:

======
SQUOTE
======

**SQUOTE** is a string-conversion function that adds single quotes around a given string. This is especially
useful with the :ref:`LISTMAP` function, to reduce the need for escaping quotes.

Usage
=====

``SQUOTE(input) : output``

Example
=======
This example query will add single quotes around each element of the list.

.. code-block:: gor

	gorrow 1,1 | calc list 'a,b,c,d' | calc x listmap(list, 'squote(x)')