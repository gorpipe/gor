.. _listfilter:

==========
LISTFILTER
==========

The **LISTFILTER** function filters a list using the expression provided in second argument. Elements in the list are represented in the expression as ``x``, and the index value is represented as ``i``.

The expression in the second argument must be enclosed in quotation marks, which means that if the elements are compared to strings, the quotations around those quotation marks must be escaped with a backslash (or if single quotations are used for the expression, then double quotation marks can be used for the string.

All mathematical operators may be used in the expression, but these are only meaningful if the elements are treated as numbers. Elements in a list are treated as strings unless they are specifically cast into integers in the expression (see below).

Optional list separator arugment.

Usage
=====

``LISTFILTER(string,expression[,sep]) : string``

Example
=======
The query shown below will result in a filtered list of 'a,b,d' because the filter only lets through elements in the example_list that are not equal to 'c'.

.. code-block:: gor

   gorrow chr1,1,1 | CALC example_list 'a,b,c,d' | calc filtered_list listfilter(example_list,'x!=c')

The elements in the list are strings unless they specifically cast otherwise and so in order to compare numerical expressions, you must use an additional int() function to cast the elements into integers.

.. code-block:: gor

   gorrow chr1,1,1 | CALC example_list '3,6,9,12' | calc filtered_list listfilter(example_list,'INT(x)>6')

The next query shows that you can also compare elements in a list to the values in another column.

.. code-block:: gor

   gorrow 1,1,1 | CALC column_1 '4' | CALC column_2 '1,2,3,4' | calc filtered_list listfilter(column_2,'x!=column_1')

Any string can be used to compare, but if the string is explicitly stated in the expression and single quotations are used, the quotation marks around the string must be escaped.

.. code-block:: gor

   gorrow chr1,1,1 | CALC example_list 'apples,bananas,pears,peaches,apricots' | calc filtered_list listfilter(example_list,'x!=\'apples\'')

Alternately, the use of double quotations around the string in the comparison may be used when single quotes are used to envelop the expression (and vice versa).

.. code-block:: gor

   gorrow chr1,1,1 | CALC example_list 'apples,bananas,pears,peaches,apricots' | calc filtered_list listfilter(example_list,'x!="apples"')

List elements can also be compared based on their position (or 'index') in the list. In these cases, ``i`` is used instead of ``x``. The next query will return all elements in the list that have an index greater than 2, or in the given example: 'pears,peaches,apricots'.

.. code-block:: gor

   gorrow chr1,1,1 | CALC example_list 'apples,bananas,pears,peaches,apricots' | calc filtered_list listfilter(example_list,'i>2')