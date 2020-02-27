.. _if:

==
IF
==

An **IF** conditional expression can be used in :ref:`CALC` commands in GOR to return one value if the condition defined in the function returns true and another if false. :ref:`IF` cannot be used within a :ref:`WHERE` command, which is itself a conditional expression.

Columns can be referred to within the IF statement by their names or by their column number, although using the names of the columns is preferable for readability of GOR queries. Strings need to be contained within quotation marks.

IF statements can be nested. I.e., both the true and the false result in an IF statement


Usage
=====

.. code-block:: gor

	if(str,any,any) : string

Examples
========

The following query would always return the value 'red', since the condition ('1=1') is always true.

.. code-block:: gor

   gor example.vcf | CALC result_column IF(1=1,'red','blue')

The next query shows an IF statement that refers to the ``Call`` column and returns true if the value of the base call is T or A. Otherwise, 'false' is returned.

.. code-block:: gor

   gor #wesvars# | TOP 100 | CALC x IF(Call='T' or Call = 'A','true', 'false')

In the following example, the length (len) of the Reference and Call columns are compared and if they are equal, the type column is given the value 'snp'. Otherwise, the type is set to 'indel'.

.. code-block:: gor

   gor #wesvars# | TOP 100 | CALC type IF(len(Reference) = len(Call),'other', 'indel')

We can use "nested" IF statements, as shown in the next example to further classify our

.. code-block:: gor

   gor #wesvars# | TOP 1000 | CALC x IF(len(Reference) = len(Call),'substitution', if(len(Reference) < len(Call), 'insertion', 'deletion'))