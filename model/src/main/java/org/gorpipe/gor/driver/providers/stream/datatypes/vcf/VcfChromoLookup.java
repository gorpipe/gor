package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.gorpipe.gor.model.ChromoCache;
import org.gorpipe.gor.model.ChromoLookup;
import org.gorpipe.gor.model.ContigDataScheme;

public class VcfChromoLookup implements ChromoLookup {

    private ContigDataScheme dataScheme;
    private ChromoCache lookupCache;
    private boolean addAnyChrToCache = true;

    public VcfChromoLookup(ContigDataScheme dataScheme, boolean addAnyChrToCache) {
        this.dataScheme = dataScheme;
        this.lookupCache = new ChromoCache(dataScheme);;
        this.addAnyChrToCache = addAnyChrToCache;
    }

    public ChromoCache getLookupCache() {
        return lookupCache;
    }

    public ContigDataScheme getDataScheme() {
        return dataScheme;
    }

    @Override
    public final String idToName(int id) {
        return lookupCache.toName(dataScheme, id);
    }

    @Override
    public final int chrToId(String chr) {
        return lookupCache.toIdOrUnknown(chr, addAnyChrToCache);
    }

    @Override
    public final int chrToLen(String chr) {
        return lookupCache.toLen(chr);
    }

    @Override
    public final int chrToId(CharSequence str, int strlen) {
        return lookupCache.toIdOrUnknown(str, strlen, addAnyChrToCache);
    }

    @Override
    public final int prefixedChrToId(byte[] buf, int offset) {
        return lookupCache.prefixedChrToIdOrUnknown(buf, offset, addAnyChrToCache);
    }

    @Override
    public final int prefixedChrToId(byte[] buf, int offset, int buflen) {
        return lookupCache.prefixedChrToIdOrUnknown(buf, offset, buflen, addAnyChrToCache);
    }

    @Override
    public ChromoCache getChromoCache() {
        return lookupCache;
    }
}
