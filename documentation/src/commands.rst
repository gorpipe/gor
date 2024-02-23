.. _quickReference:

Commands
========

.. list-table:: A list of available GOR and NOR commands with descriptions
   :widths: 15  35 8
   :header-rows: 1
   :class: wxnc-reference

   * - Command
     - Description
     - Context
   * - :ref:`ATMAX`
     - Selects a single row based on a maximum value for a column.
     - GOR
   * - :ref:`ATMIN`
     - Selects a single row based on a minimum value for a column.
     - GOR
   * - :ref:`BAMFLAG`
     - Expands the FLAG bitmap column into multiple Boolean columns.
     - GOR
   * - :ref:`BASES`
     - Separates the sequence read into the individual bases in the read based on the ``SEQ`` column.
     - GOR
   * - :ref:`BUCKETSPLIT`
     - Split the content of a column into buckets based.
     - GOR/NOR
   * - :ref:`CALC`
     - Add a calculated column to the output stream.
     - GOR/NOR
   * - :ref:`CALCIFMISSING`
     - Add a calculated column to the output stream if it does not already exist.
     - GOR/NOR
   * - :ref:`CIGARSEGS`
     - Separates the BAM-style sequence read into multiple reads based on the ``CIGAR`` column.
     - GOR
   * - :ref:`CMD`
     - Run an external operating system command from within NOR.
     - GOR/NOR
   * - :ref:`COLNUM`
     - Prefixes cell data with the number of the column in the output from which the cell data comes.
     - GOR/NOR
   * - :ref:`COLS2LIST`
     - Collapse multiple columns to one list value.
     - GOR/NOR
   * - :ref:`COLSPLIT`
     - Split the content of a column based on a defined split separator.
     - GOR/NOR
   * - :ref:`COLUMNREORDER`
     - Reorder the columns by specifying source column and target column.
     - GOR/NOR
   * - :ref:`COLUMNSORT`
     - Reorder the columns putting the specified columns at the beginning of the column list.
     - GOR/NOR
   * - :ref:`COLTYPE`
     - Prefixes cell data with the type of the column in the output from which the cell data comes.
     - GOR/NOR
   * - :ref:`CSVCC`
     - Aggregates or counts genotypes stored in horizontal CSV format.
     - GOR
   * - :ref:`CSVSEL`
     - Selects a subset of data stored in a horizontal manner, as opposed to vertically in rows.
     - GOR
   * - :ref:`DAGMAP`
     - Similar to :ref:`MULTIMAP`, but topologically ordering individuals based on their family tree relations.
     - GOR/NOR
   * - :ref:`DEFLATECOLUMN`
     - Compresses a column which meets minimum size requirement.
     - GOR/NOR
   * - :ref:`DISTINCT`
     - Eliminates duplicate rows from the output stream.
     - GOR/NOR
   * - :ref:`DISTLOC`
     - Similar to :ref:`TOP`, but counting distinct loci instead of individual rows.
     - GOR
   * - :ref:`FILTERINVALIDROWS`
     - Filters out invalid rows, e.g. rows missing columns.
     - GOR/NOR
   * - :ref:`GAVA`
     - Command for working with Gene Association
     - GOR
   * - :ref:`GOR`
     - The core source command for working with genomic-ordered relational data.
     - GOR
   * - :ref:`GORCMD`
     - Executes system commands that return tabular data within GOR queries.
     - GOR
   * - :ref:`GORROW`
     - Returns a single row of data based on the input parameters.
     - GOR
   * - :ref:`GORROWS`
     - Generates genomic ordered rows based on the input parameters.
     - GOR
   * - :ref:`GORSQL`
     - Runs arbitrary commands against a database, which can be defined in a config file.
     - GOR
   * - :ref:`GRANNO`
     - Aggregation and annotation in a single pass.
     - GOR/NOR
   * - :ref:`GREP`
     - Filter for column content based on a specified expression.
     - GOR/NOR
   * - :ref:`GROUP`
     - Aggregation of data based on a specified binsize and content of specified columns.
     - GOR/NOR
   * - :ref:`GTGEN`
     - Generate genotypes in a horizontal bucket format.
     - GOR
   * - :ref:`GTLD`
     - Calculate linkage disequilibrium between genotypes in different locations.
     - GOR
   * - :ref:`GTTRANSPOSE`
     - Generates one column named PNs and either one another column named VALUES, containing the genotypes of the pn by marker in the order defined by the marker source.
     - GOR
   * - :ref:`HIDE`
     - Removes the listed columns from the output stream.
     - GOR/NOR
   * - :ref:`INFLATECOLUMN`
     - De-compresses a column which has been compressed using :ref:`DEFLATECOLUMN`.
     - GOR/NOR
   * - :ref:`INSET`
     - Only passes rows where the column value is found in the specified single-column setfile.
     - GOR/NOR
   * - :ref:`JOIN`
     - Joins multiple sources (or a single source to itself) based on some defined overlap condition.
     - GOR
   * - :ref:`KING`
     - Calculates relationship statistics from a stream of horizontal genotypes.
     - GOR
   * - :ref:`LEFTJOIN`
     - A special type of join that returns all rows from the left source along with data from the right if any matches.
     - GOR
   * - :ref:`LEFTWHERE`
     - Supplies additional join conditions into a left-join operation.
     - GOR
   * - :ref:`LIFTOVER`
     - Converts GOR data from one reference genome build to another.
     - GOR
   * - :ref:`LOG`
     - Specifies how often to log rows when monitoring the progress of a running query.
     - GOR/NOR
   * - :ref:`MAP`
     - Joins together tables using columns other than the chromosome and position data.
     - GOR/NOR
   * - :ref:`METAINFO`
     - Allows querying of meta data associated with a data source.
     - GOR/NOR
   * - :ref:`MERGE`
     - Combines two independent sources into a single genomic-ordered stream.
     - GOR/NOR
   * - :ref:`MULTIMAP`
     - Similar to :ref:`MAP`, but with a mapfile that may contain a one-to-many mapping.
     - GOR/NOR
   * - :ref:`NOR`
     - The core source command for working with non-ordered relational data.
     - NOR only
   * - :ref:`NORCMD`
     - Executes system commands that return tabular data within NOR queries.
     - NOR only
   * - :ref:`NORROWS`
     - Returns a specified number rows of data in a NOR context based on the input parameters.
     - NOR
   * - :ref:`NORSQL`
     - Runs arbitrary commands against a database and the returned data can be used in a NOR context.
     - NOR
   * - :ref:`PARTGOR`
     - Runs queries in parallel, partitioning the query execution along the tag-partition axis.
     - GOR
   * - :ref:`PEDPIVOT`
     - Pivot a table using a pedigree file as a set of pivot points.
     - GOR/NOR
   * - :ref:`PGOR`
     - Runs queries in parallel, partitioning the query execution along the genomic axis.
     - GOR
   * - :ref:`PILEUP`
     - Describes the base-pair formation at each chromosomal position and summarises the base calls.
     - GOR
   * - :ref:`PIPESTEPS`
     - Reads a number of analysis steps from a .yml file.
     - GOR
   * - :ref:`PIVOT`
     - Extracts information from large row-based data sets and maps the data into horizontal columns.
     - GOR/NOR
   * - :ref:`QUEEN`
     - Calculates relationship statistics from a stream of horizontal genotypes.
     - GOR
   * - :ref:`PREFIX`
     - Adds a specified prefix to the listed columns.
     - GOR/NOR
   * - :ref:`RANK`
     - Adds a rank column based on a specified numeric column and binsize.
     - GOR/NOR
   * - :ref:`REGSEL`
     - Grabs values from a source column based on a specified matching expression.
     - GOR/NOR
   * - :ref:`RELREMOVE`
     - Removes related samples/individuals (PNs) from a phenotype relation.
     - NOR
   * - :ref:`RENAME`
     - Renames a column in the output of a GOR or NOR query.
     - GOR/NOR
   * - :ref:`REPLACE`
     - Similar to :ref:`CALC`, but replaces a specified column with the calculated values.
     - GOR/NOR
   * - :ref:`ROWNUM`
     - Adds a column to the output stream with an auto-incrementing unique and sequential row number.
     - GOR/NOR
   * - :ref:`SDL`
     - Runs arbitrary SDL commands against an SDL server and use the data in a NOR context.
     - NOR
   * - :ref:`SED`
     - A search and replace function on the output stream. Maybe to applied to only specified rows.
     - GOR/NOR
   * - :ref:`SEGHIST`
     - Turns a stream of annotations into a stream of non-overlapping segments.
     - GOR
   * - :ref:`SEGPROJ`
     - Projects a stream of segments to provide a picture of the overlap between regions.
     - GOR
   * - :ref:`SEGSPAN`
     - Turns a stream of segments into a stream of non-overlapping segments.
     - GOR
   * - :ref:`SEGWHERE`
     - Turns a stream of segments into a stream of non-overlapping segments based on a where condition.
     - GOR
   * - :ref:`SELECT`
     - Filters specified columns from the output stream.
     - GOR/NOR
   * - :ref:`SELWHERE`
     - Filters specified columns from the output stream using an expression on column names/indices.
     - GOR/NOR
   * - :ref:`SEQ`
     - Returns the corresponding reference sequence read for each row of the output (based on the position).
     - GOR
   * - :ref:`SETCOLTYPE`
     - Sets column data types.
     - GOR/NOR
   * - :ref:`SKIP`
     - Skips a specified number of rows before returning data.
     - GOR/NOR
   * - :ref:`SORT`
     - Sorts the rows based on position in cases where the GOR condition has been violated.
     - GOR/NOR
   * - :ref:`SPLIT`
     - Outputs multiple rows for columns that can be split based on a separator pattern.
     - GOR/NOR
   * - :ref:`TEE`
     - Splits a GOR stream into two separate outputs based on a condition.
     - GOR/NOR
   * - :ref:`THROWIF`
     - Throw an exception if the condition is satisfied.
     - GOR/NOR
   * - :ref:`TOP`
     - Specify how many rows should be returned by the query.
     - GOR/NOR
   * - :ref:`TRYHIDE`
     - Same as HIDE, but ignores errors generated from incorrect syntax.
     - GOR/NOR
   * - :ref:`TRYSELECT`
     - Same as SELECT, but ignores errors generated from incorrect syntax.
     - GOR/NOR
   * - :ref:`TRYWHERE`
     - Same as WHERE, but ignores errors generated from incorrect syntax.
     - GOR/NOR
   * - :ref:`UNPIVOT`
     - Takes information in multiple rows and splits them into multiple rows as attribute-value pairs.
     - GOR/NOR
   * - :ref:`UNTIL`
     - Terminates the stream when a condition is matched.
     - GOR/NOR
   * - :ref:`VARGROUP`
     - Groups together variants with the same reference allele at a given position.
     - GOR
   * - :ref:`VARIANTS`
     - Returns the variants found in sequence reads and their associated quality.
     - GOR
   * - :ref:`VARJOIN`
     - Joins with an additional constraint that the columns denoting the reference and alternative alleles are equal.
     - GOR
   * - :ref:`VARMERGE`
     - Ensures that overlapping variants are denoted in an equivalent manner.
     - GOR
   * - :ref:`VARNORM`
     - Normalises the variation data in a gor stream to the left or the right.
     - GOR
   * - :ref:`VERIFYCOLTYPE`
     - Ensures that the values in each row match their column type.
     - GOR/NOR
   * - :ref:`VERIFYORDER`
     - Ensures that the genomic order of a GOR stream is correct, raising an exception if not.
     - GOR
   * - :ref:`VERIFYVARIANT`
     - Ensures that the reference column corresponds to the configured build, raising an exception if not.
     - GOR
   * - :ref:`WAIT`
     - Wait the specified number of milliseconds.
     - GOR/NOR
   * - :ref:`WHERE`
     - Used to filter rows based on a specific conditional expression.
     - GOR/NOR
   * - :ref:`WRITE`
     - Used to write a stream into one or more files simultaneously.
     - GOR/NOR

