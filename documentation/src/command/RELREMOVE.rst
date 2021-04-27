.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _RELREMOVE:

=========
RELREMOVE
=========
The **RELREMOVE** command is used to remove related samples/individuals (PNs) from a phenotype relation.

The left-most column must be the sample identifier (e.g. PN) and the other columns should represent one or more phenotypes
with case-control-unknown statuses or quantitative trait (QT).  QTs use 'NA' to represent missing value while case-control
phenotypes can be in Plink compatible format (case/ctrl/unknown) = (2/1/-9), (2/1/NA), (2/1/0) or in a format like
(CASE/CTRL/EXCL) or (CASE/CTRL/NA).

Individuals that are "eliminated" are by default set to the unknown value, as defined in the column at hand, however,
this can be overwritten by the ``-rsymb`` option.  This can also be useful to inspect which samples are eliminated.

Cases and control are by default treated as one group, however, when there is a relationship between a case and a control,
the control is eliminated first.  The option ``-sepcc`` can be used to ignore relationships across the case and the control group.

The algorithm that eliminates relatives is greedy, i.e. eliminates the sample with the most relative first and then updates the
relative count after each elimination, continuing until there are no related pairs in each phenotype.

The relatives are supplied as a binary relation, e.g. (pn1,pn2).  Note that this relation does not have to be
symmetric because transitivity in relationships is assumed.

Usage
=====

.. code-block:: gor

    nor phenotypes.tsv | RELREMOVE relative_relation [-sepcc] [-rsymb value]

Options
=======

+---------------------+----------------------------------------------------------------------------------------------------+
| ``-rsymb value``    | Symbol to over-write the definition of unknown/exclusion.                                          |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-sepcc``          | Treat cases and controls as separate groups when analyzing relationships.                          |
+---------------------+----------------------------------------------------------------------------------------------------+

Examples
========

Eliminating relatives for 100 phenotypes in two steps, first closely related individuals - then less closely related individuals.

.. code-block:: gor

    nor pheno.tsv | select PN,pheno1-pheno100
    | relremove <(nor relatives.tsv | where kinship >= 0.2 | select pn1,pn2)
    | relremove <(nor relatives.tsv | where kinship >= 0.05 | select pn1,pn2)


Below are examples of self-contained tests that explain the command.

.. code-block:: gor

    /* Generate 100k samples */
    create #pns# = norrows 100000 | calc pn #1 | select pn;

    /* Generate artifical relationships */
    create #r# = nor [#pns#] | multimap -cartesian <(norrows 100 | group -lis -sc #1)
    | replace #2 listfilter(listmap(#2,'round(10000*random())'),'random()<0.05') | rename #1 pn1 | rename #2 pn2
    | split pn2 | where pn2 != '' and pn1 != pn2;

    /* Create several phenotypes */
    create #pheno# = nor [#pns#]
    | calc pheno1 if(random()<0.01,'NA',str(random()))
    | calc pheno2 mod(pn,3)
    | calc pheno3 pheno1
    | calc pheno4 decode(pheno2,'0,NA,1,0,2,1')
    | calc pheno5 decode(pheno2,'0,-9,1,0,2,1');

    /* Test if identical columns are treated in same way */
    create #t1# = nor [#pheno#] | relremove [#r#] -rsymb hakon | throwif pheno3 != pheno1  | top 1;

    /* Test if identical columns are treated in same way with -sepcc option */
    create #t2# = nor [#pheno#] | relremove [#r#] -rsymb hakon -sepcc  | throwif pheno3 != pheno1  | top 1;

    /* Test if identical columns are treated in same way with no option */
    create #t3# = nor [#pheno#] | relremove [#r#] | throwif pheno3 != pheno1  | top 1;

    /* Test if elim counts of identical columns are the same and that -sepcc option reduces the number of eliminated rows for case-control */
    create #t4# = nor [#pheno#] | relremove [#r#] -rsymb elim | unpivot 2- | where col_value = 'elim' | group -gc col_name -count
    | pivot col_name -v pheno1,pheno2,pheno3,pheno4,pheno5 | rename (.*)_allcount #{1}
    | multimap -cartesian <(nor [#pheno#] | relremove [#r#] -sepcc -rsymb elim | unpivot 2- | where col_value = 'elim' | group -gc col_name -count
    | pivot col_name -v pheno1,pheno2,pheno3,pheno4,pheno5 | rename (.*)_allcount #{1})
    | throwif pheno1 != pheno3 or pheno2!=pheno4 or pheno2 != pheno5 or pheno2 != pheno5 or pheno2 < pheno2x or pheno4 < pheno4x or pheno5 < pheno5x;

    /* Test if there are relatives after elimination */
    create #t5# = nor [#pheno#] | select pn,pheno1 | relremove [#r#] -rsymb elim | where pheno1 != 'elim' and pheno1 != 'NA' | multimap -c pn [#r#]
    | multimap -c pn2 <(nor [#pheno#] | select pn,pheno1 | relremove [#r#] -rsymb elim | where pheno1 != 'elim' and pheno1 != 'NA') | throwif 2=2;

    /* Test if there are relatives after elimination within either case or ctrl groups */
    create #t6# = nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim | where pheno4 != 'elim' and pheno4 != 'NA' | multimap -c pn [#r#]
    | multimap -c pn2 <(nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim  | where pheno4 != 'elim' and pheno4 != 'NA') | throwif 2=2;

    /* Test if there are relatives after elimination within same case-ctrl group */
    create #t7# = nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim -sepcc | where pheno4 != 'elim' and pheno4 != 'NA'
    | multimap -c pn [#r#] | multimap -c pn2 <(nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim -sepcc
    | where pheno4 != 'elim' and pheno4 != 'NA') | where pheno4 = pheno4x | throwif 2=2;

    /* Test if there are fewer eliminations than with a simple method */
    create #t8# = nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim | where pheno4 = 'elim' | group -count | calc method 'relremove'
    | merge <(nor [#pheno#] | select pn,pheno4
    | inset -c pn -b <(nor [#r#] | calc pn pn1+','+pn2 | select pn | split pn ) | where inset = 1 | group -count | calc method 'simple')
    | pivot method -v relremove,simple | throwif relremove_allcount > simple_allcount;

    /* Check that samples with the max number of relatives are eliminated and that samples with no relatives are kept */
    create #t9# = nor [#pheno#] | select pn | calc pheno random() | relremove [#r#] -rsymb elim
    | map -c pn -m 1000 <(nor [#r#] | calc pn pn1+','+pn2 | select pn | split pn | group -gc pn -count | rank allcount -o desc)
    | throwif rank_allcount > 0 and rank_allcount < 5 and pheno != 'elim' or allcount = 0 and pheno = 'elim' | top 10;

    nor [#t9#] | top 1


