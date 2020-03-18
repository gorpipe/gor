.. _GAVA:

====
GAVA
====
**GAVA** is a variant association algorithm, based on the VAAST algorithm.
http://www.ncbi.nlm.nih.gov/pubmed/21700766

Computes a p-value for a genomic feature (typically a single gene) using a randomization test, where the
affection status of the subjects is permuted and the test statistic computed using a composite likelihood
ratio test as described in the VAAST paper.

Note: This is not an exact implementation and for example we have a bailout parameter to avoid spending
too much computing time on uninteresting features.

Usage
=====

We need to first set the case and control lists, the disease model (regular, dominant, or recessive),
and other parameters. Then the procedure is as follows:
    * Before processing a feature, call initializeGroup() to clear the counts.
    * Iterate through the data for the feature and for each variant found for some subject, add the call counts using addFeature().
    * Calculate the p-value (and a few other values) using calculateValues().
    * Repeat the process for the next feature.

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
