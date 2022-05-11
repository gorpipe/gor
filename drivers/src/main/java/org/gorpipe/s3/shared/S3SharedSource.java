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

package org.gorpipe.s3.shared;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.base.Strings;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.s3.driver.*;

import java.net.MalformedURLException;

/**
 * Represents an object in Amazon S3 (created from S3Shared source reference).
 */
public class S3SharedSource extends S3Source {

    // Project link file is a link file within the project that points to this source.
    private String projectLinkFile;
    private String projectLinkFileContent;
    private String relativePath;
    private final S3SharedConfiguration s3SharedConfig;

    /**
     * Create source
     *
     */
    public S3SharedSource(AmazonS3Client client, SourceReference sourceReference,
                          String relativePath, S3SharedConfiguration s3SharedConfig) throws MalformedURLException {
        super(client, sourceReference);
        this.relativePath = relativePath;
        this.s3SharedConfig = s3SharedConfig;
    }

    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public String getProjectLinkFile() {
        return projectLinkFile;
    }

    public void setProjectLinkFile(String projectLinkFile) {
        this.projectLinkFile = projectLinkFile;
    }

    @Override
    public String getProjectLinkFileContent() {
        return !Strings.isNullOrEmpty(projectLinkFileContent) ? projectLinkFileContent : getFullPath();
    }

    public void setProjectLinkFileContent(String projectLinkFileContent) {
        this.projectLinkFileContent = projectLinkFileContent;
    }

    @Override
    public boolean forceLink() {
        return projectLinkFile != null && !projectLinkFile.isEmpty();
    }

    @Override
    public String getAccessValidationPath() {
        if (s3SharedConfig.onlyAccessWithLinksOnServer()
            && !getSourceReference().getOriginalSourceReference().isWriteSource() && !getSourceReference().isCreatedFromLink()) {
            throw new GorResourceException("S3 shared resources can only be accessed using links.", null);
        }

        if (getSourceReference().isCreatedFromLink()) {
            return getSourceReference().getOriginalSourceReference().getUrl();
        } else {
            return getRelativePath();
        }
    }
}
