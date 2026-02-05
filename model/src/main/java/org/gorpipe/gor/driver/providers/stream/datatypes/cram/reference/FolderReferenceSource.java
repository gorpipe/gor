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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reference source that resolves references by MD5 using a folder of FASTA files (with .dict and .fai)
 *
 */

public class FolderReferenceSource extends MD5CachedReferenceSource {

    private static final Logger log = LoggerFactory.getLogger(FolderReferenceSource.class);

    private static final Set<String> FASTA_EXT = Set.of("fa", "fasta");

    private record Md5ReferencePath(String md5, Path path, String contig) {}
    protected static Map<String, Md5ReferencePath> md5ToReferencePath = new ConcurrentHashMap<>();

    private Path referenceFolder;

    private final Map<Path, ReferenceSequenceFile> refFileByPath = new ConcurrentHashMap<>();

    public FolderReferenceSource(String referenceFolder) {
        this.referenceFolder = Path.of(referenceFolder);
        if (!Files.isDirectory(this.referenceFolder)) {
            throw new GorResourceException("Can not create FolderReferenceSource %s as the target is not a folder or does not exists".formatted(referenceFolder), referenceFolder);
        }
        scanReferenceFolder();
    }

    @Override
    protected byte[] loadReference(final SAMSequenceRecord record) {
        // Load form fasta file.
        Md5ReferencePath referencePath = md5ToReferencePath.get(record.getMd5());
        if (referencePath != null) {
            ReferenceSequenceFile rsFile = refFileByPath.computeIfAbsent(referencePath.path(), ReferenceSequenceFileFactory::getReferenceSequenceFile);
            if (rsFile == null) {
                throw new GorDataException(String.format("Reference file %s for md5 %s not found in reference folder %s",
                        referencePath, referencePath.md5(), referenceFolder));
            }

            return rsFile.getSequence(referencePath.contig()).getBases();
        }

        return null;
    }

    private void scanReferenceFolder() {
        md5ToReferencePath.clear();

        try {
            for (var p : Files.list(referenceFolder).filter(Files::isRegularFile).toList()) {
                var f = p.getFileName().toString().toLowerCase();
                if (FASTA_EXT.stream().anyMatch(ext -> f.endsWith("." + ext))) {
                    processFasta(referenceFolder.resolve(f));
                }
            }
        } catch (IOException e) {
            log.warn("Failed scanning reference folder {}", referenceFolder, e);
        }
    }

    private void processFasta(Path fastaFile) {
        ReferenceSequenceFile refFile = refFileByPath.computeIfAbsent(fastaFile, ReferenceSequenceFileFactory::getReferenceSequenceFile);
        SAMSequenceDictionary dictionary = refFile.getSequenceDictionary();
        if (dictionary == null) {
            throw new GorResourceException("Fasta file %s is invalid cram reference as it is missing dict file".formatted(fastaFile), fastaFile.toString());
        }
        for (SAMSequenceRecord rec : dictionary.getSequences()) {
            String md5 = rec.getMd5();
            if (md5 == null || md5.isEmpty()) continue;
            md5ToReferencePath.put(md5, new FolderReferenceSource.Md5ReferencePath(md5, fastaFile, rec.getContig()));
        }
    }

    @Override
    public void close() {
        refFileByPath.values().forEach(r -> {
            try {
                r.close();
            } catch (IOException ignored) {
            }
        });
        refFileByPath.clear();
    }

    Set<Path> getReferenceFiles() {
        return new HashSet<>(refFileByPath.keySet());
    }
}
