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

package org.gorpipe.gor.cli.cache;

import org.gorpipe.util.string.StringUtil;

import java.nio.file.Path;
import java.util.*;

class AnalysisResult {

    private Path cacheFolder;
    private Map<String, Integer> extensionCountMap = new HashMap<>();
    private List<String> fileList = new ArrayList<>();

    AnalysisResult(Path cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    Map<String, Integer> getExtensionCountMap() {
        return Collections.unmodifiableMap(this.extensionCountMap);
    }

    List<String> getFileList() {
        return Collections.unmodifiableList(this.fileList);
    }

    public void process(Path file) {
        if (!file.startsWith(cacheFolder)) {
            return;
        }

        Path relativeFilePath = cacheFolder.relativize(file);
        fileList.add(relativeFilePath.toString());

        String extension = getFileExtension(relativeFilePath);

        if (!StringUtil.isEmpty(extension)) {
            if (extensionCountMap.containsKey(extension)) {
                extensionCountMap.replace(extension, extensionCountMap.get(extension) + 1);
            } else {
                extensionCountMap.put(extension, 1);
            }
        }
    }

    private static String getFileExtension(Path file) {
        String extension = "";

        try {
            if (file != null) {
                String name = file.getFileName().toString();
                extension = name.substring(name.lastIndexOf('.'));
            }
        } catch (Exception e) {
            extension = "";
        }

        return extension.toLowerCase().trim();
    }
}
