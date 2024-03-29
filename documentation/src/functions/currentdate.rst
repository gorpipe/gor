.. _currentdate:

===========
CURRENTDATE
===========

The **CURRENTDATE** function returns the current date and time in the given format, or ‘yyyy-MM-dd HH:mm:ss’ if
no format is given.


Usage
=====

``date() : date``

``date(format) : date``

Examples
========

The following example adds a column ``x`` with the current date and time.

.. code-block:: gor

   ... | CALC x date() | ...

Date Format
===========

You can use the following symbols in your formatting pattern:

+--+--------------------------------------------------------------------------------------+
|G | Era designator (before christ, after christ)                                         |
+--+--------------------------------------------------------------------------------------+
|y | Year (e.g. 12 or 2012). Use either yy or yyyy.                                       |
+--+--------------------------------------------------------------------------------------+
|M |Month in year. Number of M's determine length of format (e.g. MM, MMM or MMMMM)       |
+--+--------------------------------------------------------------------------------------+
|d |Day in month. Number of d's determine length of format (e.g. d or dd)                 |
+--+--------------------------------------------------------------------------------------+
|h |Hour of day, 1-12 (AM / PM) (normally hh)                                             |
+--+--------------------------------------------------------------------------------------+
|H |Hour of day, 0-23 (normally HH)                                                       |
+--+--------------------------------------------------------------------------------------+
|m |Minute in hour, 0-59 (normally mm)                                                    |
+--+--------------------------------------------------------------------------------------+
|s |Second in minute, 0-59 (normally ss)                                                  |
+--+--------------------------------------------------------------------------------------+
|S |Millisecond in second, 0-999 (normally SSS)                                           |
+--+--------------------------------------------------------------------------------------+
|E |Day in week (e.g Monday, Tuesday etc.)                                                |
+--+--------------------------------------------------------------------------------------+
|D |Day in year (1-366)                                                                   |
+--+--------------------------------------------------------------------------------------+
|F |Day of week in month (e.g. 1st Thursday of December)                                  |
+--+--------------------------------------------------------------------------------------+
|w |Week in year (1-53)                                                                   |
+--+--------------------------------------------------------------------------------------+
|W |Week in month (0-5)                                                                   |
+--+--------------------------------------------------------------------------------------+
|a |AM / PM marker                                                                        |
+--+--------------------------------------------------------------------------------------+
|k |Hour in day (1-24, unlike HH's 0-23)                                                  |
+--+--------------------------------------------------------------------------------------+
|K |Hour in day, AM / PM (0-11)                                                           |
+--+--------------------------------------------------------------------------------------+
|z |Time Zone                                                                             |
+--+--------------------------------------------------------------------------------------+
|' |Escape for text delimiter                                                             |
+--+--------------------------------------------------------------------------------------+
|' |Single quote                                                                          |
+--+--------------------------------------------------------------------------------------+

Characters other than these will be treated as normal text to insert into the pattern, and thus into 
the formatted dates.

Some characters can be used in different numbers. For instance, you can write either yy for a 2-character 
version of the year (e.g. 12), or you can write yyyy for a 4-character version of the year (e.g. 2012).

Pattern Examples
================
Here are a few date pattern examples:

+--------------------------------+-----------------------------------------------------------------------+
| Pattern                        | Example                                                               |
+--------------------------------+-----------------------------------------------------------------------+
| dd-MM-yy                       | 31-01-12                                                              |
+--------------------------------+-----------------------------------------------------------------------+
| dd-MM-yyyy                     | 31-01-2012                                                            |
+--------------------------------+-----------------------------------------------------------------------+
| MM-dd-yyyy                     | 01-31-2012                                                            |
+--------------------------------+-----------------------------------------------------------------------+
| yyyy-MM-dd                     | 2012-01-31                                                            |
+--------------------------------+-----------------------------------------------------------------------+
| yyyy-MM-dd HH:mm:ss            | 2012-01-31 23:59:59                                                   |
+--------------------------------+-----------------------------------------------------------------------+
| yyyy-MM-dd HH:mm:ss.SSS        | 2012-01-31 23:59:59.999                                               |
+--------------------------------+-----------------------------------------------------------------------+
| yyyy-MM-dd HH:mm:ss.SSSZ       | 2012-01-31 23:59:59.999+0100                                          |
+--------------------------------+-----------------------------------------------------------------------+
| EEEEE MMMMM yyyy HH:mm:ss.SSSZ | Saturday November 2012 10:45:42.720+0100                              |
+--------------------------------+-----------------------------------------------------------------------+
