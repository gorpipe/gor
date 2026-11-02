package gorsat.liftover;

import gorsat.Commands.CommandParseUtilities;
import gorsat.Iterators.SingleFileSource;
import gorsat.Utilities.IteratorUtilities;
import gorsat.process.SourceProvider;
import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.meta.SourceMetadata;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.session.GorContext;

import scala.jdk.javaapi.CollectionConverters;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chain index over a chain file, loaded lazily one contig at a time by seeking to the contig. Each contig is loaded
 * at most once, concurrent first callers share the load, and a failed load is not cached. Resolving the chain
 * argument and sharing sources through the session cache are static helpers here.
 */
public final class FileChainSource implements ChainSource {

    private final ResolvedChain chain;
    private final OverlapPolicy policy;
    private final GorContext context;
    private final ConcurrentHashMap<String, CompletableFuture<ContigIndex>> loaded = new ConcurrentHashMap<>();
    private final AtomicInteger loadCount = new AtomicInteger();
    private volatile Boolean nonEmpty;

    /** Does no IO. */
    FileChainSource(ResolvedChain chain, OverlapPolicy policy, GorContext context) {
        this.chain = chain;
        this.policy = policy;
        this.context = context;
    }

    /**
     * Resolves a chain argument, a file or a nested gor query {@code <(gor ...)}. Validates that the chain exists and
     * has the chain columns, and for a file takes its unique id, which keys the session cache.
     */
    public static ResolvedChain resolve(String chainArg, GorContext context) {
        String arg = CommandParseUtilities.replaceSingleQuotes(chainArg.trim());
        String root = context.getSession().getProjectContext().getRoot();

        if (arg.startsWith("<(")) {
            String query = CommandParseUtilities.parseNestedCommand(arg).trim();
            if (isNor(query)) {
                throw new GorParsingException("Liftover chain must be a gor query", "chain", arg);
            }
            SourceProvider provider = new SourceProvider(arg, context, false, false);
            ChainColumns cols = ChainColumns.fromHeader(provider.header(), arg);
            // SourceProvider.usedFiles is read before the header call that fills it, so it stays empty for a nested
            // query; the dynamic source holds the files once the header has been read.
            List<String> usedFiles = List.copyOf(CollectionConverters.asJava(provider.dynSource().usedFiles()));
            return new ResolvedChain(arg, root, cols, "", query, usedFiles);
        }

        FileReader fileReader = context.getSession().getProjectContext().getFileReader();
        if (!fileReader.exists(arg)) {
            throw new GorResourceException("Liftover chain file not found: " + arg, arg);
        }
        String header = IteratorUtilities.getHeader(arg, root, context);
        ChainColumns cols = ChainColumns.fromHeader(header, arg);
        return new ResolvedChain(arg, root, cols, uniqueId(fileReader, arg), null, List.of(arg));
    }

    private static boolean isNor(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return q.equals("nor") || (q.startsWith("nor") && Character.isWhitespace(q.charAt(3)));
    }

    /**
     * The unique id of the file contents from the source metadata. Never the file signature: that is never null, and
     * for an unresolvable source it is random, so such a cache key would never match and would grow the session cache.
     */
    static String uniqueId(FileReader fileReader, String path) {
        DataSource source = fileReader.resolveUrl(path);
        if (source == null) {
            throw new GorResourceException("Liftover chain " + path + ": no unique id", path);
        }
        try {
            SourceMetadata metadata = source.getSourceMetadata();
            String id = metadata == null ? null : metadata.getUniqueId();
            if (id == null || id.isEmpty()) {
                throw new GorResourceException("Liftover chain " + path + ": no unique id", path);
            }
            return id;
        } finally {
            source.close();
        }
    }

    /**
     * The session shared source for a file chain. Equal (path, unique id, policy) share one instance and so one
     * loaded index per contig. A nested chain is not shared, it gets a new source.
     */
    public static ChainSource cached(ResolvedChain chain, OverlapPolicy policy, GorContext context) {
        if (chain.nestedQuery() != null) {
            return new NestedChainSource(chain.nestedQuery(), chain.cols(), policy, context);
        }
        String key = "liftover_chain|" + chain.path() + "|" + chain.uniqueId() + "|" + policy;
        return (ChainSource) context.getSession().getCache().getObjectHashMap()
                .computeIfAbsent(key, k -> new FileChainSource(chain, policy, context));
    }

    int loadCount() {
        return loadCount.get();
    }

    @Override
    public ContigIndex contig(String chrom) {
        requireNonEmpty();
        CompletableFuture<ContigIndex> existing = loaded.get(chrom);
        if (existing == null) {
            CompletableFuture<ContigIndex> ours = new CompletableFuture<>();
            existing = loaded.putIfAbsent(chrom, ours);
            if (existing == null) {
                try {
                    ContigIndex index = load(chrom);
                    ours.complete(index);
                    return index;
                } catch (RuntimeException | Error e) {
                    ours.completeExceptionally(e);
                    loaded.remove(chrom, ours);
                    throw e;
                }
            }
        }
        return await(existing);
    }

    private static ContigIndex await(CompletableFuture<ContigIndex> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new GorSystemException("Liftover chain load failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GorSystemException("Interrupted while loading liftover chain", e);
        }
    }

    private ContigIndex load(String chrom) {
        loadCount.incrementAndGet();
        ContigIndexBuilder builder = new ContigIndexBuilder(chrom, policy, chain.path());
        SingleFileSource source = new SingleFileSource(chain.path(), chain.root(), context);
        try {
            source.seek(chrom, 0);
            while (source.hasNext()) {
                Row row = source.next();
                if (!row.chr.equals(chrom)) {
                    break;
                }
                builder.addRow(row, chain.cols());
            }
        } finally {
            source.close();
        }
        return builder.build();
    }

    /**
     * After a seek, a header-only file and a contig that sorts after every chain contig look the same, so
     * emptiness is checked once without seeking.
     */
    private void requireNonEmpty() {
        Boolean known = nonEmpty;
        if (known == null) {
            known = computeNonEmpty();
        }
        if (!known) {
            throw new GorDataException("Liftover chain " + chain.path() + " has no blocks");
        }
    }

    private synchronized boolean computeNonEmpty() {
        if (nonEmpty == null) {
            SingleFileSource source = new SingleFileSource(chain.path(), chain.root(), context);
            try {
                nonEmpty = source.hasNext();
            } finally {
                source.close();
            }
        }
        return nonEmpty;
    }

    @Override
    public String toString() {
        return "FileChainSource[" + chain.path() + ", " + policy + "]";
    }
}
