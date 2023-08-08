:orphan:

===============
Language Basics
===============

Data types
==========

.. list-table:: Data types in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Type
     - Explanation
     - Example
   * - String
     - Type used to represent textual data.
     -
   * - Number
     - A number can be an integer or a float.
     -
   * - Boolean
     - A logical entity that can be "true" or "false".
     -
   * - Literal list
     - A list of individual strings, with each element in the list encased in quotations and separated by commas.
     -

The data type of a column in GOR is not fixed, but is instead inferred from the first 10.000 values of the column. Also, the data type of a column can change depending on modifications that are made to the column by steps in the GOR query.

Note that if a number is enclosed in quotation marks it is cast as a string and, as a result, any operation performed on the value that expects a number as input will fail.

Operators
=========

Operators are symbols that can be used with two values to produce some result. The table below shows operators that are used in GORql along with some examples of how to use them.

.. list-table:: Operators in GORQL
   :widths: 8  25 8 15
   :header-rows: 1

   * - Operator
     - Explanation
     - Symbol
     - Example
   * - Addition
     - Adds two numbers together or concatenates two strings.
     - ``+``
     - 2 ``+`` 2 = 4 **or** 'cat' ``+`` 'dog' = 'catdog'
   * - Subtraction, Multiplication, Division
     - Standard mathematical operators
     - ``-``, ``*``, ``/``
     -
   * - Equality
     - Depending on the context, can be assignment of a value or a comparison operator
     - ``=``
     - x ``=`` 1 **or** CREATE ##set## ``=`` (1,2,3,4)
   * - Comparison
     - Less than, less than or equal, greater than, greater than or equal to
     - ``<``, ``<=``, ``>``, ``>=``
     -
   * - Not, Does-not-equal
     -
     - ``!``, ``!=``
     -


Comments
========

You can use comments in gor using the same notation as in Javascript or CSS. All lines contained within comment notation will be treated as comments, although if you use the commenting tool in the Query Editor in Sequence Miner, it will add comment tags to each line.

.. code-block:: gor

   /*
   This is a comment
   */

Comments can be used to quickly remove certain conditions from a GOR query if the query is well-formatted in the query editor (i.e. with individual conditions placed on single lines).


Other Punctuation in GOR
========================

Quotation marks
---------------

Single and double quotation marks can be used interchangeably when writing GOR queries, but you must take care to be consistent. For readability, if you are using both single and double quotation marks then it can be better to use the single quotation marks inside the double where necessary as opposed to the other way around, as this is the convention also used when writing text.

If quotation marks need to be used as part of a string and should not be interpreted as part of the syntax of the language itself, then they can be escaped using a backslash.


Hashes
------

Sections of reusable GOR queries can be defined through the use of :ref:`CREATE` statements. The convention in GOR is to set the name of the section inside hash symbols, such as ``#name_of_statement#``. This statement can then be called elsewhere in the code by putting the full name of the statement (and the hash symbols) inside square brackets, as in ``[#name_of_statement#]``