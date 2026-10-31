package gorsat.liftover;

import gorsat.DynIterator.DynamicRowSource;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.GorContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chain index over a nested gor query. The query is read once, on the first contig call, into one index per
 * contig, validated like a chain file. A failed load is not kept, the next call loads again.
 */
public final class NestedChainSource implements ChainSource {

    private final String query;
    private final ChainColumns cols;
    private final OverlapPolicy policy;
    private final GorContext context;
    private final AtomicInteger loadCount = new AtomicInteger();
    private volatile Map<String, ContigIndex> contigs;

    public NestedChainSource(String query, ChainColumns cols, OverlapPolicy policy, GorContext context) {
        this.query = query;
        this.cols = cols;
        this.policy = policy;
        this.context = context;
    }

    int loadCount() {
        return loadCount.get();
    }

    @Override
    public ContigIndex contig(String chrom) {
        Map<String, ContigIndex> all = contigs;
        if (all == null) {
            all = loadAll();
        }
        ContigIndex index = all.get(chrom);
        return index != null ? index : ContigIndex.empty(chrom);
    }

    private synchronized Map<String, ContigIndex> loadAll() {
        if (contigs != null) {
            return contigs;
        }
        loadCount.incrementAndGet();
        String source = "<(" + query + ")";
        Map<String, ContigIndexBuilder> builders = new LinkedHashMap<>();
        DynamicRowSource rows = new DynamicRowSource(query, context, true);
        try {
            while (rows.hasNext()) {
                Row row = rows.next();
                builders.computeIfAbsent(row.chr, c -> new ContigIndexBuilder(c, policy, source)).addRow(row, cols);
            }
        } finally {
            rows.close();
        }
        if (builders.isEmpty()) {
            throw new GorDataException("Liftover chain " + source + " has no blocks");
        }
        Map<String, ContigIndex> built = new LinkedHashMap<>();
        builders.forEach((c, b) -> built.put(c, b.build()));
        contigs = built;
        return built;
    }
}
