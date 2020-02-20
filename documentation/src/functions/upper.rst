.. _upper:

=====
UPPER
=====

The **UPPER** function converts a string to upper case.

Usage
=====

``UPPER(string) : string``

Example
=======
The query below will select the position information, Gene_Symbol and Biotype columns from the alias for Gene Details and create an extra column that converts the Biotype column to uppercase letters.

.. code-block:: gor

   gor #GeneDetails# | SELECT 1-3,Gene_Symbol,Biotype | TOP 10 | CALC test upper(Biotype)