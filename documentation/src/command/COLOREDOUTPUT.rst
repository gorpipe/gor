.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _COLOREDOUTPUT:

=============
COLOREDOUTPUT
=============
The **COLOREDOUTPUT** command can be added to the end of a gor or nor query to colorise its output. The colorisation is either a rotating color or a color based on the column type.

Options
=======

+--------+-----------------------------------------+
| ``-t`` | Formats the output based on column type |
+--------+-----------------------------------------+

Use ``COLOREDOUPUT -t`` to color the output based on colum type.

Usage
=====

.. code-block:: gor

	gor ... | COLOREDOUTPUT [-t]

Examples
========

.. code-block:: gor

   gor #dbsnp# | calc a 1.0 | calc b long(a*2.0) | calc c 10 | coloredoutput -t

The above command will output the first ten rows of the ``#dbsnp#`` table with the output colored based on the column type.

.. image:: ../images/commands/COLOREDOUTPUT.png
