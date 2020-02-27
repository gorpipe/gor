========
Pivoting
========
In this chapter we will discuss how to pivot data in GOR queries. Loosely speaking, pivot commands in GOR perform a partial transpose operation. :ref:`PIVOT` turns rows into columns and :ref:`UNPIVOT`  turns columns into rows.

More specifically, pivot aggregates rows and turns multiple rows into multiple columns on the same row. :ref:`PIVOT` is useful when we have the same item (defined by some composite key, such as ``chrom+pos+reference+call`` for variants) occurring multiple times with different values of the pivot column (such as *family role* values of father, mother, :term:`proband` or sibling) and we want to use the value of that pivot column to control some other logic (such as considering the sequencing depth of the parent samples differently than we consider the sequencing depth of a proband sample).

Note that when we are pivoting a table, we need to know in advance what the possible values for the pivot column are. In the following example, we are first generating the data that we will use to pivot from the structure of the PNs and then pivoting on that generated data. For the purposes of this example, we are considering a set of samples, where the PN contains information about the family role that can be extracted using **IF** statements.

.. code-block:: gor

	/* Example of PIVOT specifically for the Simons Foundation project */
   gor #wesvars#
   | select chrom,pos,reference,call,depth,callcopies,PN
   | where PN ~ 'TO2_12324.*' | top 10
   | calc family_role  /* IF( predicate, IF_TRUE, IF(...) )
      if (PN ~ '^.*FA$', 'FATHER',
      if (PN ~ '^.*MO$', 'MOTHER',
      if (PN ~ '^.*P.$', 'PROBAND',
      if (PN ~ '^.*S.$', 'SIBLING',
      'UNKNOWN'))))
   | hide PN
   | pivot -gc reference,call family_role -v FATHER,MOTHER,SIBLING,PROBAND -e 0

Because we are in a gor context, the chrom and pos columns are inherently in the composite key. To use an even more basic example, in the following query we are annotating variants with a random color label, aggregating the stream based on the variant data (reference and call) and the color label, and then using the label to pivot the data.

.. code-block:: gor

   gor #wesvars# | top 10
   | select chrom,pos,reference,call
   | calc label if(random()<0.5,'red','green')
   | group 1 -gc reference,call,label -count
   | pivot label -v red,green -gc reference,call -e 0

This result will give us a unique row for each variant with two separate columns for the count of "red" and "green" labels for those variants. The images below show the stream of gor rows before and after the pivot.

.. figure:: images/before_pivot.png
   :scale: 100 %

   The variants with color labels before pivoting

.. figure:: images/after_pivot.png
   :scale: 100 %

   The variants with color labels after pivoting

Unpivoting rows in GOR
======================

:ref:`UNPIVOT` takes a single row and breaks it into multiple rows, showing the name and value of the columns that were unpivoted. This can be useful when we have two groups of items (such as variants) and we want to understand exactly what attributes differ between the left and right sources. By unpivoting, we can then do a join and aggregation to identify attributes that were the same or to identify attributes that differed between the two tables.

The example below takes six columns from the ``#wesvars#`` table and separates the stream into separate rows for the callcopies and depth columns.

.. code-block:: gor

   gor #wesvars#
   | top 100
   | select chrom,pos,reference,call,callcopies,depth
   | unpivot callcopies,depth

You'll notice that if the input stream to the unpivot command has X number of rows, the output should have N*X number of rows, where N is the number of columns that got unpivoted.

In the following example, we are comparing two BAM files. Note that by unpivoting from column 4 and onwards and then joining on columns 1-3 and col_name, we are defining the composite key for both the unpivoting and also the joining to be the value of columns 1, 2 and 3. It is crucial to choose an appropriate composite key during these unpivot and join steps in order to achieve a meaningful result.

.. code-block:: gor

   gor bam1
   | unpivot 4-
   | join -n -snpsnp -xl end,col_name -xr end,col_name <(gor bam2 | unpivot 4-)
   | calc label "bam1"
   | merge <(gor bam2 | unpivot 4-
   | join -n -snpsnp -xl end,col_name -xr end,col_name <(gor bam1 | unpivot 4-
   | calc label "bam1")

