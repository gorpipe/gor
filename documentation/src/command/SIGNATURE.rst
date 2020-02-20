.. _SIGNATURE:

=========
SIGNATURE
=========

Usage
=====

.. code-block:: gor

	create foo = gor ... | signature [attributes] | ...

This command will inject a signature into the create statement and invalidating it based on the input criteria.

This command will no effect standard gor queries.

Options
=======

+-----------------------+----------------------------------------------------------------------+
| ``-timeres seconds``  | Time stamp resolution in seconds.                                    |
+-----------------------+----------------------------------------------------------------------+

The ``-timeres`` option is required.

Example
=======

.. code-block:: gor

   create foo = gor #dbSNP# | signature -timeres 1000 | top 10;
   gor [foo]

The above query will re-evaluate the #dbSNP# query at least every 1000 seconds and regenerate the underlying cache-file.