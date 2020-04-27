/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.GorSession;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableStream;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.CramFile;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.CramUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.table.PathUtils;
import org.gorpipe.util.string.StringUtil;
import htsjdk.samtools.*;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.util.SequenceUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;


/**
 * Iterator for CRAM files. Cram is compressed version of the BAM file and uses a reference base to perform its
 * compression. Reference is used based on the following priorities:<p>
 * <ol>
 * <li><b>-ref</b> option in gor</li>
 * <li>File at cram location ending with <i>cram.ref</i> containing a reference to a fasta file or a chromseq directory</li>
 * <li>Fasta file through gor option <i>gor.driver.cram.fastareferencesource</i></li>
 * </ol>
 * <p>Cram is not fully compatible with BAM as it does not save the MD and NM option. Those are though easily generated
 * from the BAM row. To enable the generation of these option set the <i>gor.driver.cram.generatemissingattributes</i>
 * gor option to <i>true</i>"</p>
 */
public class CramIterator extends BamIterator {

    private final static String KEY_GENERATEMISSINGATTRIBUTES = "gor.driver.cram.generatemissingattributes";
    private final static String KEY_FASTAREFERENCESOURCE = "gor.driver.cram.fastareferencesource";

    private static final Logger log = LoggerFactory.getLogger(CramIterator.class);

    private CramFile cramFile;
    private int[] columns;
    GenomicIterator.ChromoLookup lookup;
    private String fileName;
    private String buildPath = "";
    private String cramReferencePath = "";
    private CRAMFileReader cramFileReader;
    private ReferenceSequenceFile referenceSequenceFile;
    private CRAMReferenceSource referenceSource;
    private boolean generateMissingCramAttributes;

    /**
     * Construct a CramIterator
     *
     * @param lookup   The lookup service for chromosome name to ids
     * @param cramFile The CRAM File to iterate through
     * @param columns  The columns to be included, or null to include all
     */
    public CramIterator(GenomicIterator.ChromoLookup lookup, CramFile cramFile, int[] columns) throws IOException {
        this.cramFile = cramFile;
        this.columns = columns;
        this.lookup = lookup;
    }


    /**
     * Construct a CramIterator
     *
     * @param lookup  The lookup service for chromosome name to ids
     * @param file    The CRAM File to iterate through
     * @param columns The columns to be included, or null to include all
     */
    public CramIterator(GenomicIterator.ChromoLookup lookup, String file, String index, String reference, int[] columns, boolean generateMissingAttributes) {

        fileName = file;
        generateMissingCramAttributes = generateMissingAttributes;
        File cramfile = new File(file);
        File cramindex = new File(index);
        if (!cramindex.exists()) {
            cramindex = new File(file + ".crai");
        }

        referenceSequenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(reference));
        referenceSource = createReferenceSource(fileName, "");

        try {
            cramFileReader = new CRAMFileReader(cramfile, new FileInputStream(cramindex), referenceSource);
        } catch (FileNotFoundException e) {
            throw new GorResourceException("Cram file not found.", file, e);
        }
        SamReader samreader = new SamReader.PrimitiveSamReaderToSamReaderAdapter(cramFileReader, null);
        init(lookup, samreader, columns, true);
    }

    @Override
    public Row next() {
        Row row = super.next();

        if (generateMissingCramAttributes && row instanceof SAMRecordRow) {
            SAMRecordRow samRow = (SAMRecordRow) row;
            SAMRecord record = samRow.record;

            boolean calculateMD = record.getStringAttribute(SAMTag.MD.name()) == null;
            boolean calculateNM = record.getIntegerAttribute(SAMTag.NM.name()) == null;

            if (calculateMD) {
                byte[] referenceBytes = referenceSequenceFile.getSubsequenceAt(record.getContig(), record.getAlignmentStart(), record.getAlignmentEnd()).getBases();
                CramUtils.calculateMdAndNmTags(record, referenceBytes, calculateMD, calculateNM);
            } else if (calculateNM) {
                SequenceUtil.calculateSamNmTagFromCigar(record);
            }
        }

        return row;
    }

    @Override
    public void close() {
        super.close();

        try {
            if (cramFileReader != null) {
                cramFileReader.close();
            }

            if (referenceSource != null && referenceSource instanceof Closeable) {
                ((Closeable) referenceSource).close();
            }

            closeReferenceFile();
        } catch (IOException ex) {
            throw new GorResourceException("Failed to close CRAM iterator.", fileName, ex);
        }
    }

    @Override
    public void init(GorSession session) {
        if (session == null) {
            return;
        }

        buildPath = session.getProjectContext().getReferenceBuild().getBuildPath();

        cramReferencePath = session.getProjectContext().getReferenceBuild().getCramReferencePath();

        if (cramFile != null) {
            // I read this property here through System.getProperty as there is no other way to pass properties to the driver
            generateMissingCramAttributes = System.getProperty(KEY_GENERATEMISSINGATTRIBUTES, "false").equalsIgnoreCase("true");

            fileName = cramFile.getFileSource().getSourceReference().getUrl();

            referenceSource = createReferenceSource(getInitialReferenceFile(), session.getProjectContext().getRealProjectRoot());

            SeekableBufferedStream cramStream = new SeekableBufferedStream(new StreamSourceSeekableStream(cramFile.getFileSource()));

            SeekableBufferedStream cramIndexStream = null;

            if (cramFile.getIndexSource() != null) {
                cramIndexStream = new SeekableBufferedStream(new StreamSourceSeekableStream(cramFile.getIndexSource()), 10000);
            }

            SamInputResource sir = SamInputResource.of(cramStream);
            try {
                cramFileReader = new CRAMFileReader(cramStream, cramIndexStream, referenceSource, ValidationStringency.DEFAULT_STRINGENCY);
            } catch (IOException ioe) {
                throw new GorResourceException("Failed to create cram iterator.", fileName, ioe);
            }

            SamReader samreader = new SamReader.PrimitiveSamReaderToSamReaderAdapter(cramFileReader, sir);
            init(lookup, samreader, columns, true);
        }
    }

    /**
     * The reference file is initially set as the file supplied via the -ref option if set.
     *
     * @return initial reference file
     */
    private String getInitialReferenceFile() {
        StreamSource ref = cramFile.getReferenceSource();
        String referenceFileName = "";

        if (ref != null) {
            referenceFileName = ref.getSourceReference().getUrl();
        }
        return referenceFileName;
    }

    private void closeReferenceFile() {
        if (referenceSequenceFile != null) {
            try {
                referenceSequenceFile.close();
            } catch (IOException ex) {
                log.warn("Failed to close cram reference file");
            } // Throw exception during cleanup?
        }
    }

    private CRAMReferenceSource createReferenceSource(String ref, String root) {

        File file = new File(ref);
        file = getReferenceFromReferenceLinkFile(file);
        file = getReferenceFromGorConfig(file, root);
        file = getReferenceFromGorOptions(file);

        if (!file.exists()) {
            throw new GorResourceException("Reference does not exist.", file.toString());
        }

        // This reference should be fasta but we let the htsjdk library decide
        return createFileReference(file.toString());
    }

    private File getReferenceFromGorOptions(File file) {
        if (!file.exists()) {
            String refPath = System.getProperty(KEY_FASTAREFERENCESOURCE, "");

            if (!StringUtils.isEmpty(refPath)) {
                return new File(refPath);
            }
        }
        return file;
    }

    private File getReferenceFromGorConfig(File file, String root) {
        if (!file.exists() && !StringUtil.isEmpty(cramReferencePath)) {
            return PathUtils.resolve(Paths.get(root), Paths.get(cramReferencePath)).toFile();
        }
        return file;
    }

    private File getReferenceFromReferenceLinkFile(File file) {
        if (!file.exists()) {
            File refLinkFile = new File(this.fileName + ".ref");

            if (refLinkFile.exists()) {
                try {
                    List<String> lines = FileUtils.readLines(refLinkFile, Charset.defaultCharset());

                    if (lines.size() > 0) {
                        return new File(lines.get(0));
                    }
                } catch (IOException e) {
                    /*Do Nothing*/
                }
            }
        }
        return file;
    }

    private CRAMReferenceSource createFileReference(String ref) {
        String referenceKey = FilenameUtils.removeExtension(FilenameUtils.getBaseName(ref));
        referenceSequenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(ref));
        return new SharedFastaReferenceSource(referenceSequenceFile, referenceKey);
    }

}
