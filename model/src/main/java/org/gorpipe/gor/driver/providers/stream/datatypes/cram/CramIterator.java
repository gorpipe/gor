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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram;

import htsjdk.samtools.*;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.seekablestream.SeekableBufferedStream;
import htsjdk.samtools.util.SequenceUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.providers.stream.datatypes.bam.BamIterator;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference.CompositeReferenceSource;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference.EBIReferenceSource;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference.FolderReferenceSource;
import org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference.SharedFastaReferenceSource;
import org.gorpipe.gor.model.ChromoLookup;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableStream;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.iterators.RefSeq;
import org.gorpipe.util.Strings;
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

    public final static String KEY_GENERATEMISSINGATTRIBUTES = "gor.driver.cram.generatemissingattributes";
    public final static String KEY_FASTAREFERENCESOURCE = "gor.driver.cram.fastareferencesource";
    public final static String KEY_REFERENCE_PREFER_FOLDER = "gor.driver.cram.reference.preferfolder";

    private static final Logger log = LoggerFactory.getLogger(CramIterator.class);

    private final CramFile cramFile;
    private int[] columns;
    ChromoLookup lookup;
    private String fileName;
    private String projectCramReferencePath;  // Cram reference path from project context.
    private RefSeq projectRefSeq;  // RefSeq from project context, used for MD tag calculation.
    private ReferenceSequenceFile referenceSequenceFile; // Handle to cram reference file, fallback for MD tag calculation.
    private CRAMReferenceSource referenceSource;
    private CRAMFileReader cramFileReader;
    private boolean generateMissingCramAttributes;

    /**
     * Construct a CramIterator
     *
     * @param lookup   The lookup service for chromosome name to ids
     * @param cramFile The CRAM File to iterate through
     * @param columns  The columns to be included, or null to include all
     */
    public CramIterator(ChromoLookup lookup, CramFile cramFile, int[] columns) {
        this.cramFile = cramFile;
        this.columns = columns;
        this.lookup = lookup;
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
                byte[] referenceBytes = null;
                if (projectRefSeq != null) {
                    referenceBytes = projectRefSeq.getBases(record.getContig(), record.getAlignmentStart(), record.getAlignmentEnd()).getBytes();
                } else if (referenceSequenceFile != null) {
                    // Fallback to the reference file used by the CRAM reader
                    referenceBytes = referenceSequenceFile.getSubsequenceAt(record.getContig(), record.getAlignmentStart(), record.getAlignmentEnd()).getBases();
                }
                if (referenceBytes != null) {
                    CramUtils.calculateMdAndNmTags(record, referenceBytes, calculateMD, calculateNM);
                }
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

        projectCramReferencePath = session.getProjectContext().getReferenceBuild().getCramReferencePath();
        if (!Strings.isNullOrEmpty(session.getProjectContext().getGorConfigFile())) {
            projectRefSeq = session.getProjectContext().createRefSeq();
        }

        if (cramFile != null) {
            // I read this property here through System.getProperty as there is no other way to pass properties to the driver
            generateMissingCramAttributes = System.getProperty(KEY_GENERATEMISSINGATTRIBUTES, "false").equalsIgnoreCase("true");

            fileName = cramFile.getFileSource().getSourceReference().getUrl();

            referenceSource = createReferenceSource(session.getProjectContext().getRealProjectRoot());

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
            init(lookup, samreader, true);
        }
    }

    /**
     * The reference file is initially set as the file supplied via the -ref option if set.
     *
     * @return initial reference file
     */
    private String getSourceReferenceFile() {
        StreamSource ref = cramFile.getReferenceSource();
        return ref != null ? ref.getSourceReference().getUrl() : null;
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

    private CRAMReferenceSource createReferenceSource(String root) {
        File file = null;
        boolean forceFolder = false;

        String sourceRef = getSourceReferenceFile();
        if (!Strings.isNullOrEmpty(sourceRef)) {
            file = new File(sourceRef);
        }

        if (file == null) {
            file = getReferenceFromReferenceLinkFile();
        }

        if (file == null) {
            file = getReferenceFromGorConfig(root);
            forceFolder = Boolean.parseBoolean(System.getProperty(KEY_REFERENCE_PREFER_FOLDER, "true"));
        }

        if (file == null) {
            file = getReferenceFromGorOptions();
            forceFolder = Boolean.parseBoolean(System.getProperty(KEY_REFERENCE_PREFER_FOLDER, "true"));

        }
        if (file == null || !file.exists()) {
            throw new GorResourceException("No cram reference found: %s".formatted(file), file != null ? file.getPath() : "null");
        }

        // This reference should be fasta but we let the htsjdk library decide
        return createFileReference(file, forceFolder);
    }

    private File getReferenceFromGorOptions() {
        String refPath = System.getProperty(KEY_FASTAREFERENCESOURCE, "");

        if (!StringUtils.isEmpty(refPath)) {
            return new File(refPath);
        }

        return null;
    }

    private File getReferenceFromGorConfig(String root) {
        if (!Strings.isNullOrEmpty(projectCramReferencePath)) {
            return PathUtils.resolve(Paths.get(root), Paths.get(projectCramReferencePath)).toFile();
        }
        return null;
    }

    private File getReferenceFromReferenceLinkFile() {
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

        return null;
    }

    private CRAMReferenceSource createFileReference(File refFile, boolean preferFolder) {
        if (refFile.isDirectory()) {
            return createCompositeReferenceSource(refFile);
        } else if (preferFolder) {
            try {
                return createCompositeReferenceSource(refFile.getParentFile());
            } catch (Exception e) {
                // Fallback to single file, in case none of the files contains proper meta.
                return createSharedFastaReferenceSource(refFile);
            }
        } else {
            return createSharedFastaReferenceSource(refFile);
        }
    }

    private CRAMReferenceSource createSharedFastaReferenceSource(File refFile) {
        log.debug("Using fasta reference file for CRAM: {}", refFile.getPath());
        referenceSequenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(refFile);

        String referenceKey = FilenameUtils.removeExtension(refFile.getName());
        var referenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(refFile);
        return new SharedFastaReferenceSource(referenceFile, referenceKey);
    }
    private CRAMReferenceSource createCompositeReferenceSource(File refFolder) {
        log.debug("Using folder reference for CRAM: {}", refFolder.getPath());
        return new CompositeReferenceSource(List.of(
                new FolderReferenceSource(refFolder.getPath()),
                new EBIReferenceSource(refFolder.getPath())));
    }
}
