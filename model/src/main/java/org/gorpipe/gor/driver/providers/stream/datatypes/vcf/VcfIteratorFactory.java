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

package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import com.google.auto.service.AutoService;
import org.gorpipe.gor.driver.adapters.OffsetStreamSourceSeekableFile;
import org.gorpipe.gor.driver.adapters.StreamSourceSeekableFile;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.FileMetaIterator;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.binsearch.StringIntKey;
import org.gorpipe.gor.model.*;
import org.gorpipe.gor.util.DynamicRowIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@AutoService(StreamSourceIteratorFactory.class)
public class VcfIteratorFactory implements StreamSourceIteratorFactory {

    @Override
    public GenomicIterator createIterator(StreamSourceFile file) throws IOException {
        boolean compressed = file.getFileSource().getDataType() == DataType.VCFGZ || file.getFileSource().getDataType() == DataType.GVCFGZ || file.getFileSource().getDataType() == DataType.VCFBGZ;
        if (file.getIndexSource() == null || !file.getIndexSource().exists()) {
            // Use the same iterator for as the non-seekable .vcf.gz files as for .vcf files if ignore order is requested
            if (compressed) {
                return new VcfFileIterator(file, file.getFileSource().getSourceReference().getLookup(), compressed);
            } else {
                try (InputStream instream = file.getFileSource().open()) {
                    final Map<Integer, String> id2chr = new HashMap();
                    final Map<Integer, Integer> order2id = new HashMap();
                    final Map<Integer, Integer> id2order = new HashMap();
                    ContigDataScheme dataScheme = new ContigDataScheme() {
                        @Override
                        public String id2chr(int i) {
                            return id2chr.get(i);
                        }

                        @Override
                        public byte[] id2chrbytes(int i) {
                            return id2chr.get(i).getBytes();
                        }

                        @Override
                        public int id2order(int i) {
                            return id2order.get(i);
                        }

                        @Override
                        public void setId2order(int i, int val) {
                            id2order.put(i, val);
                            order2id.put(val, i);
                        }

                        @Override
                        public void setId2chr(int i, String chr) {
                            id2chr.put(i, chr);
                        }

                        @Override
                        public void newOrder(int[] neworder) {
                            for (int i = 0; i < neworder.length; i++) {
                                id2order.put(i, neworder[i]);
                                order2id.put(neworder[i], i);
                            }
                        }

                        @Override
                        public void newId2Chr(String[] newid2chr) {
                            for (int i = 0; i < newid2chr.length; i++) {
                                id2chr.put(i, newid2chr[i]);
                            }
                        }

                        @Override
                        public int order2id(int i) {
                            return order2id.get(i);
                        }

                        @Override
                        public int length() {
                            return id2chr.size();
                        }
                    };
                    final int[] info = VcfFile.findVcfGorDataOffset(instream, dataScheme);
                    final ContigDataScheme finalDataScheme;

                    final Comparator<StringIntKey> comparator;
                    if (dataScheme.length() == 0) {
                        finalDataScheme = ChrDataScheme.HG;
                        comparator = StringIntKey.cmpHumanGenomeMitoFirst();
                    } else {
                        final boolean addAnyChrToCache = true;
                        finalDataScheme = dataScheme;
                        ChromoCache lookupCache = new ChromoCache(dataScheme);
                        comparator = StringIntKey.customComparator(lookupCache);
                        ChromoLookup lookup = new ChromoLookup() {
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
                            public ChromoCache getChromCache() {
                                return lookupCache;
                            }
                        };
                        file.getFileSource().getSourceReference().setLookup(lookup);
                    }
                    StreamSourceSeekableFile sssf = new OffsetStreamSourceSeekableFile(file.getFileSource(), info[0]);
                    return new VcfSeekableIterator(sssf, comparator, finalDataScheme);
                }
            }
        } else {
            return new VcfIndexedFileIterator(file);
        }
    }

    @Override
    public GenomicIteratorBase createMetaIterator(StreamSourceFile file) throws IOException {
        var fileIt = new FileMetaIterator();
        fileIt.initMeta(file);
        return fileIt;
    }

    @Override
    public DataType[] getSupportedDataTypes() {
        return new DataType[]{DataType.VCF, DataType.GVCF, DataType.VCFGZ, DataType.VCFBGZ, DataType.GVCFGZ};
    }

    @Override
    public StreamSourceFile resolveFile(StreamSource source)
            throws IOException {
        switch (source.getDataType()) {
            case GVCF:
            case VCF:
                return new VcfFile(source);
            case VCFGZ:
            case VCFBGZ:
            case GVCFGZ:
                return new VcfGzFile(source);
            default:
                throw new RuntimeException("Unsupported data type " + source.getDataType());
        }
    }

}
