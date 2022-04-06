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

import javax.swing.table.TableModel;
import java.util.WeakHashMap;

/**
 * Session Iterator iterates through gor data that are currently in memory.
 */
public class NamedTableGorIterators {
    private final static WeakHashMap<String, TableModel> tableModels = new WeakHashMap<>();

    /**
     * Adds a new iterator to the global iterator context
     *
     * @param name  name of the table model
     * @param model table model
     */
    public static void setTableModel(String name, TableModel model) {
        if (name != null) {
            tableModels.put(name, model);
        }
    }

    /**
     * @param name Name of the genomic iterator
     * @return iterator that is registered with the name
     */
    public static GenomicIterator getIterator(String name) {
        TableModel m = tableModels.get(name);
        if (m != null) {
            return new TableModelIterator(m);
        } else {
            return null;
        }
    }

    /**
     * Removes an iterator from the global context
     *
     * @param name Name of the iterator to remove
     */
    public static void removeIterator(String name) {
        tableModels.remove(name);
    }

}
