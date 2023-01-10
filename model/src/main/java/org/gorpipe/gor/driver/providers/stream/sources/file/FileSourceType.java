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

package org.gorpipe.gor.driver.providers.stream.sources.file;

import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.util.DataUtil;

public class FileSourceType extends SourceType {
    public static final FileSourceType FILE = new FileSourceType();

    private FileSourceType() {
        super("FILE", false, "");
    }

    @Override
    public boolean match(String file) {
        // TODO: Until we get better matching strategy we must exclude mem here.
        return !DataUtil.isMem(file) && super.match(file);
    }
}
