.. _GAVA:

====
GAVA
====
The **GAVA** command

Usage
=====

.. code-block:: gor

    gor ... | GAVA maxiterations -casefile <filename> -ctrlfile <filename> [ attributes ]

or:

.. code-block:: gor

    gor ... | GAVA maxiterations -caselist <subjects> -ctrllist <subjects> [ attributes ]

Options
=======

+------------------------+-------------------------------------------------------------------------+
| ``-recessive``         | Use recessive model, i.e. only pick 2 variants per subject.             |
+------------------------+-------------------------------------------------------------------------+
| ``-dominant``          | Use dominant model, i.e. only pick 1 variant per subject.               |
+------------------------+-------------------------------------------------------------------------+
| ``-casepene number``   | The case penetrance (locus heterogeneity).                              |
+------------------------+-------------------------------------------------------------------------+
| ``-ctrlpene number``   | The ctrl penetrance.                                                    |
+------------------------+-------------------------------------------------------------------------+
| ``-noMaxAlleleCounts`` | No max allele count for | each case in dominant/recessive models.       |
+------------------------+-------------------------------------------------------------------------+
| ``-protective``        | Include protective alleles.                                             |
+------------------------+-------------------------------------------------------------------------+
| ``-grouping number``   | Collapsing threshold for rare variants (default is 5).                  |
+------------------------+-------------------------------------------------------------------------+
| ``-bailout number``    | Bail out of the randomization with this number of hits (default is 10). |
+------------------------+-------------------------------------------------------------------------+
| ``-usePhase``          | Use the phase (in recessive modeling).                                  |
+------------------------+-------------------------------------------------------------------------+
| ``-maxAf number``      | Upper threshold for the estimated control allele frequency.             |
+------------------------+-------------------------------------------------------------------------+

.. WARNING::

    casepene and ctrlpene specify the number of cases and ctrls which DO NOT have to comply with the inheritance model.

Examples
========
.. code-block:: gor

    create #cosmic# = pgor ref/cosmic.gorz | select 1,2,ref,alt,CNT- | distinct | group 1 -gc ref,alt -len 1000 -sc primarysites,cnt,pscount -lis