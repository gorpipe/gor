.. _listzip:

=======
LISTZIP
=======

The **LISTZIP** function zips two lists together into a single string separated by a semi-colon.

Usage
=====

``LISTZIP(string,string) : string``

Example
=======

The following query combines two lists ('A;B;C' and 'X;Y;Z') into a single list.

.. code-block:: gor

   gorrow 1,1,1 | CALC x 'A;B;C' | CALC y 'X;Y;Z' | CALC test listzip(x,y)

For a more practical example, the query below combines two columns of the Phenotypes report in the subject reports folder in a study. Note that in this example we are working in a NOR context, since this information is not genomic-ordered.

.. code-block:: gor

   nor -h SubjectReports/Phenotypes.rep | calc zipped_list listzip(present,code)