package org.gorpipe.gor.driver.providers.stream.datatypes.gord;

import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.DefaultChromoLookup;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.EmptyIterator;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorAdapterBase;
import org.gorpipe.gor.model.MergeIterator;
import org.gorpipe.gor.model.SourceRef;
import org.gorpipe.gor.table.Dictionary;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class GordIterator extends GenomicIteratorAdapterBase {
    private final StreamSource source;

    public GordIterator(StreamSource source) throws IOException {
        this.source = source;
        setIterator(createIterator());
    }

    private GenomicIterator createIterator() throws IOException {
        var sourceRef = source.getSourceReference();
        var fileReader = new DriverBackedFileReader(sourceRef.securityContext, sourceRef.commonRoot, sourceRef.queryTime);
        var dictionary = Dictionary.getDictionary(source.getFullPath(), fileReader, sourceRef.commonRoot, true);
        var dictionaryLines = dictionary.getSources(null, true, true);

        if (dictionaryLines.length == 0) {
            return new EmptyIterator("Chrom\tPos");
        }

        List<SourceRef> refs = new ArrayList<>();
        for (Dictionary.DictionaryLine line : dictionaryLines) {
            Collection<String> deletedTags = dictionary.getBucketDeletedFiles(Paths.get(line.fileRef.logical).getFileName().toString());
            addDictionaryLine(refs, line, sourceRef, fileReader, deletedTags);
        }

        if (refs.isEmpty()) {
            return new EmptyIterator("Chrom\tPos");
        }

        if (refs.size() == 1) {
            SourceRef ref = refs.get(0);
            GenomicIterator iterator = ref.iterate(new DefaultChromoLookup(), null).filter(r -> !r.isProgress);
            iterator.setSourceName(ref.getName());
            iterator.setSourceAlreadyInserted(ref.sourceAlreadyInserted);
            return iterator;
        }

        List<GenomicIterator> iterators = new ArrayList<>(refs.size());
        for (SourceRef ref : refs) {
            GenomicIterator iterator = ref.iterate(new DefaultChromoLookup(), null).filter(r -> !r.isProgress);
            iterator.setSourceName(ref.getName());
            iterator.setSourceAlreadyInserted(ref.sourceAlreadyInserted);
            iterators.add(iterator);
        }
        return new MergeIterator(iterators, true, null, null);
    }

    private void addDictionaryLine(List<SourceRef> refs, Dictionary.DictionaryLine dictionaryLine, SourceReference sourceRef, FileReader fileReader) throws IOException {
        addDictionaryLine(refs, dictionaryLine, sourceRef, fileReader, null);
    }

    private void addDictionaryLine(List<SourceRef> refs, Dictionary.DictionaryLine dictionaryLine, SourceReference sourceRef, FileReader fileReader, Collection<String> deletedTags) throws IOException {
        if (dictionaryLine.sourceInserted) {
            refs.add(new SourceRef(dictionaryLine.fileRef.logical, null, null, dictionaryLine.alias,
                    dictionaryLine.startChr, dictionaryLine.startPos, dictionaryLine.stopChr, dictionaryLine.stopPos,
                    dictionaryLine.tags != null ? new HashSet<>(dictionaryLine.tags) : null, deletedTags,
                    dictionaryLine.sourceInserted, sourceRef.securityContext, sourceRef.commonRoot));
            return;
        }

        String nestedPath = dictionaryLine.fileRef.physical;
        if (nestedPath != null && nestedPath.toLowerCase().endsWith(".gord")) {
            var nestedReference = new SourceReference(nestedPath, sourceRef);
            var nestedSource = (StreamSource) fileReader.resolveUrl(nestedReference);
            try {
                var nestedDictionary = Dictionary.getDictionary(nestedSource.getFullPath(), fileReader, sourceRef.commonRoot, true);
                var nestedLines = nestedDictionary.getSources(dictionaryLine.tags, true, true);
                for (Dictionary.DictionaryLine nestedLine : nestedLines) {
                    Collection<String> nestedDeletedTags = nestedDictionary.getBucketDeletedFiles(Paths.get(nestedLine.fileRef.logical).getFileName().toString());
                    addDictionaryLine(refs, nestedLine, sourceRef, fileReader, nestedDeletedTags);
                }
            } finally {
                nestedSource.close();
            }
            return;
        }

        refs.add(new SourceRef(
                dictionaryLine.fileRef.physical,
                null,
                null,
                dictionaryLine.alias,
                dictionaryLine.startChr,
                dictionaryLine.startPos,
                dictionaryLine.stopChr,
                dictionaryLine.stopPos,
                dictionaryLine.tags != null ? new HashSet<>(dictionaryLine.tags) : null,
                deletedTags,
                dictionaryLine.sourceInserted,
                sourceRef.securityContext,
                sourceRef.commonRoot
        ));
    }
}
