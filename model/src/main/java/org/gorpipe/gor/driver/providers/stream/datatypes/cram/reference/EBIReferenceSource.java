package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.io.InputStreamUtils;
import htsjdk.samtools.util.SequenceUtil;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Get reference sources from the EBI service.   Optionally caching them locally for future reference.
 * The downloaded references sequences are stored in the provided folder as md5_<MD5>.txt files.
 */
public class EBIReferenceSource extends MD5CachedReferenceSource {
    private static final Logger log = LoggerFactory.getLogger(EBIReferenceSource.class);

    public static final String KEY_USE_CRAM_REF_DOWNLOAD = "gor.driver.cram.ref.download";

    private static final String REFBASES_PREFIX = "md5_";
    private static final String REFBASES_EXT = ".txt";

    protected static Map<String, Path> md5ToRefbases = new ConcurrentHashMap<>();

    private static final int DOWNLOAD_TRIES_BEFORE_FAILING = 2;

    private Path referenceFolder;  // If null we do not download.

    public EBIReferenceSource() {
    }

    public EBIReferenceSource(String referenceFolder) {
        if (!Strings.isNullOrEmpty(referenceFolder)) {
            this.referenceFolder = Path.of(referenceFolder);

            if (!Files.isDirectory(this.referenceFolder)) {
                throw new GorResourceException("Can not create FolderReferenceSource %s as the target is not a folder or does not exists".formatted(referenceFolder), referenceFolder);
            }
            scanReferenceFolder();
        }
    }

    Set<Path> getRefbasesFiles() {
        return new HashSet<>(md5ToRefbases.values());
    }

    private void scanReferenceFolder() {
        md5ToRefbases.clear();

        if (referenceFolder == null) return;

        try {
            for (var p : Files.list(referenceFolder).filter(Files::isRegularFile).toList()) {
                var f = p.getFileName().toString().toLowerCase();
                if (f.startsWith(REFBASES_PREFIX) && f.endsWith(REFBASES_EXT)) {
                    processRefbasesFile(referenceFolder.resolve(f));
                }
            }
        } catch (IOException e) {
            log.warn("Failed scanning reference folder {}", referenceFolder, e);
        }
    }

    private void processRefbasesFile(Path refbases) {
        String fileName = refbases.getFileName().toString();
        if (!fileName.startsWith(REFBASES_PREFIX) || !fileName.endsWith(REFBASES_EXT)) return;

        String md5 = fileName.substring(REFBASES_PREFIX.length(), fileName.length() - REFBASES_EXT.length());
        md5ToRefbases.put(md5, refbases);
    }

    @Override
    protected byte[] loadReference(final SAMSequenceRecord record) {
        var md5 = record.getMd5();

        // Load from refbases file.
        Path refbasesPath = md5ToRefbases.get(md5);
        if (refbasesPath != null) {
            try {
                byte[] bases = Files.readAllBytes(refbasesPath);
                if (bases == null || bases.length == 0) {
                    throw new GorDataException("Reference sequence in " + refbasesPath + " is empty");
                }
                return bases;
            } catch (IOException e) {
                throw new GorDataException("Could not read refbases file %s".formatted(refbasesPath), e);
            }
        }

        // Load from EBI service.
        if (Boolean.parseBoolean(System.getProperty(KEY_USE_CRAM_REF_DOWNLOAD, "True"))) {
            try {
                // Just use mem, this is going into mem cache anyway.
                byte[] bases =  downloadFromEBI(md5);
                if (bases != null) {
                    saveRefbasesToDisk(md5, bases);
                }
                return bases;
            } catch (Exception e) {
                log.warn("Could not download/save reference sequence for md5 " + md5, e);
            }
        }

        return null;
    }

    /**
     * Download reference sequence from EBI by MD5 and store it in the reference folder.
     * @param md5
     * @return bytes of the reference sequence, null if not found.
     * @throws IOException    if the sequence is not found or the download fails.
     */
    private byte[] downloadFromEBI(final String md5) throws IOException {
        final String url = String.format(Locale.US, Defaults.EBI_REFERENCE_SERVICE_URL_MASK, md5);

        for (int i = 0; i < DOWNLOAD_TRIES_BEFORE_FAILING; i++) {
            try (final InputStream is = new URL(url).openStream()) {
                if (is == null)
                    return null;

                log.info("Downloading reference sequence: {}", url);
                final byte[] bases = InputStreamUtils.readFully(is);
                log.info("Downloaded {} bytes for md5 {}", bases.length, md5);

                final String downloadedMD5 = SequenceUtil.calculateMD5String(bases);
                if (md5.equals(downloadedMD5)) {
                    return bases;
                } else {
                    log.error("Downloaded sequence is corrupt: requested md5={}, received md5={}",
                            md5, downloadedMD5);
                }
                return bases;
            }
            catch (final IOException e) {
                log.warn("Failed to download reference sequence for md5 {} on try {}/{}",
                        md5, (i + 1), DOWNLOAD_TRIES_BEFORE_FAILING, e);
            }
        }
        throw new IOException("Giving up on downloading sequence for md5 %s".formatted(md5));
    }

    private void saveRefbasesToDisk(String md5, byte[] bases) throws IOException {
        if (referenceFolder == null) return;

        Path basesPath = referenceFolder.resolve(REFBASES_PREFIX + md5 + REFBASES_EXT);
        Path tempBasesPath = PathUtils.getTempFilePath(basesPath);

        Files.write(tempBasesPath, bases);
        Files.move(tempBasesPath, basesPath);

        md5ToRefbases.put(md5, basesPath);
    }
}
