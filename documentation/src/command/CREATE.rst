.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: Materialised Queries</span>

.. _CREATE:

======
CREATE
======

**CREATE** statements can written for any valid GOR query. A **CREATE** statement will make an intermediate table, which can then be used (and reused) in other GOR queries.

The notation used for **CREATE** statements is not restricted, but the convention is often to use double hash symbols on either side of the create, which makes the instances of **CREATE** easier to find in a longer GOR query.

When referring to a **CREATE** statement within a GOR query, you must contain the name of the statement within square brackets.

Usage
=====

.. code-block:: gor

   CREATE ##example## = gor ... ;

Examples
========

The following GOR query creates an intermediate table of twenty rows in the first line. This table is then called within the gor query in the second line.

.. code-block:: gor

   CREATE ##example## = gorrows -p chr1:0-20;
   gor [##example##]