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

The ``-c`` and ``-m`` options are used to encrypt the output of the WRITE command. ``-c`` tells the command to use column store compression for the output and ``-m`` is used to encrypt the files with the MD5 algorithm.

The ``-i`` option writes an index file containing the file position of each chromosome. This option requires a type to be set, which can be FULL or CHROM.

The ``-l`` option selects the compression level to be used by the deflater. Default: 1 (BEST_SPEED)

Usage
=====

.. code-block:: gor

	gor ... | write filename [-f forkCol] [-r] [-c] [-m] [-i]

Options
=======

+-----------------+-----------------------------------------------------------------+
| ``-f column``   | The "fork column" used to split the output into multiple files. |
+-----------------+-----------------------------------------------------------------+
| ``-d``          | Use subdirectories instead of #{fork} in filename for forkwrite |
+-----------------+-----------------------------------------------------------------+
| ``-r``          | Eliminate the fork column from the output.                      |
+-----------------+-----------------------------------------------------------------+
| ``-c``          | Use column store compression for the output.                    |
+-----------------+-----------------------------------------------------------------+
| ``-m``          | Use MD5 encryption on the output file.                          |
+-----------------+-----------------------------------------------------------------+
| ``-i [type]``   | Write index file (.gori) with a .gorz file                      |
|                 | Must state the type, which can be FULL or CHROM                 |
+-----------------+-----------------------------------------------------------------+
| ``-l [level]``  | Compression level (0-9). Default 1.                             |
+-----------------+-----------------------------------------------------------------+
| ``-t '[tags]'`` | List of tags which write ensures a file will be created.        |
|                 | Only valid with the -f option.                                  |
+-----------------+-----------------------------------------------------------------+
| ``-prefix hf``  | Takes in a text source containing prefix to be prepended to the |
|                 | file written.                                                   |
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