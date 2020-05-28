.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor only</span>

.. _KING:

====
KING
====
The **KING** command is used to calculate parameters that can be used in the KING algorithm (see Ani Manichaikul et.al.
Bioinformatics, Vol. 26 no. 22 2010, pages 2867–2873).

The left-input stream is of the same type as for **CSVSEL**, i.e. it must contain the columns ``values`` and ``bucket``.
The tag-bucket relation is used to define how the values for different tags are stored in the value column in each bucket.
The tag selections specify which tags are selected and the calculation is carried out for each tag (PN) in the first
tag list against each tag in the second tag list, for every variant in the left-input stream.  The left-input stream
must include a column with the allele frequency, named AF.  Such column is most easily added to the horizontal genotype
variant format with a **VARJOIN**.

The output is a row for each tag-pair, using columns named PN1 and PN2.  Since the output is an aggregate over the genome
is also includes dummy columns for Chrom and Pos with the values "ChrA" and 0, respectively.  The important columns are
however PN1, PN2, IBS0, XX, tpq, kpq, Nhet, Nhom, NAai, NAaj, count, pi0, phi, and theta.  See the code examples
and the KING paper for further details related to the **KING** command.


Usage
=====

.. code-block:: gor

    gor ... | KING tagbucketrelation tagselection1 tagselection2  [-gc cols] [(-s sep | -vs charsize)]

Options
=======

+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gc cols``        | Grouping columns other than (chrom,pos).  NOTE, one of the must include AF                         |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-s sep``          | Specify separator for the elements in values.  Default to comma.                                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-vs size``        | Use a fixed character size for values, e.g. rather than variable length separated with comma.      |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-pi0thr value``   | Threshold value to filter output on pi0, e.g. to pass we must have pi0 < value                     |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-phithr value``   | Threshold value to filter output on phi, e.g. to pass we must have phi > value                     |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-thetathr value`` | Threshold value to filter output on theta, e.g. to pass we must have theta > value                 |
+---------------------+----------------------------------------------------------------------------------------------------+


Examples
========

.. code-block:: gor

    The following query:

    gor [#vars#] | king -gc ref,alt,af -vs 1 #buckets#
                   <(nor #buckets# | inset -c pn <(nor #leftpns# ) | select pn)
                   <(nor #buckets# | inset -c pn <(nor #leftpns# ) | select pn)

    is equivalent to this query written withouth the KING command:

    gor #hvars# | csvsel -gc ref,alt,tpq,kpq -vs 1 -u 3 -tag PN #buckets# <(nor #buckets# | inset -c pn <(nor #leftpns# ) | select pn)
    | where value != '3'
    | multimap -c pn -h <(nor [#rel#] | where part = '#{col:part}' | select id1,id2)
    | varjoin -norm -r -xl id2 -xr pn <(gor #hgt#
    | where len(ref)=len(alt)
    | varjoin -i -norm [#usedvars#]
    | csvsel -gc ref,alt -vs 1 -u 3 -tag PN #buckets# <(nor #buckets# | inset -c pn <(nor #rightpns# ) | select pn)
    | where value != '3'
    )
    | calc values = str(value)+str(valuex)
    | calc IBS0 if(values='02' or values = '20',1,0)
    | calc XX if(values='01' or values = '10' or values = '21' or values = '12',1,if(values='02' or values = '20',4,0))
    | calc Nhet if(values = '11',1,0)
    | calc Nhom if(values = '02' or values = '20',1,0)
    | calc NAai if(left(values,1)='1',1,0)
    | calc NAaj if(right(values,1)='1',1,0)
    | group genome -gc pn,pnx -sum -fc IBS0,XX,tpq,kpq,Nhet,Nhom,NAai,NAaj

    The query that uses the KING command is MUCH faster when the PN-list are large!

.. code-block:: gor

    def #r# = rename #1 PN | replace #1 'PN'+#1;
    def #n# = 100;
    def #bucksize# = 17;

    create #bucket# = norrows #n# | #r# | rownum | calc bucket 'b'+str(1+div(rownum, #bucksize# )) | hide rownum;
    create #dummybuck# = norrows #n# | #r# | calc bucket 'b1';
    create #pns# = norrows #n# | #r#;
    create #pns1# = nor [#pns#] | top 10;
    create #pns2# = nor [#pns#] | skip 10 | top 20;

    create #rel# = nor [#pns1#] | multimap -cartesian [#pns2#] | rename PN id1 | rename PNx id2 | select id1,id2;

    create #values# = norrows 100 | rename #1 i | multimap -cartesian <(norrows #n# | rename #1 j) | calc gt mod(i+j,4) | group -gc i -lis -sc gt -s '' | rename #2 values;
    create #vars# = gorrows -p chr1:1-100 | select 1-2 | calc ref 'C' | calc alt 'A' | calc af random() | map -c pos [#values#] | calc bucket 'b1'
    | csvsel -tag PN -gc ref,alt,af -vs 1 [#dummybuck#] [#pns#] | map -c pn [#bucket#] | rename value gt | gtgen -gc ref,alt,af [#bucket#] <(gorrows -p chr1:1-2 | group chrom | calc pn '' | top 0);

    create #king# = nor <(gor [#vars#] | king -gc af [#bucket#] [#pns1#] [#pns2#] -vs 1) | select pn1-;

    nor [#king#] | calc monozygotic if(phi > pow(2.0,-1.5) and phi < 0.1,1,0)
    | calc parent_offspring if(phi > pow(2.0,-2.5) and phi < pow(2.0,-1.5) and pi0 < 0.1,1,0)
    | calc full_sib if(phi > pow(2.0,-2.5) and phi < pow(2.0,-1.5) and pi0 > 0.1  and pi0 < 0.365,1,0)
    | calc second_degree if(phi > pow(2.0,-3.5) and phi < pow(2.0,-2.5) and pi0 > 0.365 and pi0 < 1.0-pow(2,-1.5),1,0)
    | calc third_degree if(phi > pow(2.0,-4.5) and phi < pow(2.0,-3.5) and pi0 > 1.0-pow(2,-1.5) and pi0 < 1.0-pow(2,-2.5),1,0)

    The above query example shows how the parameters pi0 and phi (or theta) can be used in a relationship classifier.

Related commands
----------------

:ref:`CSVCC` :ref:`GTGEN` :ref:`GTLD`