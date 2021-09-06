.. raw:: html

   <span style="float:right; padding-top:10px; color:#BABABA;">Used in: gor/nor</span>

.. _WRITE:

=====
WRITE
=====
The :ref:`WRITE` command can be used to write a stream into one or more files simultaneously.

By specifying a "fork" column, with the ``-f`` option, the stream can be forked, i.e. written into multiple files.  In this case, #{fork} has to appear in the filename template. If you specify the ``-f`` option, you must also specify the column that is used to fork the output files.

The ``-d`` option can be used to write the fork files to directories instead of spliting filenames with #{fork}.

The ``-r`` option can be used to eliminate the fork column from the output, since it is already represented in the filenames.

The ``-c`` tells the command to use column store compression for the output.

Usage
=====

.. code-block:: gor

	gor ... | write filename [-f forkCol] [-d] [-r] [-c] [-m] [-i type]

Options
=======

+-----------------+-----------------------------------------------------------------+
| ``-f column``   | The "fork column" used to split the output into multiple files. |
+-----------------+-----------------------------------------------------------------+
| ``-d``          | Use subdirectories instead of #{fork} in filename for forkwrite.|
+-----------------+-----------------------------------------------------------------+
| ``-r``          | Eliminate the fork column from the output.                      |
+-----------------+-----------------------------------------------------------------+
| ``-c``          | Use column store compression for the output.                    |
+-----------------+-----------------------------------------------------------------+
| ``-m``          | Create MD5 sum file along with the output file.                 |
+-----------------+-----------------------------------------------------------------+
| ``-i type``     | Write index file (.gori) with a .gorz file, (.tbi) with .vcf.gz |
|                 | Must state the type, which can be FULL, CHROM or TABIX          |
+-----------------+-----------------------------------------------------------------+
| ``-l level``    | Compression level (0-9). Default 1.                             |
+-----------------+-----------------------------------------------------------------+
| ``-t 'tags'``   | List of tags which write ensures a file will be created.        |
|                 | Only valid with the -f option.                                  |
+-----------------+-----------------------------------------------------------------+
| ``-tags 'tags'``| List of tags/alias to use in the resulting dictionary when      |
|                 | writing the files to directories.   Usually used with partgor   |
|                 | as ``-tags #{tags}``.                                           |
+-----------------+-----------------------------------------------------------------+
| ``-prefix hf``  | Takes in a text source containing prefix to be prepended to the |
|                 | file written. Also support string in single quotes              |
+-----------------+-----------------------------------------------------------------+
| ``-noheader``   | Don't write a header lines.  Not valid with gor/gorz/nor/norz.  |
+-----------------+-----------------------------------------------------------------+
| ``-card 'cols'``| Calculate cardinality of columns in 'cols' and adds to the      |
|                 | outputs meta data.                                              |
+-----------------+-----------------------------------------------------------------+

Examples
========

.. code-block:: gor

    gor -p chr1:10020-10051 fileA.gor | write fileB.gorz

The query above will read the first four rows of the example file shown above and write them to a compressed GOR file in the same directory.

.. code-block:: gor

    gor multiPIDfile.gor | write data_#{fork}.gor -f PID -r

The query above will write the contents of multiPIDfile.gor into as many files as there are distinct PIDs in the file. In this example, the output files from the WRITE command will be named data_101.gor and data_102.gor and the ``-r`` flag removes the PID column (i.e. the column used to fork the data).

.. code-block:: gor

    gor multiPIDfile.gor | write data_#{fork}.gor -f PID -r -t 'PN001,PN002,PN003'

The query above will write the contents of multiPIDfile.gor into as many files as there are distinct PIDs in the file. In this example a list of PIDs is supplied and the write command will create an empty file for each of the tags listed.