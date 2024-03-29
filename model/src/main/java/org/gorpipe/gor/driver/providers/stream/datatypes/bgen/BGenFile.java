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

package org.gorpipe.gor.driver.providers.stream.datatypes.bgen;

import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.providers.stream.StreamSourceFile;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.util.DataUtil;

import java.io.IOException;
import java.util.List;

public class BGenFile extends StreamSourceFile {
    public BGenFile(StreamSource fileSource) {
        super(fileSource);
    }

    public BGenFile(StreamSource fileSource, StreamSource indexSource) {
        super(fileSource, indexSource);
    }

    @Override
    public boolean supportsIndex() {
        return true;
    }

    @Override
    public List<String> possibleIndexNames() throws IOException {
        final List<String> result = super.possibleIndexNames();
        final String name = getFileSource().getSourceMetadata().getNamedUrl();
        if (DataUtil.isBgen(name)) {
            result.add(DataUtil.toFile(name, DataType.BGI));
        }
        return result;
    }
}
