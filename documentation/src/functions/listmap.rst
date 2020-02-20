.. _listmap:

=======
LISTMAP
=======

The **LISTMAP** function translates the given list using the expression in the second argument. It can be used to perform a ``+`` operation, whether to add a number to every number or to append text to elements in a list.

Each element in the list is referred to as ``x`` in the expression in the second argument.

The expression in the second argument must be in quotation marks and any additional quotation marks that are contained in the expression must be escaped with a backslash. This would be the case if you are appending text to elements in the given list as is shown in the examples below.

Usage
=====

``LISTMAP(string,expression) : string``

Example
=======
The query shown here uses the GOR alias ``#ClinicalGenes#`` as an example. The Gene_diseases column contains a list of diseases associated with each gene in a comma-separated list. The CALC step here appends the text 'xoxox' to each element in the list. Note that the element in the list is referred to as ``x`` in the function.

.. code-block:: gor

   gor #ClinicalGenes# | SELECT 1-3,Gene_diseases | TOP 100 | CALC test LISTMAP(Gene_diseases,'x+\'xoxox\'')

If the elements in a list are numbers, then the expression can be used to add a numerical value to the numbers in the list.

.. code-block:: gor

   gor #ClinicalGenes# | SELECT 1-3,Gene_diseases | TOP 100 | CALC test LISTMAP(gene_start,'x+1')