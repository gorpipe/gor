.. _lower:

=====
LOWER
=====

The **LOWER** function converts a string to lowercase.

Usage
=====

``LOWER(string) : string``

Example
=======
The query below will select the position information and Gene_Symbol columns from the alias for Gene Details and create an extra column that converts the Gene_Symbol column to lowercase letters.

.. code-block:: gor

   gor #GeneDetails# | SELECT 1-3,Gene_Symbol | TOP 10 | CALC test lower(Gene_Symbol)