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

See also the KING2 and QUEEN commands.


Usage
=====

.. code-block:: gor
    gor ... | KING tagbucketrelation tag-list1 tag-list2  [-gc cols] [-sym] [(-s sep | -vs valuesize)] [-maxvars num_variants]

Options
=======

+---------------------+----------------------------------------------------------------------------------------------------+
| ``-gc cols``        | Grouping columns other than (chrom,pos).  NOTE, one of the must include AF                         |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-sym``            | Assume tags1 and tags2 lists are the same and return only output for one of the pairs, e.g. pn1<pn2|
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-s sep``          | Specify separator for the elements in values.  Default to comma.                                   |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-vs size``        | Use a fixed character size for values, e.g. rather than variable length separated with comma.      |
+---------------------+----------------------------------------------------------------------------------------------------+
| ``-maxvars value``  | The maximum number of variants used in the calculation, e.g. 100k.  Memory 2bits per gt. Def. 1000 |
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
                   <(nor #buckets# | inset -c pn <(nor #rightpns# ) | select pn)

    is equivalent to this query written without the KING command:

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

.. code-block:: gor
    Below is a query that compares the different commands using simulated data for testing purposes:

    def #r# = rename #1 PN | replace #1 'PN'+#1;
    def #n# = 10;
    def #bucksize# = 17;

    create #bucket# = norrows #n# | #r# | rownum | calc bucket 'b'+str(1+div(rownum, #bucksize# )) | hide rownum;
    create #dummybuck# = norrows #n# | #r# | calc bucket 'b1';
    create #pns# = norrows #n# | #r#;
    create #pns1# = nor [#pns#];
    create #pns2# = nor [#pns#];

    create #rel# = nor [#pns1#] | multimap -cartesian [#pns2#] | rename PN id1 | rename PNx id2 | select id1,id2;

    create #values# = norrows 1000 | rename #1 i | multimap -cartesian <(norrows #n# | rename #1 j) | calc gt if(random()<0.5,'1','0') /*  mod(i+j,2) */ | group -gc i -lis -sc gt -s ''
    | rename #2 values;

    create #vars# = gorrows -p chr1:1-1000 | select 1-2 | calc ref 'C' | calc alt 'A' | calc af random() | map -c pos [#values#] | calc bucket 'b1'
    | csvsel -tag PN -gc ref,alt,af -vs 1 [#dummybuck#] [#pns#] | map -c pn [#bucket#] | rename value gt | gtgen -gc ref,alt,af [#bucket#] <(gorrows -p chr1:1-2 | group chrom
    | calc pn '' | top 0);

    create #queen# = nor <(gor [#vars#] | distloc 100000 | queen [#bucket#] [#pns1#] [#pns2#] -vs 1 -minSharing -10) | select pn1-  | signature -timeres 100;

    create #king2# = nor <(gor [#vars#] | distloc 100000 | king2 -gc af [#bucket#] <(nor [#pns1#] | multimap -cartesian [#pns2#] ) -vs 1 ) | select pn1- | signature -timeres 11 | calc t time();

    create #king# = nor <(gor [#vars#] | distloc 100000 | king -gc af -maxvars 110000 [#bucket#] [#pns1#] <(nor [#pns2#] | top 1) -vs 1 ) | select pn1- | signature -timeres 11 | calc t time();

    create #kingSym# = nor <(gor [#vars#] | distloc 100001 | king -gc af -maxvars 110001 -sym [#bucket#] [#pns1#] [#pns2#]  -vs 1 ) | select pn1- | signature -timeres 11 | calc t time();

    create #kingJoin# = nor <(gor [#vars#] | distloc 1000
    | calc tpq 2*af*af*(1.0-af)*(1.0-af)
    | calc kpq 2.0*af*(1-af)
    | csvsel -gc ref,alt,tpq,kpq -vs 1 -u 3 -tag PN [#bucket#] [#pns1#]
    | where value != '3'
    | multimap -c pn <(nor [#rel#] | select id1,id2)
    | varjoin -norm -r -xl id2 -xr pn <(gor [#vars#]
      | csvsel -gc ref,alt -vs 1 -u 3 -tag PN [#bucket#] [#pns2#]
      | where value != '3')
    | calc values = str(value)+str(valuex)
    | calc IBS0 if(values='02' or values = '20',1,0)
    | calc XX if(values='01' or values = '10' or values = '21' or values = '12',1,if(values='02' or values = '20',4,0))
    | calc Nhet if(values = '11',1,0)
    | calc Nhom if(values = '02' or values = '20',1,0)
    | calc NAai if(left(values,1)='1',1,0)
    | calc NAaj if(right(values,1)='1',1,0)
    | group genome -gc pn,pnx -sum -fc IBS0,XX,tpq,kpq,Nhet,Nhom,NAai,NAaj -count
    | rename allcount count
    | rename pn pn1 | rename pnx pn2
    | rename sum_(.*pq) #{1}
    | replace sum_* int(float(#rc))
    | rename sum_(.*) #{1})
    | select pn1-
    | calc pi0 IBS0/float(tpq)
    | calc phi 0.5-float(XX)/float(4.0*kpq)
    | calc theta (Nhet-2.0*Nhom)/(NAai+NAaj)
    | calc t time();
    ;

    nor [#king2#] | calc file 'king2'
    | merge <(nor [#king#] | calc file 'king')
    | merge <(nor [#kingSym#] | calc file 'kingSym')
    | merge <(nor [#kingJoin#] | calc file 'kingJoin')
    | calc sortcol if(pn1>pn2,pn2+','+pn1,pn1+','+pn2)
    | sort -c sortcol,file
    | hide sortcol
    | replace tpq form(tpq,4,3)
    | replace kpq form(kpq,4,3)
    | replace phi form(phi,4,3)
    | replace theta form(theta,4,3)
    | rename t time()
    | top 100

Related commands
----------------

:ref:`KING2` :ref:`QUEEN` :ref:`CSVCC` :ref:`GTGEN` :ref:`GTLD`


