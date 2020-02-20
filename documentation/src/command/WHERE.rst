.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _WHERE:

=====
WHERE
=====
The :ref:`WHERE` command is used to filter rows in a GOR stream and extract only the rows that fulfil a specific conditional expression.

Usage
=====

.. code-block:: gor

   gor ... | WHERE expression1 AND (expression2 OR expression3) AND NOT (expression4)

Operators
---------
The standard comparison operators (<,<=,= or ==,<> or !=,=>,>) can be applied for each type: floating point numbers, integers, and strings. Boolean conditions can also be formed as: expr1 AND ( expr2 OR NOT (expr3) ).

The LIKE and RLIKE operators are also supported for strings. **LIKE** uses standard ``?`` and ``*`` notation in the pattern whereas **RLIKE** uses any regular-expression pattern, e.g. #7 rlike '.*[a|b].+'.


The WHERE command supports predicates based on any calculations as described for the :ref:`CALC` command.
Predicates can also be based on ISINT and ISFLOAT, e.g. ISFLOAT(#3) and not( ISINT(#2) ).

See also :ref:`LEFTWHERE` to provide generic left-join filtering following the :ref:`JOIN` command.

Examples
========

.. code-block:: gor

	gor ... | where Colx > Coly and not (#3 like '*dna*' or #4f > 0.0)