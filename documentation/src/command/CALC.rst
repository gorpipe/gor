.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _CALC:

====
CALC
====
The :ref:`CALC` command supports integer, floating point, and string-based calculations. It adds a single new column (named according to the parameter colname) to the output. The command can be used repeatedly to create multiple new columns.

Reference to columns and functions is case-insensitive. The column data type (int,float, or string) is inferred from the first 10000 rows in the input stream.

In expressions, columns can be referred to either by their name or with the syntax #colnum[s|i|f].

As an example #3 refers to column 3 whereas #3s refers to column 3 as a string.  Likewise, #4i instructs the compiler that column four contains integers and #4f that it contains floating point numbers. If no type-suffix is specified or the column name is used, the command will infer the data type from the first rows in the input.  This can potentially cause the type to be set to integers while the rest of the input contains floats or that the type is set to float while the column actually contains strings. Therefore, it is made possible to specify the type through the # suffix syntax as described above.

Examples of arithmetic expressions are: (#3 + 4.0) or for instance IF(#4 > 1.0,log(#3),exp(#3))

The CALC command also allows equal sign, e.g. calc colname = expression.

See the :ref:`functions<predefinedFunctions>` to get a list of available functions and :ref:`WHERE` for the structure of functions.

Usage
=====

.. code-block:: gor

	gor ... | CALC colname expression

	gor ... | CALC colname1,colname2 expression1,expression2

Examples
========
In the following examples, we use the :ref:`GORROW` command to generate a single row, which we will then use to demonstrate various different calculations using the CALC command.

The following query example shows a string concatenation that combines two calculated columns in a third and then calculates the length of the string using the ``len`` function.

.. code-block:: gor

   GORROW chr1,1,1 | CALC FirstName = 'John' | CALC LastName 'Smith'
   | CALC FullName FirstName + ' ' + LastName | CALC NameLength len(FullName)

The next query example calculates a random number (using the random function) and performs an additional calculation checking if the number generated is greater than 0.5 or not and assigns a label of "red" or "green" to the row based on that calculation. This example also shows how to use a negative match within an IF statement. Note the use of brackets in the example.

.. code-block:: gor

   GORROW chr1,1,1 | CALC random_number = random() | CALC checkcol if(NOT(#4f > 0.5),'red','green')

The query below will throw an exception if the random number that is generated is greater than 0.5.

.. code-block:: gor

   GORROW chr1,1,1 | CALC random_number = random() | THROWIF (#3f > 0.5)

The CALC command supports calculating more than one columns in on step.

.. code-block:: gor

   gor ... | select 1,2 | calc ColumnThree,aIntCol random(),int(#3)+3

If the column name is a nucleotide, the column name must be uppercase.

