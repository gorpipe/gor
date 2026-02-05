package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.util.StringUtil;
import htsjdk.utils.ValidationUtils;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public abstract class MD5CachedReferenceSource implements CRAMReferenceSource, Closeable {
    private static final Logger log = LoggerFactory.getLogger(MD5CachedReferenceSource.class);

    private static final Cache<String, byte[]> md5BasesCache = createMd5BasesCache();

    private static Cache<String, byte[]> createMd5BasesCache () {
        RemovalListener<String, byte[]> removalNotifier;
        removalNotifier = notification -> log.debug("Removing from cache, key: {}, cause: {}",  notification.getKey(), notification.getCause());

        // TODO: Move this to owner config or context object when the dirver model supports that
        Integer cacheTimout = Integer.parseInt(System.getProperty("gor.driver.cram.referencetimeout", "60")); // Seconds

        return CacheBuilder.newBuilder().removalListener(removalNotifier)
                .expireAfterAccess(cacheTimout, TimeUnit.SECONDS).build();
    }

    @Override
    public synchronized byte[] getReferenceBases(final SAMSequenceRecord record,
                                                 final boolean tryNameVariants) {
        // check cache by sequence name:
        final String md5 = record.getMd5();

        if (Strings.isNullOrEmpty(md5)) {
            throw new GorDataException("Can not load reference bases as SAMSequenceRecord does not contain MD5");
        }

        byte[] bases = md5BasesCache.getIfPresent(md5);

        if (bases == null) {
            // Syncrhonize on md5 string to avoid multiple threads loading the same reference at the same time.
            synchronized (md5) {
                // Double check if another thread has loaded the reference while we were waiting for the lock.
                bases = md5BasesCache.getIfPresent(md5);
                if (bases == null) {
                    log.debug("Loading reference for md5 {}", md5);
                    bases = loadReference(record);
                    if (bases != null) {
                        // Normalize to upper case (that is what HTSJDK impl does).
                        StringUtil.toUpperCase(bases);
                        md5BasesCache.put(md5, bases);
                    }
                }
            }
        }

        return bases;
    }

    @Override
    public byte[] getReferenceBasesByRegion(
            final SAMSequenceRecord sequenceRecord,
            final int zeroBasedStart,
            final int requestedRegionLength) {

        ValidationUtils.validateArg(zeroBasedStart >= 0, "start must be >= 0");
        byte[] bases = getReferenceBases(sequenceRecord, false);
        if (bases != null) {
            if (zeroBasedStart >= bases.length) {
                throw new IllegalArgumentException(String.format("Requested start %d is beyond the sequence length %s",
                        zeroBasedStart,
                        sequenceRecord.getSequenceName()));
            }
            return Arrays.copyOfRange(bases, zeroBasedStart, Math.min(bases.length, zeroBasedStart + requestedRegionLength));
        }
        return bases;
    }

    /**
     * Load reference by MD5 from disk, downloading from EBI ENA if needed.
     * @param record       SAM record with the sequence detail (must include MD5).
     * @return the bases, or null if no bases where found on disk and on EBI.
     */
    protected abstract byte[] loadReference(SAMSequenceRecord record);

    @Override
    public void close() throws IOException {

    }
}
