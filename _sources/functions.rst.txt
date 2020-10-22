.. _predefinedFunctions:

=========
Functions
=========

The following tables contain all of the functions that can be used in WHERE and CALC expressions in GOR queries.

+------------------------------------------+--------------------------------------------------------------------------------------------+
| ``EVAL(string) : string``                | Returns the result of a GOR/NOR query                                                      |
+------------------------------------------+--------------------------------------------------------------------------------------------+

Boolean Functions
=================

Certain functions in GOR can be used to return a Boolean result (i.e. 0 for false, 1 for true) or a conditional result. By their nature, Boolean-type functions cannot be used with all CALC and WHERE commands (as other functions can). For example, the IF function returns one of two values based on a condition set in the function. As a result, IF can only be used with CALC commands.

.. list-table:: Boolean functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``IF(cond,any,any) : any``
     - Returns the second parameter if the condition in the first parameter is true, otherwise the third parameter.
     - :ref:`if`
   * - ``ISINT(col) : bool``
     - Test if a column has integer value.
     - :ref:`isint`
   * - ``ISFLOAT(col) : bool``
     - Test if a column has floating point value.
     - :ref:`isfloat`
   * - ``LISTHASANY(str,lit-list) : bool``
     - Returns true if the comma separated list has an element in the lit-list.
     - :ref:`containsany`
   * - ``CSLISTHASANY(str,lit-list) : bool``
     - Case-sensitive version of :ref:`containsany`
     - :ref:`containsany`
   * - ``CONTAINS(str,lit-list) : bool``
     - Returns true if the string contains all the string literals in the list. Not case-sensitive.
     - :ref:`contains`
   * - ``CONTAINSALL(str,lit-list) : bool``
     - Returns true if the string contains all the string literals in the list. Not case-sensitive. Same as :ref:`contains`.
     - :ref:`contains`
   * - ``CONTAINSANY(str,lit-list) : bool``
     - Returns true if the string contains any of the string literals in the list. Not case-sensitive. Same as :ref:`contains`.
     - :ref:`contains`
   * - ``CSCONTAINS(str,lit-list) : bool``
     - Case-sensitive version of :ref:`CONTAINS`.
     - :ref:`contains`
   * - ``CSCONTAINSALL(str,lit-list) : bool``
     - Returns true if the string contains all the string literals in the list. Not case-sensitive. Same as :ref:`contains`.
     - :ref:`contains`
   * - ``CSCONTAINSANY(str,lit-list) : bool``
     - Returns true if the string contains any of the string literals in the list. Not case-sensitive. Same as :ref:`contains`.
     - :ref:`contains`

Type and String Conversion
==========================

.. list-table:: Type and string conversion in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``STR(num) : str``
     - Convert number to string.
     - :ref:`str`
   * - ``STRING(num) : str``
     - Same as :ref:`str`.
     - :ref:`str`
   * - ``FLOAT(str) : float``
     - Convert string to float.
     - :ref:`float`
   * - ``FLOAT(str, float) : float``
     - Convert string to float with default value.
     - :ref:`float`
   * - ``NUMBER(str) : float``
     - Same as :ref:`float`.
     - :ref:`float`
   * - ``INT(str) : int``
     - Convert string to integer.
     - :ref:`int`
   * - ``BASE26(num) : str``
     - Converts an integer number to base 26 representation.
     - :ref:`base26`
   * - ``BASEPN(num) : str``
     - Converts an integer number to PN-like ID, e.g. AAAAAAA
     - :ref:`basepn`
   * - ``LEN(str) : int``
     - The length of the string.
     - :ref:`len`
   * - ``REVERSE(str) : str``
     - The string reversed.
     - :ref:`reverse`
   * - ``TRIM(str) : str``
     - Trims whitespace from the beginning and end of a string.
     - :ref:`trim`
   * - ``MD5(str) : str``
     - The MD5 message-digest of the string.
     - :ref:`md5`
   * - ``IOOA(str) : int``
     - In order of appearance, i.e. returns an integer number based on the order in which str value shows up.
     - :ref:`iooa`
   * - ``UPPER(str) : str``
     - Upper-case a string.
     - :ref:`upper`
   * - ``LOWER(str) : str``
     - Lower-case a string.
     - :ref:`lower`
   * - ``LEFT(str,int) : str``
     - Return the specified left-most characters in a string.
     - :ref:`left`
   * - ``RIGHT(str,int) : str``
     - Return the specified right-most characters in a string.
     - :ref:`right`
   * - ``SUBSTR(str,int,int) : str``
     - A substring.  A zero-based range, not including the last position.
     - :ref:`substr`
   * - ``MID(str,int,int) : str``
     - A substring.  A zero-based starting position and size.
     - :ref:`mid`
   * - ``REPLACE(str,str,str) : str``
     - Replace from a string a pattern with the new string.
     - :ref:`f_replace`
   * - ``POSOF(str,str) : int``
     - The position within a string of a string.  -1 if not found.
     - :ref:`posof`
   * - ``BRACKETS(string) : string``
     - Adds brackets around the string, e.g. LISTMAP(LISTZIP(col1,col2),'BRACKETS(x)').
     - :ref:`brackets`
   * - ``UNBRACKET(string) : string``
     - Removes surrounding brackets from a string.
     - :ref:`unbracket`
   * - ``SQUOTE(string) : string``
     - Adds single quotes around the string, e.g. LISTMAP(LISTZIP(col1,col2),'SQUOTE(x)').
     - :ref:`squote`
   * - ``SUNQUOTE(string) : string``
     - Removes surrounding single quotes from a string.
     - :ref:`sunquote`
   * - ``DQUOTE(string) : string``
     - Adds double quotes around the string, e.g. LISTMAP(LISTZIP(col1,col2),'DQUOTE(x)').
     - :ref:`dquote`
   * - ``DUNQUOTE(string) : string``
     - Removes surrounding double quotes from a string.
     - :ref:`dunquote`
   * - ``FORM(num,int,int) : str``
     - Format a number, specifying the space and decimals (C-style %x.yf format style).
     - :ref:`form`
   * - ``REGSEL(str,str-const) : str``
     - Extract a single substring based on regular expression binding pattern, e.g. with single brackets.
     - :ref:`f_regsel`


Algebraic Functions
===================

.. list-table:: Algebraic functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``FLOOR() : int``
     - Floor value, e.g. the largest integer that is smaller or equal.
     - :ref:`floor`
   * - ``CEIL(num) : int``
     - Ceiling value, e.g. the smallest integer that is larger or equal.
     - :ref:`ceil`
   * - ``ROUND(num) : int``
     - The closest integer.
     - :ref:`round`
   * - ``DIV(int,int) : int``
     - The quotient in integer division.
     - :ref:`div`
   * - ``MOD(int,int) : int``
     - The remainder in integer division.
     - :ref:`mod`
   * - ``POW(num,num) : float``
     - Raise the first parameter to the power of the second.
     - :ref:`pow`
   * - ``MIN(num,num) : num``
     - Minimum.
     - :ref:`min`
   * - ``MAX(num,num) : num``
     - Maximum.
     - :ref:`max`
   * - ``SQRT(num) : float``
     - Square root.
     - :ref:`sqrt`
   * - ``SQR(num) : float``
     - Square.
     - :ref:`sqr`
   * - ``ABS(num) : num``
     - The absolute value, e.g. abs(x) = \|x\|.
     - :ref:`abs`
   * - ``LOG(num) : float``
     - 10-based logarithm.
     - :ref:`log10`
   * - ``LN(num) : float``
     - Natural logarithm.
     - :ref:`natural_log`
   * - ``EXP(num) : float``
     - The exponent, e.g. exp(x) = e^x.
     - :ref:`exponential`
   * - ``RANDOM() : num``
     - A random floating point number.
     - :ref:`random`
   * - ``SEGDIST(x,y,a,b) : num``
     - The distance between two segments. Returns 0 if segments overlap.
     - :ref:`segdist`
   * - ``SEGOVERLAP(x,y,a,b) : num``
     - Determines if two segments overlap.
     - :ref:`segoverlap`


Trigonometric Functions
=======================

.. list-table:: Trigonometric functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``SIN(num) : float``
     - Sine based on radians.
     - :ref:`sine`
   * - ``COS(num) : float``
     - Cosine based on radians.
     - :ref:`cosine`
   * - ``ASIN(num) : float``
     - Arc-sine or inverse sine.
     - :ref:`arcsine`
   * - ``ACOS(num) : float``
     - Arc-cosine.
     - :ref:`arccosine`
   * - ``TAN(num) : float``
     - Tangents based on radians.
     - :ref:`tangent`
   * - ``ATAN(num) : float``
     - Arc-tangents.
     - :ref:`arctangent`



Statistical Functions
=====================

.. list-table:: Algebraic functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``CHI(int,int,int,int) : float``
     - The chi-squared value in a 2x2 table.
     - :ref:`chi_2x2`
   * - ``CHI2(float,float) : float``
     - Chi-square complement.
     - :ref:`chi2`
   * - ``CHISQUARE(float,float) : float``
     - Chi-square
     - :ref:`chisquare`
   * - ``CHISQUARECOMPL(float,float)``
     - Chi-square complement.  Same as CHI2.
     - :ref:`chi2`
   * - ``PVAL(int,int,int,int) : float``
     - The 2-sided Fisher-exact or Chi-square based p-val for a 2x2 table.
     - :ref:`pval`
   * - ``PVALONE(int,..,int) : float``
     - The one-sided Fisher-exact or Chi-square based p-val for a 2x2 table.
     - :ref:`pvalone`
   * - ``STUDENT(int,float) : float``
     - Student T distribution.  (degrees of freedom, integration end point)
     - :ref:`student`
   * - ``INVSTUDENT(float,int) : float``
     - Inverse Student T distribution (alpha, size).
     - :ref:`invstudent`
   * - ``NORMAL(float) : float``
     - Normal distribution
     - :ref:`normal`
   * - ``INVNORMAL(float) : float``
     - Inverse Normal distribution.
     - :ref:`invnormal`
   * - ``POISSON(int,float) : float``
     - Poisson distribution.
     - :ref:`poission`
   * - ``POISSONC(int,float) : float``
     - Poisson complement.  See http://acs.lbl.gov/software/colt/api/cern/jet/stat/Probability.html
     - :ref:`poissonc`


.. _list_functions:

List Functions
==============
The table below shows a complete list of functions for dealing with lists in the GOR query language.

The first input value for any list function is the source of the list. This can be explicitly set by either by listing elements in quotation marks or by referencing a column in the input stream. The other input values vary depending on the function. String inputs must be input

With functions that allow a ``sep`` or separator value, note that the separator value in the list is assumed to be a comma and the separator value is optional. If a separator other than a comma is used in the list, the separator value should be defined in quotation marks.

Click on "More info" for any of the functions below to get more information about how to use the function in a GOR query.

.. list-table:: List functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``LISTHASANY(str,lit-list) : bool``
     - Returns true if the comma separated list has an element in the literal list.
     - :ref:`listhasany`
   * - ``LISTHASCOUNT(str,lit-list) : int``
     - Returns a number indicating how many items in the literal list are contained in the list defined in the first parameter.
     - :ref:`listhascount`
   * - ``CONTAINS(str,lit-list) : bool``
     - Returns true if the string contains all the string literals in the list. Not case-sensitive.
     - :ref:`contains`
   * - ``CSCONTAINS(str,lit-list) : bool``
     - Returns true if the string contains all the string literals in the list. Case-sensitive.
     - :ref:`contains`
   * - ``CONTAINSCOUNT(str,lit-list) : int``
     - Returns a number indicating how many items in the literal list are contained in the string. Also try CONTAINSANY, CSCONTAINSANY, CSCONTAINSCOUNT, LISTHASCOUNT, and CSLISTHASCOUNT.
     - :ref:`containscount`
   * - ``LISTFIRST(str,sep) : str``
     - Retrieves the first element (i.e. the head) in a comma-separated list.
     - :ref:`listfirst`
   * - ``LISTSECOND(str,sep) : str``
     - Second element in a comma separated list.
     - :ref:`listsecond`
   * - ``LISTLAST(str,sep) : str``
     - Last element in a comma separated list.
     - :ref:`listlast`
   * - ``LISTTAIL(str,sep) : str``
     - The tail (the list minus the first element).
     - :ref:`listtail`
   * - ``LISTREVERSE(str,sep) : str``
     - The list reversed with optional "sep", e.g. ';'.
     - :ref:`listreverse`
   * - ``LISTSORTASC(str) : str``
     - The list sorted alphabetically in a ascending order.
     - :ref:`listsortasc`
   * - ``LISTSORTDESC(str) : str``
     - The list sorted alphabetically in a descending order.
     - :ref:`listsortdesc`
   * - ``LISTNUMSORTASC(str) : str``
     - The list sorted numerically in a ascending order.
     - :ref:`listnumsortasc`
   * - ``LISTNUMSORTDESC(str): str``
     - The list sorted numerically in a descending order
     - :ref:`listnumsortdesc`
   * - ``LISTTRIM(str) : str``
     - A comma separated list trimmed from white-spaces.
     - :ref:`listtrim`
   * - ``LISTDIST(str) : str``
     - The distinct elements in the list, i.e. corresponding set.
     - :ref:`listdist`
   * - ``LISTMAX(str) : str``
     - The maximum element (element as string).
     - :ref:`listmax`
   * - ``LISTMIN(str): str``
     - The minimum element (element as string).
     - :ref:`listmin`
   * - ``LISTSIZE(str,sep) : int``
     - The size of the list.
     - :ref:`listsize`
   * - ``LISTNUMMAX(str) : float``
     - The maximum element (element as number).
     - :ref:`listnummax`
   * - ``LISTNUMMIN(str): float``
     - The minimum element (element as number).
     - :ref:`listnummin`
   * - ``LISTNUMSUM(str) : float``
     - The sum of the elements (element as numbers).
     - :ref:`listnumsum`
   * - ``LISTNUMAVG(str) : float``
     - The average of the elements (element as numbers).
     - :ref:`listnumavg`
   * - ``LISTNUMSTD(str) : float``
     - The unbiased standard deviation of the elements (element as numbers).
     - :ref:`listnumstd`
   * - ``LISTINDEX(str, str, sep) : int``
     - The one based index to a list of elements where a target is found. The function will search the list defined in parameter 1 for the first instance of parameter 2.
     - :ref:`listindex`
   * - ``LISTMAP(str,str-con) : str``
     - The list translated using expression provided in second argument. Element denoted with x.
     - :ref:`listmap`
   * - ``LISTFILTER(str,str-con) : str``
     - The list filtered using expression provided in second argument. Element as x, index as i. Example LISTFILTER(col,'x != 1') or LISTFILTER(col,'i > 2')
     - :ref:`listfilter`
   * - ``LISTZIP(str,str) : str``
     - Two lists zipped together, each pair of elements separated with a semicolon.
     - :ref:`listzip`
   * - ``LISTZIPFILTER(str,str,str-con) : str``
     - Filter the first list by the content of the second list.
     - :ref:`listzipfilter`
   * - ``LISTCOMB(str,int,int) : str``
     - Returns a semi-comma-separated list of all combinations of elements in the input list of length within the interval specified by the input integers.
     - :ref:`listcomb`
   * - ``LISTADD(str,str,str) : str``
     - Returns a list with the given item added to the end.
     - :ref:`listadd`
   * - ``FSVMAP(str,int,str-con,str) : str``
     - The list of equally separated values (second argument) translated using expression provided in third argument. Fourth argument is the result separator. Element denoted with x. Example FSVMAP(col,2,'x+1',','). Also see :ref:`LISTMAP<listmap>`.
     - :ref:`fsvmap`
   * - ``COLS2LIST(str) : str``
     - Collapse values from multiple columns into a single list, separated by commas
     - :ref:`cols2list`
   * - ``COLS2LIST(str, str) : str``
     - Collapse values from multiple columns into a single list, with a custom separator
     - :ref:`cols2list`
   * - ``COLS2LISTMAP(str, str) : str``
     - Collapse values from multiple columns into a single list with an expression applied, separated by commas
     - :ref:`cols2listmap`
   * - ``COLS2LISTMAP(str, str, str) : str``
     - Collapse values from multiple columns into a single list with an expression applied, with a custom separator
     - :ref:`cols2listmap`

Genomic-Specific Functions
==========================

.. list-table:: Genomic-Specific functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``HAPLDIFF(str,str) : int``
     - The Hamming-like distance between two haplotype strings.
     - :ref:`hapldiff`
   * - ``VARSIG(str,str) : str``
     - Variant signature.
     - :ref:`varsig`
   * - ``REVCOMPL(str) : str``
     - Reverse complement of a DNA sequence string.
     - :ref:`revcompl`
   * - ``RC(str) : str``
     - Shorthand for REVCOMPL(str).
     - :ref:`revcompl`
   * - ``REVCIGAR(str) : str``
     - BAM cigar string for the corresponding reverse complement sequence.
     - :ref:`revcigar`
   * - ``REFBASE(str,int) : str``
     - The reference base at the given locus, based on the build specified in the gor_config.txt file.
     - :ref:`refbase`
   * - ``REFBASES(str,int,int) : str``
     - The reference bases, based on the build specified in the gor_config.txt file.
     - :ref:`refbases`
   * - ``BAMTAG(col,str) : str``
     - Extract a single substring from an attribute value TAG_VALUE-like field (as in BAM files).
     - :ref:`bamtag`
   * - ``TAG(col,str,sep) : str``
     - Extract a single substring from an attribute value field (as in GFF or VCF files, e.g. use semicolon ';' as separator).
     - :ref:`tag`
   * - ``VCFFORMATTAG(str,str,str) : str``
     - Gets a value from a vcf file.
       Parameters are: <name of the format column that contains the format of the data column>,
       <the name of the data column that contains data for a given PN, the column name is the same as the pn>,
       <the tag to get value for>.
     - :ref:`vcfformattag`
   * - ``IHA(str,str) : int``
     - Returns 1 if IUPAC genotype string contains SNP allele str, zero otherwise.
     - :ref:`iha`
   * - ``IUPAC2GT(str) : str``
     - Converts IUPAC genotype to 'A1/A2' genotype.
     - :ref:`iupac2gt`
   * - ``IUPACGTSTAT(str,str) : str``
     - Input IUPAC genotypes for subject, father and mother. Returns '0' if IHE, '1' if OK, and '2' if OK and phase-able.
     - :ref:`iupacgtstat`
   * - ``IUPACFA(str,str,str) : str``
     - Returns the SNP allele of the father.  Only valid if IUPACGTSTAT returns 2.
     - :ref:`iupacfa`
   * - ``IUPACMA(str,str,str) : str``
     - Returns the SNP allele of the mother.  Only valid if IUPACGTSTAT returns 2.
     - :ref:`iupacma`
   * - ``GTSHARE(str,int,str,str,int,str,str) : int``
     - Input two (pos,Ref,Alleles) genotypes where Alleles = 'All1,All2,..' or 'All1/All2/..' or 'All1|All2'.
       Returns the number of identical allels based on all pairwise comparisons between Alleles1 and Alleles2. First
       parameter is chr.
     - :ref:`gtshare`
   * - ``GTSTAT(int,str,str,int,str,str,int,str,str) : str``
     - Input (pos,Ref,Alt) genotypes for subject, father and mother. Returns '0' if IHE, '1' if OK, and '2' if OK and phase-able.
     - :ref:`gtstat`
   * - ``GTFA(int,str,str,int,str,str,int,str,str) : str``
     - Returns the Alt allele of the father.  Only valid if GTSTAT returns '2'.
     - :ref:`gtfa`
   * - ``GTMA(int,str,str,int,str,str,int,str,str) : str``
     - Returns the Alt allele of the mother.  Only valid if GTSTAT returns '2'.
     - :ref:`gtma`
   * - ``INDAG(dag file,str-cont) : bool``
     - Not a standard function.  Should be used as | where go_id INDAG('go.txt','GO\:111111') or INDAG([#temp#],'GO\:111111')
     - :ref:`indag`


.. _genotype_quality_functions:

Genotype-quality Functions
==========================

.. list-table:: Genotype-quality functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``CHARS2PRHOM(str) : float``
     - Turn 2 chars into homozygous genotype prob
     - :ref:`chars2prom`
   * - ``CHARS2PRHET(str) : float``
     - Turn 2 chars into heterozygous genotype prob
     - :ref:`chars2prhet`
   * - ``CHARS2DOSE(str) : float``
     - Turn 2 chars into genotype dosage
     - :ref:`chars2dose`
   * - ``CHARS2PRPRPR(str) : str``
     - Turn 2 chars, round((1.0-pr)*93.0)+33 , into genotype prob triplet (Pr(gt=0),Pr(gt=1),Pr(gt=2)).
       Two spaces (c32c32) map to '0;0;0' (unknown).  Assumes non-phased.
     - :ref:`chars2prprpr`
   * - ``CHARS2PRPR(str) : str``
     - Turn 2 chars, round((1.0-pr)*93.0)+33 , into genotype prob doublet (Pr(gt=1),Pr(gt=2)).
     - :ref:`chars2prpr`
   * - ``CHAR2PR(str) : str``
     - Turn on char into probability, e.g CHAR2PR('!') = 1.0, CHAR2PR('~')=0.
     - :ref:`chars2pr`
   * - ``PR2CHAR(str) : str``
     - Turn probability to characters, i.e. the semi-inverse of CHAR2PR.
     - :ref:`pr2char`
   * - ``PRPR2CHARS(str) : str``
     - Turn probability pair to chars, e.g. PRPR2CHAR('1.0;0.0') = '!~'
     - :ref:`prpr2chars`
   * - ``PRPRPR2CHARS(str) : str``
     - Turn probability triplet to chars, e.g. PRPR2CHAR('1.0;0.0;0.0') = '!~'
     - :ref:`prprpr2chars`
   * - ``PRPR2CHARS(str,sep) : str``
     - Turn probability pair to chars, e.g. PRPR2CHAR('1.0;0.0') = '!~' with custom separator
     - :ref:`prpr2chars`
   * - ``PRPRPR2CHARS(str,sep) : str``
     - Turn probability triplet to chars, e.g. PRPRPR2CHAR('1.0;0.0') = '!~' with custom separator
     - :ref:`prprpr2chars`
   * - ``CHARS2GT(str,float) : str``
     - Turn 2 chars, round((1.0-pr)*93.0)+33 , into genotype 0,1,2 if prob >= thresh else 3.
       Two spaces (c32c32) map to 3 (unknown).  Assumes non-phased probabilities.
     - :ref:`chars2gt`
   * - ``CHARSPHASED2GT(str,float) : str``
     - Turn 2 chars, round((1.0-pr)*93.0)+33 , into phased genotype 0,1,2 if prob >= thresh else 3.
       Two spaces (c32c32) map to 3 (unknown).  Assumes probabilities of phased haplotypes.
       Pr(gt=0) = (1.0 - pfalt) * (1.0 - pmalt), Pr(gt=1) = (1.0 - pfalt) * pmalt + pfalt * (1.0 - pmalt),
       Pr(gt=2) = pfalt * pmalt, where pfalt and pmalt are the chars probabilities values.
     - :ref:`charsphased2gt`


Date Functions
==============

.. list-table:: Date functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``DATE() : string``
     - The current time in the format 'yyyy-MM-dd HH:mm:ss'.
     - :ref:`date`
   * - ``DATE(string) : string``
     - The current time in a specific format, defined by a string of characters that represent time units.
       Example: 'dd/MM/yyyy'. Uses the Java SimpleDateFormat class for formatting.
     - :ref:`date`
   * - ``DAYDIFF(string, string, string) : int``
     - The difference, in days, between two dates.
     - :ref:`daydiff`
   * - ``EDATE(long) : string``
     - A specific time, indicated by a timestamp, in the format 'yyyy-MM-dd HH:mm:ss'.
     - :ref:`edate`
   * - ``EDATE(long,string) : string``
     - A specific time, indicated by a timestamp, in a specific format. The format is defined in the same way as with date(string).
     - :ref:`edate`
   * - ``EPOCH() : long``
     - A timestamp of the current time.
     - :ref:`epoch`
   * - ``EPOCH(string,string) : long``
     - A timestamp of a specific time, indicated with a specified format.
       The format is defined in the same way as with date(string) and edate(long, string). Example: epoch('16/06/2017','dd/MM/yyyy').
     - :ref:`epoch`
   * - ``YEARDIFF(string, string, string) : int``
     - The difference, in years, between two dates.
     - :ref:`yeardiff`



Administration Functions
========================

Diagnostic Functions
--------------------

.. list-table:: Diagnostic functions in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``TIME() : int``
     - The time in milli seconds since the query started.
     - :ref:`time`
   * - ``SLEEP(int) : string``
     - Sleep for given milliseconds while processing each row
     - :ref:`sleep`
   * - ``HOSTNAME() : string``
     - Name of the host running the query
     - :ref:`hostname`
   * - ``IP() : string``
     - IP number of the host running the query
     - :ref:`ip`
   * - ``ARCH() : string``
     - CPU architecture of the host running the query
     - :ref:`arch`
   * - ``THREADID() : int``
     - Thread id of the thread running the query
     - :ref:`threadid`
   * - ``CPULOAD() : float``
     - The cpuload of the process running the query
     - :ref:`cpuload`
   * - ``SYSCPULOAD() : float``
     - The cpuload on the system running the query
     - :ref:`syscpuload`
   * - ``FREE() : float``
     - Free physical memory on the system running the query
     - :ref:`free`
   * - ``FREEMEM() : float``
     - Free memory on the system running the query
     - :ref:`freemem`
   * - ``TOTALMEM() : float``
     - Total memory on the system running the query
     - :ref:`totalmem`
   * - ``MAXMEM() : float``
     - Maximum memory of the process running the query
     - :ref:`maxmem`
   * - ``AVAILCPU() : int``
     - Number of available cpus on the system
     - :ref:`availcpu`
   * - ``OPENFILES() : int``
     - Number of open filedescriptors on the system
     - :ref:`openfiles`
   * - ``MAXFILES() : int``
     - Maximum number of file descriptors
     - :ref:`maxfiles`
   * - ``SYSTEM(string) : string``
     - Returns one line from the stdout of a whitelisted system command
     - :ref:`system`
   * - ``AVGSEEKTIMEMILLIS() : float``
     - Returns the average seektime for the current rowSource in milliseconds
     - :ref:`avgseektimemillis`
   * - ``AVGROWSPERMILLIS() : float``
     - Returns average rows per millisecond for the current rowSource
     - :ref:`avgrowspermillis``
   * - ``AVGBASESPERMILLIS() : float``
     - Returns average bases per millisecond for the current rowSource
     - :ref:`avgbasespermillis`


Version Information
-------------------

.. list-table:: Version information in GOR query language
   :widths: 10  25 5
   :header-rows: 1

   * - Function
     - Description
     - More info
   * - ``GORVERSION() : str``
     - Returns the GOR version
     - :ref:`gorversion`
   * - ``MAJORVERSION() : int``
     - Returns the major version of GOR
     - :ref:`majorversion`
   * - ``MINORVERSION() : int``
     - Returns the minor version of GOR
     - :ref:`minorversion`
   * - ``JAVAVERSION() : str``
     - Returns the JRE version
     - :ref:`javaversion`


