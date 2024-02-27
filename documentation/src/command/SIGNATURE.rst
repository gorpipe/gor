.. _SIGNATURE:

=========
SIGNATURE
=========

Usage
=====

.. code-block:: gor

	create foo = gor ... | signature [attributes] | ...

This command will inject a signature into the create statement and invalidating it based on the input criteria.

This command will not effect standard gor queries.

Options
=======

+-----------------------+----------------------------------------------------------------------+
| ``-timeres seconds``  | Time stamp resolution in seconds.                                    |
+-----------------------+----------------------------------------------------------------------+
| ``-file filepath``    | Path to a file to calculate the signature from.                      |
+-----------------------+----------------------------------------------------------------------+

The ``-timeres`` or the ``-file`` option is required. If both are present the command fails with an error.

Example
=======

.. code-block:: gor

   create foo = gor #dbSNP# | signature -timeres 1000 | top 10;
   gor [foo]

The above query will re-evaluate the #dbSNP# query at least every 1000 seconds and regenerate the underlying cache-file.

.. code-block:: gor

   create foo = gor #dbSNP# | signature -file ../foo.txt | top 10;
   gor [foo]

The above query will re-evaluate the #dbSNP# query when changes are detected in the ``../foo.txt`` file.