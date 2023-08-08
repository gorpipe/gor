.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _VERIFYVARIANT:

=============
VERIFYVARIANT
=============

The :ref:`VERIFYVARIANT` command ensures that the reference column (REF or REFERENCE) corresponds to the reference from
the configured build. It also verifies that the alt column (ALT, CALL or ALLELE) does not exceed a maximum length.

Usage
=====

.. code-block:: gor

	gor ... | VERIFYVARIANT

Options
=======

+--------------------+-----------------------------------------------------+
| ``-ref col``       | The column denoting the reference seq.              |
+--------------------+-----------------------------------------------------+
| ``-alt col``       | The column denoting the alternate (alleles) seq.    |
+--------------------+-----------------------------------------------------+
| ``-maxlen len``    | The maximum length allowed for the alternate seq.   |
+--------------------+-----------------------------------------------------+
