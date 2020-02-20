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

package org.gorpipe.gor.driver.adapters;

import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.File;

/**
 * Adapter methods for Sam libraries
 * Created by villi on 23/08/15.
 */
public class SamtoolsAdapter {

    /**
     * Create SamReader from file and index sources.
     */
    public static SamReader createReader(StreamSource source, StreamSource index) {
        return createReader(source, index, null);
    }

    /**
     * Create SamReader from file and index sources.
     */
    public static SamReader createReader(StreamSource source, StreamSource index, File ref) {
        SamReaderFactory srf = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT);
        if (ref != null) srf.referenceSource(new ReferenceSource(ref));
        SeekableStream bamStream = new StreamSourceSeekableStream(source);
        SamInputResource sir = SamInputResource.of(bamStream);
        if (index != null) {
            SeekableStream indexStream = new StreamSourceSeekableStream(index);
            sir.index(indexStream);
        }
        return srf.open(sir);
    }
}
