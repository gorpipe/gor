## Introduction

The GORpipe analysis tool is developed and released by Genuity Science. It originates from the pioneers of population based genomic analysis, deCODE genetics, headquartered in Reykjavik, Iceland.
        
GORpipe is a tool based on a genomic ordered relational architecture and allows analysis of large sets of genomic and phenotypic tabular data using a declarative query language, in a parallel execution engine.  It is very efficient in a wide range of use-cases, including genome wide batch analysis, range-queries, genomic table joins of variants and segments, filtering, aggregation etc.  The query language combines ideas from SQL and Unix shell pipe syntax, supporting seek-able nested queries, materialized views, and a rich set of commands and functions.  For more information see the paper in Bioinformatics (https://dx.doi.org/10.1093%2Fbioinformatics%2Fbtw199).

## Prerequisites

Before setting up GORpipe you need to have Java JDK or JRE version 8 or higher set up on your computer. 
For example Open JDK (https://openjdk.java.net/install/).

Alternatively Oracle distributions can also be used (https://www.oracle.com/java/technologies/javase-downloads.html).

## Getting started with GORpipe

Download the latest release of GORpipe from https://github.com/gorpipe/gor/releases.

Extract the package (gor-scripts-\<version\>.zip). 

Then running GOR without setting up test data can be done by generating GOR rows and then running GOR against that data. For example:

    ./gor-scripts-<version>/bin/gorpipe "gor <(gorrows -p chr1:1000-20000 -segment 100 -step 50 | multimap -cartesian <(norrows 100 | group -lis -sc #1))"  

Note: Substitute <version> with the actual latest version number (e.g. gor-scripts-2.8.1). 
        
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

## Setting up an interactive shell for GORpipe

GORpipe can be invoked and used through an interactive shell session in a terminal - a sort of REPL for GOR, coined GORshell. Start a GORshell by executing:

    ./gor-scripts-<version>/bin/gorshell 
    
This will start an interactive shell session where queries can be executed:

    gor gor-test-data/gor/dbsnp_test.gor | top 10
        
For a list of GOR input sources, pipe commands and other details, simply type `help` within the GOR shell.

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