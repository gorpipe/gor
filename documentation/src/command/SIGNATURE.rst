.. _SIGNATURE:

=========
SIGNATURE
=========

Usage
=====

.. code-block:: gor

	create foo = gor ... | signature [attributes] | ...

This command will inject an additional invalidation criterion into the cached result of the ``CREATE`` statement.

The signature of any ``CREATE`` encompasses the signatures of the files it uses, so that a change to any of those files
will be recognized as invalidating any cached result from that ``CREATE``.
Invalidation means that the expression is re-evaluated when its value is used.
If a ``SIGNATURE`` command is included, that injects an *additional* signature, so that the cached value will also be
subject to invalidation by the selected criterion: either when a certain time has passed (``timeres``)
or when a designated file has been updated (``file``).

This command will not affect standard gor queries.

Options
=======

+-----------------------+------------------------------+-----------------------------------------------------------+
| ``-timeres seconds``  | Time resolution in seconds.  | Result is invalidated periodically with this time period. |
+-----------------------+------------------------------+-----------------------------------------------------------+
| ``-file filepath``    | Path to a file.              | Result is invalidated when the file's timestamp changes.  |
+-----------------------+------------------------------+-----------------------------------------------------------+

The ``-timeres`` or the ``-file`` option is required. If both are present the command fails with an error.




If multiple ``SIGNATURE`` commands are included in a single ``CREATE``, only the first one is honored, and the others are ignored.


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

