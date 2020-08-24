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

package org.gorpipe.querydialogs.argument;

import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.ArgumentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an argument that expects a file path.
 */
public class FileArgument extends Argument {
    private final List<String> fileFilterComponents;

    /**
     * Constructor.
     *
     * @param argDescr           argument description
     * @param optional           <code>true</code> if argument is optional, otherwise <code>false</code>
     * @param defaultValue       default value
     * @param advanced           <code>true</code> if an advanced argument (displayed in advanced section), otherwise <code>false</code>
     * @param displayWidth       the width of the argument when displayed
     * @param filterFileSuffixes the file suffixes to use when filtering files to select
     */
    public FileArgument(final ArgumentDescription argDescr, final Boolean optional, final Object defaultValue, final Boolean advanced,
                        final Integer displayWidth, final String filterFileSuffixes) {
        super(ArgumentType.FILE, argDescr, optional, defaultValue, null, null, null, advanced, displayWidth);
        this.fileFilterComponents = createFileFilterComponents(filterFileSuffixes);
    }

    /**
     * Constructor that copies the input file argument.
     *
     * @param arg the file argument to copy
     */
    public FileArgument(final FileArgument arg) {
        super(arg);
        this.fileFilterComponents = copyFileFilterComponents(arg.fileFilterComponents);
    }

    @Override
    public FileArgument copyArgument() {
        return new FileArgument(this);
    }

    /**
     * Get components for file filters (for example ['*.txt', '*.gor']).
     *
     * @return array of file filter components
     */
    public String[] getFileFilterComponents() {
        return fileFilterComponents.toArray(new String[fileFilterComponents.size()]);
    }

    private List<String> copyFileFilterComponents(final List<String> fileFiltersComponentsToCopy) {
        return new ArrayList<>(fileFiltersComponentsToCopy);
    }

    private List<String> createFileFilterComponents(final String filterFileSuffixes) {
        List<String> tmpFileFilterComponents = new ArrayList<String>();
        if (filterFileSuffixes != null && filterFileSuffixes.length() > 0) {
            final String[] splits = filterFileSuffixes.split(filterFileSuffixes, ',');
            for (String filterFileSuffix : splits) {
                tmpFileFilterComponents.add("*." + filterFileSuffix.trim());
            }
        }
        return tmpFileFilterComponents;
    }
}
