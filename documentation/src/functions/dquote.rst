.. _dquote:

======
DQUOTE
======

**DQUOTE** is a string-conversion function that adds double quotes around a given string. This is especially
useful with the :ref:`LISTMAP` function, to reduce the need for escaping quotes.

Usage
=====

``DQUOTE(input) : output``

Example
=======
This example query will add double quotes around each element of the list.

.. code-block:: gor

	gorrow 1,1 | calc list 'a,b,c,d' | calc x listmap(list, 'dquote(x)')