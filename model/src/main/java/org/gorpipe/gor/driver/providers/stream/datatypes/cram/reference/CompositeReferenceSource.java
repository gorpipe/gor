package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Composite reference source, tries out different reference source in order.
 */
public class CompositeReferenceSource implements CRAMReferenceSource, Closeable  {

    List<CRAMReferenceSource> sources;

    public CompositeReferenceSource(List<CRAMReferenceSource> sources) {
        this.sources =  sources != null ? new ArrayList<>(sources) : new ArrayList<>();
    }

    @Override
    public byte[] getReferenceBases(SAMSequenceRecord sequenceRecord, boolean tryNameVariants) {
        byte[] bytes = null;
        for (var source : sources) {
            bytes = source.getReferenceBases(sequenceRecord, tryNameVariants);
            if (bytes != null) {
                return bytes;
            }
        }
        return bytes;
    }

    @Override
    public byte[] getReferenceBasesByRegion(SAMSequenceRecord sequenceRecord, int zeroBasedStart, int requestedRegionLength) {
        byte[] bytes = null;
        for (var source : sources) {
            bytes = source.getReferenceBasesByRegion(sequenceRecord, zeroBasedStart, requestedRegionLength);
            if (bytes != null) {
                return bytes;
            }
        }
        return bytes;
    }

    @Override
    public void close() throws IOException {
        for (var source : sources) {
            if (source instanceof Closeable) {
                ((Closeable) source).close();
            }
        }
        sources.clear();
    }
}
