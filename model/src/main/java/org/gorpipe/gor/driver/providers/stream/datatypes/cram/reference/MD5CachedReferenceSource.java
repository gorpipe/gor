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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class MD5CachedReferenceSource implements CRAMReferenceSource, Closeable {
    private static final Logger log = LoggerFactory.getLogger(MD5CachedReferenceSource.class);

    public static final byte[] EMPTY_BASES = new byte[0];

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

        try {
            var bases = md5BasesCache.get(md5, () -> loadReference(record));
            StringUtil.toUpperCase(bases);
            return bases != EMPTY_BASES ? bases : null;
        }catch (ExecutionException e) {
            throw new GorDataException("Failed to load CRAM reference: " + md5, e);
        }
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
     * @return the bases, or EMPTY_BASES if no bases where found on disk and on EBI.
     */
    protected abstract byte[] loadReference(SAMSequenceRecord record);

    @Override
    public void close() throws IOException {

    }
}
