## Introduction

The GORpipe analysis tool is developed and released by Genuity Science. It originates from the pioneers of population based genomic analysis, deCODE genetics, headquartered in Reykjavik, Iceland.
        
GORpipe is a tool based on a genomic ordered relational architecture and allows analysis of large sets of genomic and phenotypic tabular data using a declarative query language, in a parallel execution engine.  It is very efficient in a wide range of use-cases, including genome wide batch analysis, range-queries, genomic table joins of variants and segments, filtering, aggregation etc.  The query language combines ideas from SQL and Unix shell pipe syntax, supporting seek-able nested queries, materialized views, and a rich set of commands and functions.  For more information see the paper in Bioinformatics (https://dx.doi.org/10.1093%2Fbioinformatics%2Fbtw199).

## Prerequisites

Before setting up GORpipe you need to have Java JDK or JRE version 11 or higher set up on your computer. 
For example Open JDK (https://openjdk.java.net/install/). To check your Java version, open up a terminal and enter:

    java -version

Alternatively Oracle distributions can also be used (https://www.oracle.com/java/technologies/javase-downloads.html).

## Getting started with GORpipe

Download the latest release of GORpipe from https://github.com/gorpipe/gor/releases.

Extract the package (gor-scripts-\<version\>.zip). 

Then running GOR without setting up test data can be done by generating GOR rows and then running GOR against that data. For example:

    ./gor-scripts-<version>/bin/gorpipe "gor <(gorrows -p chr1:1000-20000 -segment 100 -step 50 | multimap -cartesian <(norrows 100 | group -lis -sc #1))"  

Note: Substitute \<version\> with the actual latest version number (e.g. gor-scripts-2.9.0). 

Optional: A version of GORpipe (GORspark) including an integration with Apache Spark can be setup by downloading the latest 
release from https://github.com/gorpipe/gor-spark/releases. This release is much larger (~270MB) than the regular GORpipe release since it contains Apache Spark libraries. 
        
## Setting up test data (Optional)

Download the latest released GORpipe test data from https://github.com/gorpipe/gor-test-data/releases.

Extract the package (gor-test-data.zip) and assuming it is located in the same folder as the latest release of GORpipe, 
gor queries can been run against test data via:

     ./gor-scripts-<version>/bin/gorpipe "nor gor-test-data/gor/dbsnp_test.gor | top 10" 

The results should be as follows:

    Chrom   POS     reference       allele  differentrsIDs
    chr1    10179   C       CC      rs367896724
    chr1    10250   A       C       rs199706086
    chr10   60803   T       G       rs536478188
    chr10   61023   C       G       rs370414480
    chr11   61248   G       A       rs367559610
    chr11   66295   C       A       rs61869613
    chr12   60162   C       G       rs544101329
    chr12   60545   A       T       rs570991495
    chr13   19020013        C       T       rs181615907
    chr13   19020145        G       T       rs28970552

If the GORspark version was setup the following query should work as well the results the same as above:

    ./gor-scripts-<version>/bin/gorpipe "select * from gor-test-data/gor/dbsnp_test.gor limit 10" 

## Setting up an interactive shell for GORpipe

GORpipe can be invoked and used through an interactive shell session in a terminal - a sort of REPL for GOR, coined GORshell. Start a GORshell by executing:

    ./gor-scripts-<version>/bin/gorshell 
    
This will start an interactive shell session where queries can be executed:

    gor gor-test-data/gor/dbsnp_test.gor | top 10
        
For a list of GOR input sources, pipe commands and other details, simply type `help` within the GOR shell.    

## Setting up reference data (Optional)

Go to into the gor-scripts folder (gor-scripts-\<version\>/) and download the reference data found at:

    https://s3.amazonaws.com/wuxinextcode-sm-public/data/standalone-project-data.tar.gz
    
If using a command line, this can be accomplished using `wget`

    wget https://s3.amazonaws.com/wuxinextcode-sm-public/data/standalone-project-data.tar.gz

Since this is a large dataset (~9gb), this download could take a few minutes. After that extract the package via:

    tar -xvf standalone-project-data.tar.gz

and a folder called `ref` should be created.

Note that the file standalone-project-data.tar.gz will remain in the folder. You may want to delete it afterwards. 
    
To test the reference data, using aliases, try the following query while being within the gor-scripts folder:

    ./bin/gorpipe "gor #genes# | top 10" -aliases config/gor_aliases.txt
   
The results should be as follows:

    Chrom	gene_start	gene_end	Gene_Symbol
    chr1	11868	14412	DDX11L1
    chr1	14362	29806	WASH7P
    chr1	29553	31109	MIR1302-10
    chr1	34553	36081	FAM138A
    chr1	52472	54936	OR4G4P
    chr1	62947	63887	OR4G11P
    chr1	69090	70008	OR4F5
    chr1	89294	133566	RP11-34P13.7
    chr1	89550	91105	RP11-34P13.8
    chr1	131024	134836	CICP27

## Setting up Phecode gwas data (Optional)

Go to into the gor-scripts folder (gor-scripts-\<version\>/) and download the Phecode gwas data found at:

    https://s3.amazonaws.com/wuxinextcode-sm-public/data/standalone-project-phecode-gwas-data.tar.gz
    
If using a command line, this can be accomplished using `wget`

    wget https://s3.amazonaws.com/wuxinextcode-sm-public/data/standalone-project-phecode-gwas-data.tar.gz

Since this is a large dataset (~10gb), this download could take a few minutes. After that extract the package via:

    tar -xvf standalone-project-phecode-gwas-data.tar.gz

and a folder called `phecode_gwas` should be created.

Note that the file standalone-project-phecode-gwas-data.tar.gz will remain in the folder. You may want to delete it afterwards.

To test this data, the GOR dictionary for the data, `Phecode_adjust_f2.gord` can be queried via for example:

    ./bin/gorpipe "gor phecode_gwas/Phecode_adjust_f2.gord | top 10"
    
The results should be as follows:

    CHROM	POS	REF	ALT	pVal_mm	OR_mm	CASE_info	GC	QQ	BONF	HOLM	Source
    chr1	11008	C	G	7.0e-09	1.3071212318082575	11/520/7230	0.065424	0.24246	0.11973	0.090699	218.1
    chr1	11008	C	G	6.6e-22	4.444107087017232	4/60/248	0.23978	0.35578	1.1289e-14	7.2724e-15	282.5
    chr1	11012	C	G	7.0e-09	1.3071212318082575	11/520/7230	0.065424	0.24246	0.11973	0.090699	218.1
    chr1	11012	C	G	6.6e-22	4.444107087017232	4/60/248	0.23978	0.35578	1.1289e-14	7.2724e-15	282.5
    chr1	13116	T	G	3.7e-10	0.1992753733291237	0/9/272	0.44383	0.46853	0.0063285	0.0033634	282.5
    chr1	13118	A	G	3.7e-10	0.1992753733291237	0/9/272	0.44383	0.46853	0.0063285	0.0033634	282.5
    chr1	13273	G	C	4.9e-08	0.1156812360571759	0/3/261	0.50503	0.50293	0.83810	0.41659	282.5
    chr1	14464	A	T	8.1e-06	4.3201606068833875	2/12/23	1.2298e-10	1.2424e-05	1.0000	1.0000	362.8
    chr1	14464	A	T	7.8e-08	0.22233506547729875	0/8/278	0.51155	0.50681	1.0000	0.65797	282.5
    chr1	15211	T	G	3.9e-15	0.2678814996572006	29/38/16	0.33692	0.41018	6.6706e-08	3.9344e-08	282.5
    
## Setting environment variables

For convenience, GORpipe and GORshell can be added to path. For example on Mac by editing /etc/paths: 

    sudo vim /etc/paths 
    
and add the following line: 

    <PATH_TO_GOR_SCRIPTS>/bin

Then GORpipe and GORshell can be started via `gorpipe` and `gorshell` from any location.
    
## Build GORpipe from source

For developers, to get started with GORpipe, first clone the repo via:

    git clone https://github.com/gorpipe/gor

Test data for GOR is then obtained by cloning the GOR test data repository (https://github.com/gorpipe/gor-test-data) as a submodule into the `tests/data` folder:

    git submodule update --init --recursive

The code is built via:

    make build
         
Now gor queries can been run against test data. For example:

    ./gortools/build/install/gor-scripts/bin/gorpipe "gor tests/data/gor/dbsnp_test.gor | top 10"

The results should be as follows:

    Chrom   POS     reference       allele  differentrsIDs
    chr1    10179   C       CC      rs367896724
    chr1    10250   A       C       rs199706086
    chr10   60803   T       G       rs536478188
    chr10   61023   C       G       rs370414480
    chr11   61248   G       A       rs367559610
    chr11   66295   C       A       rs61869613
    chr12   60162   C       G       rs544101329
    chr12   60545   A       T       rs570991495
    chr13   19020013        C       T       rs181615907
    chr13   19020145        G       T       rs28970552

GORshell can also be started up via:

    ./gortools/build/install/gor-scripts/bin/gorshell

## How to get help?

Documentation for GORpipe can be found at: http://docs.gorpipe.org/. Additionally, help can be found while using GORpipe
by executing `gorpipe help` or just `help` within the GOR shell.

## Citations
        
If you make use of GORpipe in your research, we would appreciate a citation to the following paper:
        
    GORpipe: a query tool for working with sequence data based on a Genomic Ordered Relational (GOR) architecture
    Bioinformatics, Volume 32, Issue 20, 15 October 2016, Pages 3081–3088,
    https://dx.doi.org/10.1093%2Fbioinformatics%2Fbtw199

## License

    GORpipe is free software: you can redistribute it and/or modify
    it under the terms of the AFFERO GNU General Public License as published by
    the Free Software Foundation.

    GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
    INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
    NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
    the AFFERO GNU General Public License for the complete license terms.

    You should have received a copy of the AFFERO GNU General Public License
    along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>