.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: Materialised Queries</span>

.. _DEF:

===
DEF
===

**DEF** statements are used in a GOR query for definitions (or "aliases"). They may be used to define paths or to calculate values that will be used repeated in the GOR query. Definitions are typically found at the beginning of a report builder yml before the main query for the report builder starts.

The name of the **DEF** statement can be any string, but typically definition names are enclosed in double hash symbols, as in ``##name_of_definition##``.

Usage
=====

.. code-block:: gor

   def <name_of_definition> = <path_or_calculation>;

Examples
========

The example below shows a typical definition that would be found at the beginning of a report builder that would refer to the Ensemble genes files in the reference data. When working in the Sequence Miner, these definitions would not be necessary since the ``##genes##`` alias is defined as part of the GOR alias list.

.. code-block:: gor

   def ##genes## = ##ref##/ensgenes/genes.gorz;