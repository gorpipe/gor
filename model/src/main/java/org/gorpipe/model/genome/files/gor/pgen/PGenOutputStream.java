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

package org.gorpipe.model.genome.files.gor.pgen;

import java.io.IOException;

abstract class PGenOutputStream<T extends VariantRecord> implements AutoCloseable {
    protected static final byte MAGIC_BYTE_1 = 0x6c;
    protected static final byte MAGIC_BYTE_2 = 0x1b;

    private boolean firstRecord = true;
    protected int numberOfSamples;
    protected final String fileName;
    protected int numberOfVariants;

    protected PGenOutputStream(String fileName) {
        this.numberOfVariants = 0;
        this.fileName = fileName;
    }

    abstract void write(T record) throws IOException;

    protected void handleNumberOfSamples(T record) {
        if (this.firstRecord) {
            this.numberOfSamples = record.getNumberOfSamples();
            this.firstRecord = false;
        } else if (this.numberOfSamples != record.getNumberOfSamples()) {
            throw new IllegalArgumentException("The variant records must all have the same number of samples!");
        }
    }
}
