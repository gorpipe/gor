package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.Defaults;
import htsjdk.samtools.SAMSequenceRecord;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
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
        // Load from refbases file.
        Path refbasesPath = md5ToRefbases.get(record.getMd5());
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
        if (Boolean.parseBoolean(System.getProperty(KEY_USE_CRAM_REF_DOWNLOAD,
                Boolean.toString(Defaults.USE_CRAM_REF_DOWNLOAD)))) {
            try {

                byte[] bases =  downloadFromEBI(record.getMd5());
                if (bases != EMPTY_BASES) {
                    saveRefbasesToDisk(record.getMd5(), bases);
                }
                return bases;
            } catch (IOException e) {
                log.warn("Could not download/save reference sequence for md5 " + record.getMd5(), e);
            }
        }

        return EMPTY_BASES;
    }

    /**
     * Download reference sequence from EBI by MD5 and store it in the reference folder.
     * @param md5
     * @return bytes of the reference sequence, null if not found.
     * @throws IOException
     */
    private byte[] downloadFromEBI(String md5) throws IOException {
        log.info("Downloading reference {} from ENA", md5);
        URL url = new URL(String.format(Defaults.EBI_REFERENCE_SERVICE_URL_MASK, md5));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            log.warn("ENA returned {} for {}", conn.getResponseCode(), md5);
            return EMPTY_BASES;
        }

        byte[] bases;
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
            bases = in.readAllBytes();
        }
        if (bases.length == 0) return EMPTY_BASES;

        return bases;
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
