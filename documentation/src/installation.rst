============
Installation
============

This page describes how to get started with GOR

Prerequisites
=============

Before setting up GorPipe the following need to be installed:

* Java JDK or JRE version 8 or higher (https://www.oracle.com/java/technologies/javase-downloads.html)
* Open JDK can also be used (https://openjdk.java.net/install/)
* Gradle (https://gradle.org/install/)

Setting Up Test Data
====================

In the parent folder where this repository is located, setup test data by cloning the GOR Test Data repository (https://github.com/gorpipe/GOR-Test-Data)

.. code-block:: bash

    git clone git@github.com:gorpipe/GOR-Test-Data.git

When done cloning, setup the test data as a submodule to this repo via:

.. code-block:: bash

    git submodule init
    git submodule update

Getting Started With GorPipe
============================

To get started with GorPipe, compile the code:

.. code-block:: bash

     make build

Now gor queries can been run against test data. For example via:

.. code-block:: bash

    ./gortools/build/install/gor-scripts/bin/gorpipe "gor tests/data/gor/dbsnp_test.gor | top 10"

The results should be as follows:

.. code-block:: bash

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

Setting Up GOR Shell
====================

As an alternative for a local GorPipe, the GOR shell is a an interactive GOR session in a terminal - a sort of REPL for GOR. Start a GorShell by executing:

.. code-block:: bash

    tools/build/install/tools/bin/gorshell

Now queries can be via:

.. code-block:: gor

    gor tests/data/gor/dbsnp_test.gor | top 10

For a list of GOR input sources and pipe commands and other details, simple type "help" within the GOR shell

Running GOR without setting up the test data submodule can be done via generating GOR rows and then running GOR against that data. For example:

.. code-block:: gor

    gor <(gorrows -p chr1:1000-20000 -segment 100 -step 50 | multimap -cartesian <(norrows 100 | group -lis -sc #1))

Add To Path
-----------

For convenience, GorPipe and GOR Shell can be added to path. For example on mac via by editing /etc/paths:

.. code-block:: bash

    sudo vim /etc/paths

and adding lines

.. code-block:: bash

    <...>/gortools/build/install/gor-scripts/bin
    <...>/tools/build/install/tools/bin/gorshell

to /etcs/paths

Then Gorpipe and GOR Shell can be started via `gorpipe` and `gorshell`

Other
=====

Citations
---------

If you make use of GORpipe in your research, we would appreciate a citation of the following paper:

 GORpipe: a query tool for working with sequence data based on a Genomic Ordered Relational (GOR) architecture
 Bioinformatics, Volume 32, Issue 20, 15 October 2016, Pages 3081–3088,
 https://dx.doi.org/10.1093%2Fbioinformatics%2Fbtw199

License
-------

 GORpipe is free software: you can redistribute it and/or modify
 it under the terms of the AFFERO GNU General Public License as published by
 the Free Software Foundation.

 GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 the AFFERO GNU General Public License for the complete license terms.

 You should have received a copy of the AFFERO GNU General Public License
 along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>