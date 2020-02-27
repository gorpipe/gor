.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _SETCOLTYPE:

==========
SETCOLTYPE
==========
The **SETCOLTYPE** command explicitly sets the types of columns.

The command takes two parameters - a list of columns and a list of types. The known types are:
Integer, Long, Double and String. The type may also be specified by the first character of the type.

Usage
=====

.. code-block:: gor

	gor ... | SETCOLTYPE list-of-columns list-of-types

Examples
========

.. code-block:: gor

   gorrow 1,1 | calc x 42 | calc y 3.14 | calc z x+y | setcoltype x,y,z string,STRING,s | coltype

The calc expressions in the command above produce numeric results, but the SETCOLTYPE command changes
them all to string columns.

