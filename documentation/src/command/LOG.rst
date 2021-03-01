.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _LOG:

===
LOG
===
The **LOG** command specifies how often we log rows to monitor the progress of a query as it runs.

Usage
=====

.. code-block:: gor

    gor ... | LOG [-l label | -t time] number

Options
=======

+---------------+----------------------------------------------------------+
| ``-l label``  | The column which the lookup is based upon.               |
+---------------+----------------------------------------------------------+
| ``-t time``   | Case-insensitive column lookup.                          |
+---------------+----------------------------------------------------------+
| ``-a level``  | Log level, error, warn or info.                          |
+---------------+----------------------------------------------------------+

Examples
========

.. code-block:: gor

    gor #dbSNP# | log 1000000 | group 1000 -count | log 1000

The query above returns two types of log records (log1 and log2).  log1 shows every million'th row in the #dbSNP# source while log2 shows every thousand aggregated row. If no number is specified the default value is 1 and every row is logged.

If the -t flag is set, the time specifies the seconds between logs and the number will no longer have any effect.

For example, the following query:

.. code-block:: gor

    gor #dbSNP# | log -t 10 logs every ten seconds.

The log goes to stderr in GORpipe and the monitor window in the gorviewer.