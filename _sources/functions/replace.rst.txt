.. _f_replace:

=======
REPLACE
=======

The **REPLACE** function replaces all occurrences of a substring in a source string with a replacement substring. It takes three parameters as input: the source string, the search substring, and the replacement substring.

Note that this is not the same as the :ref:`REPLACE` command, which can replace a whole column based on some condition.

Usage
=====

``REPLACE(string,string,string) : string``

Example
=======
The example query replaces instances of 'Apples' with 'Peaches' in the provided source string ('Apples,Bananas,Pears) resulting in a column (labelled 'demo') with a value of 'Peaches,Bananas,Pears'.

.. code-block:: gor

   gorrow chr1,1,1 | CALC demo replace('Apples,Bananas,Pears','Apples','Peaches')
