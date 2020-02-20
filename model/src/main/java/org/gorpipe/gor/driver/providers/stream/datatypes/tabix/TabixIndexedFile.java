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

package org.gorpipe.gor.driver.providers.stream.datatypes.tabix;

import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

import java.io.IOException;
import java.util.List;

/**
 * Created by sigmar on 12/10/15.
 */
public class TabixIndexedFile extends StreamSourceFile {

    //TODO Readd CSI support (GOR-443):  public static String[] indexSuffixes = {"tbi","csi"};
    public static String[] indexSuffixes = {"tbi"};

    /**
     * Construct a TabixIndexedFile using just the file source.
     * The index file name will be inferred from the file source.
     */
    public TabixIndexedFile(StreamSource vcfSource) {
        super(vcfSource);
    }

    /**
     * Construct a TabixIndexedFile using file and index sources
     * The index file name will be inferred from the file source.
     */
    public TabixIndexedFile(StreamSource vcfGzSource, StreamSource indexSource) {
        super(vcfGzSource, indexSource);
    }

    @Override
    public List<String> possibleIndexNames() throws IOException {
        List<String> result = super.possibleIndexNames();
        String name = getFileSource().getSourceMetadata().getNamedUrl();
        for (int i = 0; i < indexSuffixes.length; i++) {
            result.add(name + "." + indexSuffixes[i]);
        }
        return result;
    }

    @Override
    public boolean supportsIndex() {
        return true;
    }
}
