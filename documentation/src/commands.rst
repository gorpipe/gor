.. _quickReference:

Commands
========

.. list-table:: A list of available GOR and NOR commands with descriptions
   :widths: 3  15 1  1
   :header-rows: 1
   :class: wxnc-reference

   * - Command
     - Description
     - GOR
     - NOR
   * - :ref:`ATMAX`
     - Selects a single row based on a maximum value for a column.
     - .. image:: images/check.png
     -
   * - :ref:`ATMIN`
     - Selects a single row based on a minimum value for a column.
     - .. image:: images/check.png
     -
   * - :ref:`BAMFLAG`
     - Expands the FLAG bitmap column into multiple Boolean columns.
     - .. image:: images/check.png
     -
   * - :ref:`BASES`
     - Separates the sequence read into the individual bases in the read based on the ``SEQ`` column.
     - .. image:: images/check.png
     -
   * - :ref:`BUCKETSPLIT`
     - Split the content of a column into buckets based.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`CALC`
     - Add a calculated column to the output stream.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`CALCIFMISSING`
     - Add a calculated column to the output stream if it does not already exist.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`CIGARSEGS`
     - Separates the BAM-style sequence read into multiple reads based on the ``CIGAR`` column.
     - .. image:: images/check.png
     -
   * - :ref:`CMD`
     - Run an external operating system command from within NOR.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`COLNUM`
     - Prefixes cell data with the number of the column in the output from which the cell data comes.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`COLS2LIST`
     - Collapse multiple columns to one list value.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`COLSPLIT`
     - Split the content of a column based on a defined split separator.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`COLUMNSORT`
     - Reorder the columns putting the specified columns at the beginning of the column list.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`COLTYPE`
     - Prefixes cell data with the type of the column in the output from which the cell data comes.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`CSVCC`
     - Aggregates or counts genotypes stored in horizontal CSV format.
     - .. image:: images/check.png
     -
   * - :ref:`CSVSEL`
     - Selects a subset of data stored in a horizontal manner, as opposed to vertically in rows.
     - .. image:: images/check.png
     -
   * - :ref:`DAGMAP`
     - Similar to :ref:`MULTIMAP`, but topologically ordering individuals based on their family tree relations.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`DISTINCT`
     - Eliminates duplicate rows from the output stream.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`DISTLOC`
     - Similar to :ref:`TOP`, but counting distinct loci instead of individual rows.
     - .. image:: images/check.png
     -
   * - :ref:`GAVA`
     - Command for working with Gene Association
     - .. image:: images/check.png
     -
   * - :ref:`GOR`
     - The core source command for working with genomic-ordered relational data.
     - .. image:: images/check.png
     -
   * - :ref:`GORCMD`
     - Executes system commands that return tabular data within GOR queries.
     - .. image:: images/check.png
     -
   * - :ref:`GORINDEX`
     - Creates a gori index file for preexisting gorz files.
     - .. image:: images/check.png
     -
   * - :ref:`GORROW`
     - Returns a single row of data based on the input parameters.
     - .. image:: images/check.png
     -
   * - :ref:`GORROWS`
     - Generates genomic ordered rows based on the input parameters.
     - .. image:: images/check.png
     -
   * - :ref:`GORSQL`
     - Runs arbitrary commands against a database, which can be defined in a config file.
     - .. image:: images/check.png
     -
   * - :ref:`GRANNO`
     - Aggregation and annotation in a single pass.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`GREP`
     - Filter for column content based on a specified expression.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`GROUP`
     - Aggregation of data based on a specified binsize and content of specified columns.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`GTGEN`
     - Generate genotypes in a horizontal bucket format.
     - .. image:: images/check.png
     -
   * - :ref:`GTLD`
     - Calculate linkage disequilibrium between genotypes in different locations.
     - .. image:: images/check.png
     -
   * - :ref:`HIDE`
     - Removes the listed columns from the output stream.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`INSET`
     - Only passes rows where the column value is found in the specified single-column setfile.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`JOIN`
     - Joins multiple sources (or a single source to itself) based on some defined overlap condition.
     - .. image:: images/check.png
     -
   * - :ref:`LEFTJOIN`
     - A special type of join that returns all rows from the left source along with data from the right if any matches.
     - .. image:: images/check.png
     -
   * - :ref:`LEFTWHERE`
     - Supplies additional join conditions into a left-join operation.
     - .. image:: images/check.png
     -
   * - :ref:`LIFTOVER`
     - Converts GOR data from one reference genome build to another.
     - .. image:: images/check.png
     -
   * - :ref:`LOG`
     - Specifies how often to log rows when monitoring the progress of a running query.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`MAP`
     - Joins together tables using columns other than the chromosome and position data.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`MERGE`
     - Combines two independent sources into a single genomic-ordered stream.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`MULTIMAP`
     - Similar to :ref:`MAP`, but with a mapfile that may contain a one-to-many mapping.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`NOR`
     - The core source command for working with non-ordered relational data.
     -
     - .. image:: images/check.png
   * - :ref:`NORCMD`
     - Executes system commands that return tabular data within NOR queries.
     -
     - .. image:: images/check.png
   * - :ref:`NORROWS`
     - Returns a specified number rows of data in a NOR context based on the input parameters.
     -
     - .. image:: images/check.png
   * - :ref:`NORSQL`
     - Runs arbitrary commands against a database and the returned data can be used in a NOR context.
     -
     - .. image:: images/check.png
   * - :ref:`PARTGOR`
     - Runs queries in parallel, partitioning the query execution along the tag-partition axis.
     - .. image:: images/check.png
     -
   * - :ref:`PEDPIVOT`
     - Pivot a table using a pedigree file as a set of pivot points.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`PGOR`
     - Runs queries in parallel, partitioning the query execution along the genomic axis.
     - .. image:: images/check.png
     -
   * - :ref:`PILEUP`
     - Describes the base-pair formation at each chromosomal position and summarises the base calls.
     - .. image:: images/check.png
     -
   * - :ref:`PIPESTEPS`
     - Reads a number of analysis steps from a .yml file.
     - .. image:: images/check.png
     -
   * - :ref:`PIVOT`
     - Extracts information from large row-based data sets and maps the data into horizontal columns.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`PREFIX`
     - Adds a specified prefix to the listed columns.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`RANK`
     - Adds a rank column based on a specified numeric column and binsize.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`REGSEL`
     - Grabs values from a source column based on a specified matching expression.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`RENAME`
     - Renames a column in the output of a GOR or NOR query.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`REPLACE`
     - Similar to :ref:`CALC`, but replaces a specified column with the calculated values.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`ROWNUM`
     - Adds a column to the output stream with an auto-incrementing unique and sequential row number.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`SDL`
     - Runs arbitrary SDL commands against an SDL server and use the data in a NOR context.
     -
     - .. image:: images/check.png
   * - :ref:`SED`
     - A search and replace function on the output stream. Maybe to applied to only specified rows.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`SEGHIST`
     - Turns a stream of annotations into a stream of non-overlapping segments.
     - .. image:: images/check.png
     -
   * - :ref:`SEGPROJ`
     - Projects a stream of segments to provide a picture of the overlap between regions.
     - .. image:: images/check.png
     -
   * - :ref:`SEGSPAN`
     - Turns a stream of segments into a stream of non-overlapping segments.
     - .. image:: images/check.png
     -
   * - :ref:`SELECT`
     - Filters specified columns from the output stream.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`SEQ`
     - Returns the corresponding reference sequence read for each row of the output (based on the position).
     - .. image:: images/check.png
     -
   * - :ref:`SETCOLTYPE`
     - Sets column data types.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`SKIP`
     - Skips a specified number of rows before returning data.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`SORT`
     - Sorts the rows based on position in cases where the GOR condition has been violated.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`SPLIT`
     - Outputs multiple rows for columns that can be split based on a separator pattern.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`TEE`
     - Splits a GOR stream into two separate outputs based on a condition.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`THROWIF`
     - Throw an exception if the condition is satisfied.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`TOP`
     - Specify how many rows should be returned by the query.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`TRYHIDE`
     - Same as HIDE, but ignores errors generated from incorrect syntax.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`TRYSELECT`
     - Same as SELECT, but ignores errors generated from incorrect syntax.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`TRYWHERE`
     - Same as WHERE, but ignores errors generated from incorrect syntax.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`UNPIVOT`
     - Takes information in multiple rows and splits them into multiple rows as attribute-value pairs.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`UNTIL`
     - Terminates the stream when a condition is matched.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`VARIANTS`
     - Returns the variants found in sequence reads and their associated quality.
     - .. image:: images/check.png
     -
   * - :ref:`VARJOIN`
     - Joins with an additional constraint that the columns denoting the reference and alternative alleles are equal.
     - .. image:: images/check.png
     -
   * - :ref:`VARMERGE`
     - Ensures that overlapping variants are denoted in an equivalent manner.
     - .. image:: images/check.png
     -
   * - :ref:`VARNORM`
     - Normalises the variation data in a gor stream to the left or the right.
     - .. image:: images/check.png
     -
   * - :ref:`VERIFYCOLTYPE`
     - Ensures that the values in each row match their column type.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`VERIFYORDER`
     - Ensures that the genomic order of a GOR stream is correct, raising an exception if not.
     - .. image:: images/check.png
     -
   * - :ref:`WAIT`
     - Wait the specified number of milliseconds.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`WHERE`
     - Used to filter rows based on a specific conditional expression.
     - .. image:: images/check.png
     - .. image:: images/check.png
   * - :ref:`WRITE`
     - Used to write a stream into one or more files simultaneously.
     - .. image:: images/check.png
     - .. image:: images/check.png
