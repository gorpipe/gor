.. _f_regsel:

======
REGSEL
======

The **REGSEL** function extracts a single substring based on regular expression binding pattern, e.g. with single brackets.

The function takes two parameters - the source string and the regular expression to filter the source string. The part of the sting that you wish to "grab" out of the regular expression should be placed in curved brackets () to set it apart from the rest of the matching expression. The whole matching expression needs to be set in quotation marks.

If you are matching many different parts of a source to map to multiple columns, it can be more efficient to use the :ref:`REGSEL` command.

Usage
=====

``REGSEL(string,expression) : string``

Example
=======
The example query creates a column called "source" with a value of '41000_1_1', which is similar to how a phenotype column might look. The second **CALC** expression uses the regsel() function to match the pattern in the source column and only take the first number in the pattern.

.. code-block:: gor

   gorrow chr1,1,1 | CALC source '41000_1_1' | CALC test regsel(source,'(.*)_.*_.*')

