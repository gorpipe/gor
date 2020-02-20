.. _groupingAggregation:

========================
Grouping and Aggregation
========================
The GOR syntax provides two commands to do grouping and aggregation, the :ref:`GROUP` command and the :ref:`GRANNO` command, which is for aggregating and annotating in a single pass.


Grouping
========
The :ref:`GROUP` command allows you aggregate your results similarly to the way grouping works in SQL, although the syntax is slightly different. In the GOR language, grouping is always based on a sliding genomic bin size.

For example, the following GOR query calculates the number of PNs with vars in chr21, i.e. the number of variants per pn and then the number of PNs.

.. code-block:: gor

   gor -p chr21 #wgsvars# | GROUP chrom -gc pn -count | GROUP chrom -count

We will now look at a few different types of grouping in the GOR query language.


Defining the Binsize
====================
When performing a :ref:`GROUP` command, the query can be set to group based on an integer to denote a range (in base pairs) or it can be set to chromosome or genome. The following query returns the ``#dbsnp#`` variant table grouped by chromosome with a count of how many SNPs are on each (shown in the allCount column):

.. code-block:: gor

   gor #dbsnp# | GROUP chrom -count

.. list-table:: #dbsnp# grouped by chromosome
   :widths: 5  5  10 10
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - allCount
   * - chr1
     - 0
     - 249250621
     - 18642860
   * - chr2
     - 0
     - 243199373
     - 20216254
   * - chr3
     - 0
     - 198022430
     - 16557941
   * - chr4
     - 0
     - 191154276
     - 16044444
   * - chr5
     - 0
     - 180915260
     - 14938502
   * - chr6
     - 0
     - 171115067
     - 14143210
   * - ...
     - ...
     - ...
     - ...



..   * - chr7
     - 0
     - 159138663
     - 13384493
   * - chr8
     - 0
     - 146364022
     - 12951034
   * - chr9
     - 0
     - 141213431
     - 10380512
   * - chr10
     - 0
     - 135534747
     - 11151498
   * - chr11
     - 0
     - 135006516
     - 11499951
   * - chr12
     - 0
     - 133851895
     - 11115933
   * - chr13
     - 0
     - 115169878
     - 8001496
   * - chr14
     - 0
     - 107349540
     - 7537524
   * - chr15
     - 0
     - 102531392
     - 6900201
   * - chr16
     - 0
     - 90354753
     - 7863983
   * - chr17
     - 0
     - 81195210
     - 6896845
   * - chr18
     - 0
     - 78077248
     - 6338217
   * - chr19
     - 0
     - 59128983
     - 5604169
   * - chr20
     - 0
     - 63025520
     - 5238905
   * - chr21
     - 0
     - 48129895
     - 3175264
   * - chr22
     - 0
     - 51304566
     - 3262023
   * - chrM
     - 0
     - 16571
     - 2737
   * - chrX
     - 0
     - 155270560
     - 8287745
   * - chrY
     - 0
     - 59373566
     - 266875


Another way to get the total number of SNPs on chromosome 1, would be to use the following GOR query:

.. code-block:: gor

   gor -p chr1 #dbsnp# | GROUP genome -count

Since we made the grouping over the genome, a special segment named ``chrA`` (chromosome all) is returned.

To further illustrate the use of binsize, we could then take only chromosome 21 (using the position filter on the GOR query) and group with a binsize of 1 million basepairs, as shown below:

.. code-block:: gor

   gor -p chr21 #dbsnp# | GROUP 10000000 -count

.. list-table:: #dbsnp# chr21 grouped into buckets of 10m bp
   :widths: 5  5  10 10
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - allCount
   * - chr21
     - 0
     - 10000000
     - 38990
   * - chr21
     - 10000000
     - 20000000
     - 607009
   * - chr21
     - 20000000
     - 30000000
     - 889141
   * - chr21
     - 30000000
     - 40000000
     - 857939
   * - chr21
     - 40000000
     - 50000000
     - 782185

The above example shows how many SNPs exist in each segment of 1m base pairs on chromosome 21.

It is important to note that the binsize can directly impact the performance of your queries. It is advisable to limit the binsize to as small a number as possible (i.e. do not use the "genome" binsize when the "chrom" will do). Often you can limit the binsize to 3 megabases, which is longer than the longest human gene.


Introducing Parallel Queries
----------------------------
The query above that shows a :ref:`GROUP` command that returns the number of SNPs on each chromosome, but that query takes a very long time to run because it goes through the genome chromosome by chromosome. A better way to do this is using :ref:`PGOR`. If we were to write the query as follows:

.. code-block:: gor

   pgor #dbsnp# | GROUP chrom -count

the query will automatically be split up and will return the count for each chromosome much faster. A full discussion of parallelization can be found in a later chapter specifically on :ref:`Parallel GOR<parallelGOR>`.


Grouping on Columns
===================
We can specify additional grouping by using the ``-gc`` option, which stands for "grouping column". To understand this type of group, we could take the same example of the 21st chromosome in the ``#dbsnp#``, but in this case we would only look at substitutions as in the table shown below:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1

.. list-table:: #dbsnp# chr21 - substitutions only
   :widths: 5  10 5  5  15
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - rsIDs
   * - chr21
     - 9411199
     - T
     - C
     - rs376129767
   * - chr21
     - 9411236
     - G
     - A
     - rs922165264
   * - chr21
     - 9411239
     - G
     - A
     - rs559462325
   * - chr21
     - 9411242
     - C
     - A
     - rs531773366
   * - chr21
     - 9411243
     - A
     - C
     - rs191612142


Next, we might want to use the :ref:`CALC` command to create a new column with the type of SNP in each substitution, as shown below:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs

.. list-table:: #dbsnp# chr21 with SNP type
   :widths: 5  5 5  5  5
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - snptype
   * - chr21
     - 9411199
     - T
     - C
     - T/C
   * - chr21
     - 9411236
     - G
     - A
     - G/A
   * - chr21
     - 9411239
     - G
     - A
     - G/A
   * - chr21
     - 9411242
     - C
     - A
     - C/A
   * - chr21
     - 9411243
     - A
     - C
     - A/C


Using the ``-gc`` option on the :ref:`GROUP` command, we could then group based on this new ``snptype`` column using the GOR query shown here:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs | GROUP chrom -count -gc snptype

.. list-table:: #dbsnp# chr21: SNP types grouped over the chromosome
   :widths: 5  5  5 5 5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - snptype
     - allCount
   * - chr21
     - 0
     - 48129895
     - A/C
     - 117055
   * - chr21
     - 0
     - 48129895
     - A/G
     - 399872
   * - chr21
     - 0
     - 48129895
     - A/T
     - 109699
   * - chr21
     - 0
     - 48129895
     - C/A
     - 152813
   * - chr21
     - 0
     - 48129895
     - C/G
     - 133850

As before, we could also define the binsize as a specific number of base pairs, in which case the output of the query would give us the number of each type of SNP in each interval we define in the binsize. In the GOR query below, we are grouping the SNP types with a bin size of 1m base pairs.

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs | GROUP 1000000 -count -gc snptype

The next example shows how we could calculate the number of PNs with variants in chr21.

.. code-block:: gor

   gor -p chr21 #wgsvars# | GROUP chrom -gc PN -count | GROUP chrom -count

The example also shows a good example of using the GROUP command twice in a query, first to count the number of variants per PN and then to count the number of PNs.

As we will see in the next section, we can then use another :ref:`GROUP` command to perform various statistical calculations in the query.


Calculating Max, Min, Avgs
==========================
We can use some options in the :ref:`GROUP` command to calculate the maximum, minimum, and average number of each type of SNP in each 1 megabase binsize as is shown in the following example. Note that you must specify the type (in this case ``-ic`` for integer column) and name of the column (or multiple columns, comma delimted) that you wish to perform the calculations for.

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | CALC snptype reference+'/'+allele | HIDE rsIDs | GROUP 100000 -count -gc snptype
   | GROUP chrom -max -min -avg -ic allCount -gc snptype

.. list-table:: #dbsnp# chr21: SNP types (max, min, avg)
   :widths: 5  5  5 5 5 5  5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - snptype
     - min_allCount
     - max_allCount
     - avg_allCount
   * - chr21
     - 0
     - 48129895
     - A/C
     - 37
     - 936
     - 327.885154
   * - chr21
     - 0
     - 48129895
     - A/G
     - 66
     - 2337
     - 1120.08963
   * - chr21
     - 0
     - 48129895
     - A/T
     - 24
     - 1279
     - 307.28011
   * - chr21
     - 0
     - 48129895
     - C/A
     - 61
     - 1422
     - 428.04761
   * - chr21
     - 0
     - 48129895
     - C/G
     - 39
     - 1055
     - 374.92997
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...
     - ...

As you can see in the table above, the calculated columns are prefixed with the type of calculation that has been performed in each case.


Using GRANNO to Annotate
========================
The :ref:`GRANNO` command allows you to group and annotate in a single pass. A full description of the :ref:`GRANNO` command can be found :ref:`here<GRANNO>`.

To illustrate the use of the **GRANNO** command, we could take a look at the previous example. Let's say that instead of grouping over the whole chromosome, we might want to find out how many different SNPs there are, on average, on each gene. The ``#dbsnp#`` does not have gene data, so we will have to join to either the ``#genes#`` or ``#exons#`` table and group on the ``gene_symbol`` column, as shown in the example below:

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | JOIN -snpseg #exons# | CALC snptype reference+'/'+allele
   | HIDE rsIDs | GROUP chrom -gc gene_symbol -count

Note that in the example we show here, we are hiding certain columns to sculpt the output of the query to include only those columns relevant to the example.

.. list-table:: Aggregation of SNPs for each Gene
   :widths: 5  5  5 5 5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - gene_symbol
     - allCount
   * - chr21
     - 0
     - 48129895
     - 7SK
     - 107
   * - chr21
     - 0
     - 48129895
     - ABCC13
     - 1654
   * - chr21
     - 0
     - 48129895
     - ABCG1
     - 5180
   * - chr21
     - 0
     - 48129895
     - ADAMTS1
     - 1955
   * - chr21
     - 0
     - 48129895
     - ADARB1
     - 1103
   * - chr21
     - 0
     - 48129895
     - ADAMTS5
     - 5693
   * - ...
     - ...
     - ...
     - ...
     - ...


However, as you can see above, this only gives us the start and stop position for the chromosome itself and not the exon in question. We may want to have this data genomic-ordered and so we need to add in the ``chromstart`` and ``chromend`` columns from the ``#exons#`` table.

.. code-block:: gor

   gor -p chr21 #dbsnp# | WHERE len(reference)=1 AND len(allele)=1
   | JOIN -snpseg #exons# | CALC snptype reference+'/'+allele
   | HIDE rsIDs | GROUP chrom -gc gene_symbol -count

.. list-table:: Aggregation of SNPs for each gene with position information
   :widths: 5  5  5 5 5 5  5
   :header-rows: 1

   * - Chrom
     - bpStart
     - bpStop
     - chromstart
     - chromend
     - gene_symbol
     - allCount
   * - chr21
     - 0
     - 48129895
     - 10199942
     - 10200025
     - CR381653.1
     - 9
   * - chr21
     - 0
     - 48129895
     - 10380379
     - 10380661
     - RN7SL52P
     - 13
   * - chr21
     - 0
     - 48129895
     - 10385952
     - 10386047
     - SNORA70
     - 9
   * - chr21
     - 0
     - 48129895
     - 10475514
     - 10476061
     - bP-21201H5.1
     - 57
   * - chr21
     - 0
     - 48129895
     - 10862621
     - 10862667
     - IGHV1OR21-1
     - 64
   * - chr21
     - 0
     - 48129895
     - 10862750
     - 10863057
     - IGHV1OR21-1
     - 243
   * - chr21
     - 0
     - 48129895
     - 10862750
     - 10863067
     - IGHV1OR21-1
     - 250
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...
     - ...


As you can above, this gives us something closer to the genomic ordered stream we are looking for, but there are still multiple entries for individual genes (since they sometimes overlap with multiple exonic regions). To remedy this, we can use the :ref:`GRANNO` command, which can group and annotate in a single pass.

In the next example, we use the :ref:`GRANNO` command and we can define the binsize as a special value of ``gene``, which essentially sets the value to 3M base pairs in conjunction with the ``-range`` option. This means that we will not look for any grouping of the gene_symbol column (indicated with the ``-gc`` option) outside of a range of 3M base pairs. Next, we define the annotation columns that we wish to generate (with the ``-ic`` option) and what types of annotations we wish to perform (in this case, ``-max`` and ``-min``).

.. code-block:: gor

   gor -p chr21 #dbsnp# | JOIN -snpseg #exons#
   | WHERE len(reference)=1 AND len(allele)=1 | CALC snptype reference+'/'+allele
   | GRANNO gene -range -gc gene_symbol -ic chromstart,chromend -max -min
   | SELECT 1-4,chromstart,chromend,gene_symbol,snptype-

.. list-table:: Aggregation of SNPs for each gene with GRANNO command
   :header-rows: 1

   * - Chrom
     - POS
     - reference
     - allele
     - chromstart
     - chromend
     - gene_symbol
     - snptype
     - min_chromstart
     - max_chromstart
     - min_chromend
     - max_chromend
   * - chr21
     - 9683195
     - G
     - A
     - 9683190
     - 9683272
     - CR381670.1
     - G/A
     - 9683190
     - 9683190
     - 9683272
     - 9683272
   * - chr21
     - 9683199
     - C
     - G
     - 9683190
     - 9683272
     - CR381670.1
     - C/G
     - 9683190
     - 9683190
     - 9683272
     - 9683272
   * - chr21
     - 9683201
     - G
     - T
     - 9683190
     - 9683272
     - CR381670.1
     - G/T
     - 9683190
     - 9683190
     - 9683272
     - 9683272


We can then further group this result over the chromosome (with a :ref:`GROUP` command) on the columns ``min_chromstart``, ``max_chromend``, ``gene_symbol``,  and ``snptype``, as shown in the example below:

.. code-block:: gor

   gor -p chr21 #dbsnp# | JOIN -snpseg #exons#
   | WHERE len(reference)=1 AND len(allele)=1 | CALC snptype reference+'/'+allele
   | GRANNO gene -range -gc gene_symbol -ic chromstart,chromend -max -min
   | GROUP chrom -gc min_chromstart,max_chromend,gene_symbol,snptype -count
   | SELECT Chrom,min_chromstart- | SORT chrom

.. list-table:: Aggregation of SNPs using both GRANNO and GROUP, genomic-ordered stream
   :widths: 5  5  5 5 5 5
   :header-rows: 1

   * - Chrom
     - min_chromstart
     - max_chromend
     - gene_symbol
     - snptype
     - allCount
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - A/C
     - 1
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - C/G
     - 1
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - C/T
     - 2
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - G/A
     - 1
   * - chr21
     - 9683190
     - 9683272
     - CR381670.1
     - G/T
     - 3
   * - ...
     - ...
     - ...
     - ...
     - ...
     - ...

There is a lot going on here, so we can take it line by line. The first line is the join between the tables. The second line does the filtering on the left source and adds the calculated column. The third line runs the :ref:`GRANNO` command as described above, grouping and annotating in a single step to find the ``min_chromstart`` and ``max_chromend`` for the genes. Next we group over the whole chromosome to get the count of each type of SNP. Finally, we :ref:`SELECT` the columns we want to display.

You'll note at the end of this query that we have added a **SORT** command to make sure that the genomic order is preserved in the GOR stream. As discussed in a previous section :ref:`on genomic order<genomicOrder>`, this is often necessary when columns are being moved around so much within a GOR query.