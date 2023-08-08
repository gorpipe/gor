.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Source Command</span>

.. _METAINFO:

====
META
====

**META** is a :term:`Source commands` that displays all meta information associated with a given input source.

To use META, you specify the input source to explore and the META input source will return source, name and value of all meta properties associated with the given input source.

Usage
=====

.. code-block:: gor

   META <input_source>

Examples
========
The following example creates 10 rows starting from 10 with increment of 10.

.. code-block:: gor

   META samples.bam

Output
======

+--------+--------------------+----------------------------------+
| Source | Name               | Value                            |
+--------+--------------------+----------------------------------+
| SOURCE | NAME               | <path>/samples.bam               |
+--------+--------------------+----------------------------------+
| SOURCE | PATH               | <path>/samples.bam               |
+--------+--------------------+----------------------------------+
| SOURCE | DATA_TYPE          | BAM                              |
+--------+--------------------+----------------------------------+
| SOURCE | TYPE               | FILE                             |
+--------+--------------------+----------------------------------+
| SOURCE | PROTOCOLS          |                                  |
+--------+--------------------+----------------------------------+
| SOURCE | REMOTE             | false                            |
+--------+--------------------+----------------------------------+
| SOURCE | SUPPORTED          | true                             |
+--------+--------------------+----------------------------------+
| SOURCE | MODIFIED           | 1673530583578                    |
+--------+--------------------+----------------------------------+
| SOURCE | ID                 | 3F1D72574FE3B8DA60FDFBA7E0BCDFE0 |
+--------+--------------------+----------------------------------+
| FILE   | PATH               | <path>/BVL_FATHER_SLC52A2.bam    |
+--------+--------------------+----------------------------------+
| FILE   | NAME               | BVL_FATHER_SLC52A2.bam           |
+--------+--------------------+----------------------------------+
| FILE   | TYPE               | BAM                              |
+--------+--------------------+----------------------------------+
| FILE   | SUFFIX             | .bam                             |
+--------+--------------------+----------------------------------+
| FILE   | SIZE               | 171379                           |
+--------+--------------------+----------------------------------+
| FILE   | MODIFIED           | 1673530583578                    |
+--------+--------------------+----------------------------------+
| FILE   | MODIFIED_UTC       | Jan 12, 2023, 1:36:23 PM         |
+--------+--------------------+----------------------------------+
| FILE   | ID                 | 3F1D72574FE3B8DA60FDFBA7E0BCDFE0 |
+--------+--------------------+----------------------------------+
| FILE   | SUPPORTS_INDEX     | true                             |
+--------+--------------------+----------------------------------+
| FILE   | INDEX              | <path>>/samples.bam.bai          |
+--------+--------------------+----------------------------------+
| FILE   | SUPPORTS_REFERENCE | false                            |
+--------+--------------------+----------------------------------+
| FILE   | REFERENCE          |                                  |
+--------+--------------------+----------------------------------+