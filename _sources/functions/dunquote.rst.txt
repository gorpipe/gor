.. _dunquote:

========
DUNQUOTE
========

The **DUNQUOTE** function removes single quotes surrounding a given string. If the string does not start and end
with a single quote, it is returned unchanged.

Usage
=====

``DUNQUOTE(string) : string``

Example
=======
This example query removes the quotes from each item in the list.

.. code-block:: gor

	gorrow 1,1 | calc list '"a","b","c","d"' | calc x listmap(list, 'dunquote(x)')