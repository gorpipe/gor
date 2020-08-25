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

package org.gorpipe.gor.model;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SharedChromSeqReferenceSource extends SharedCachedReferenceSource implements Closeable {

    private final String referencePath;

    public SharedChromSeqReferenceSource(String path, String referenceKey) {
        super(referenceKey);
        this.referencePath = path;
    }

    @Override
    protected byte[] loadReference(String name) {

        File referenceFile = new File(referencePath, name + ".txt");

        if (!referenceFile.exists()) {
            return new byte[0];
        }

        try {
            return Files.readAllBytes(referenceFile.toPath());
        } catch (IOException ex) {
            // we need to catch the exception and return an empty reference
        }

        return new byte[0];
    }
}
