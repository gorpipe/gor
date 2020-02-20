.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _SDL:

===
SDL
===
The SDL command allows you to run arbitrary SDL commands against an SDL server (the server here being defined by the content of a file called gor.sdl.credentials in the config directory).

SDL statements need to be encapsulated between curly brackets, e.g. sdl {sdl_statement}.

.. warning:: The feature described here should be considered to be **early access** and users should be advised that functionality is still under active development.

Usage
=====

.. code-block:: gor

   SDL {<sdl query>}

If the SDL query contains no pipe symbol ('|') the quotes can be omitted.
The SDL query can be any valid SDL query. See SDL documentation on query construction.

Examples
========

Example queries:


 * ``SDL { PN.POPUCHA.BASECHA.AGEA > 70 }``
 * ``CREATE ##a## = SDL '{ PN.POPUCHA.BASECHA.AGEA > 70 }'; nor [##a##]``
 * ``SDL {select  distinct D_PN from ( select PN AS  D_PN ,VALUE AS  D_AGEA  from  UKB_21022 ) REC_1  where REC_1.D_AGEA > 70} | top 1``
 * ``SDL {'[{PN | PN.POPUCHA.BASECHA.AGEA > 70 },descr(PN.POPUCHA.BASECHA.SEX) as sex,PN.POPUCHA.BASECHA.TOWNS as townsend_index]'}``

.. note:: The following would fail because of pipe symbol in sdl query

  * ``SDL {[{PN | PN.POPUCHA.BASECHA.AGEA > 70 },descr(PN.POPUCHA.BASECHA.SEX) as sex,PN.POPUCHA.BASECHA.TOWNS as townsend_index]}``