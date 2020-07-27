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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram;

import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;

import java.io.IOException;
import java.util.List;

/**
 * Represents a Cram file source
 * Created by villi on 23/08/15.
 */
public class CramFile extends StreamSourceFile {

    /**
     * Construct a CramFile using just the file source.
     * The index file name will be inferred from the file source.
     */
    public CramFile(StreamSource source) {
        super(source);
    }

    @Override
    public boolean supportsIndex() {
        return true;
    }

    @Override
    public boolean supportsReference() { return true; }


    @Override
    public List<String> possibleIndexNames() throws IOException {
        List<String> result = super.possibleIndexNames();
        String name = getFileSource().getSourceMetadata().getNamedUrl();
        if (name.toLowerCase().endsWith(".cram")) {
            result.add(name + ".crai");
            int endidx = name.length() - 5;
            result.add(name.substring(0, endidx) + ".crai");
        }
        return result;
    }


}
