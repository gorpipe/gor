.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor</span>

.. _VARGROUP:

===========
GTTRANSPOSE
===========
The incoming stream are variants stored in horizontal manner. The command takes in a sources containing
tag/bucket relations, list of pns and a list of markers. The optional headers in the tag/bucket file and
the pns file should start with '#'. The marker file must contain a header.

The out-coming stream is a nor table, with one column named PNs and either one another column named VALUES,
containing the genotypes of the pn by marker in the order defined by the marker source, or one column for each
marker. (It picks out the given pns from the incoming stream and then transposes the table.)

In order to find the markers in the incoming source, all the columns from the header of the marker source are taken
and the values in the corresponding columns in the incoming source (we assume they exist, else we throw and error) are
taken as identifier for the marker.

Usage
=====

.. code-block:: gor

	gor ... | GTTRANSPOSE tagBucketSource pnSource markerSource [-vs w] [-sep s] [-cols]

Options
=======

+-------------------+---------------------------------------------------------------------------+
| ``-vs w           | The number of characters used to store the genotypes, in case they are in |
|                   | fixed width format.                                                       |
+-------------------+---------------------------------------------------------------------------+
| ``-sep separator``| The separator used to separate the genotypes in the incoming stream, in   |
|                   | case they are separated by some separator. Must be a single character.    |
|                   | The same separator is used in the output.                                 |
+-------------------+---------------------------------------------------------------------------+
| ``-cols``         | Whether to write all the genotypes in one column or get one column per    |
|                   | marker.                                                                   |
+-------------------+---------------------------------------------------------------------------+