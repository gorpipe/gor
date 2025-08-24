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

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.s3.driver.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.MalformedURLException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents an object in Amazon S3 (created from S3Shared source reference).
 */
public class S3SharedSource extends S3Source {

    // Project link file is a link file within the project that points to this source.
    private String projectLinkFile;
    private String projectLinkFileContent;
    private final String relativePath;
    private final S3SharedConfiguration s3SharedConfig;

    /**
     * Create source
     *
     */
    public S3SharedSource(S3Client client, S3AsyncClient asyncClient, SourceReference sourceReference,
                          String relativePath, S3SharedConfiguration s3SharedConfig) throws MalformedURLException {
        super(client, asyncClient, sourceReference);
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
        if (getSourceReference().isCreatedFromLink()) {
            return getSourceReference().getOriginalSourceReference().getUrl();
        } else {
            return getRelativePath();
        }
    }

    @Override
    protected String s3SubPathToUriString(Path p) {
        // Need to change the s3 path to s3data path.

        Path subPath = getPath().relativize(p);
        String uri = PathUtils.resolve(getSourceReference().getParentSourceReference().getUrl(), subPath.toString());
        String updatedUri = removeExtraFolder(uri);
        return PathUtils.formatUri(updatedUri.toString());
    }

    // Get extra folder, empty string if there is no extra folder.
    private String getExtraFolder(String path) {
        if (!path.toString().endsWith("/")) {
            String fileName = PathUtils.getFileName(path);
            int fileNameDotIndex = fileName.indexOf('.');
            String extraFolderCand = fileName.substring(0, fileNameDotIndex > 0 ? fileNameDotIndex : fileName.length());

            String parentPath = PathUtils.getParent(path);
            String parentParentPath = PathUtils.getParent(parentPath);
            if (!Strings.isNullOrEmpty(extraFolderCand) &&
                    extraFolderCand.equals(PathUtils.getFileName(parentPath)) &&
                    !Strings.isNullOrEmpty(parentParentPath)) {
                return extraFolderCand;
            }
        }
        return "";  // No extra folder
    }

    private String removeExtraFolder(String path) {
        if (!Strings.isNullOrEmpty(getExtraFolder(path))) {
            return PathUtils.resolve(PathUtils.getParent(PathUtils.getParent(path)), PathUtils.getFileName(path));
        } else {
            return path;
        }
    }

    @Override
    public SourceReference getTopSourceReference() {
        // Shared source should be access though links, so find the first link (which should be the direct access link)
        SourceReference top = getSourceReference();
        while (top.getParentSourceReference() != null && !DataUtil.isLink(top.getParentSourceReference().getUrl())) {
            top = top.getParentSourceReference();
        }
        return top;
    }

    // As S3SharedSource has artificial extra folders, we need to override the list method to get the correct list of files.
    @Override
    public Stream<String> list() {
        try {
            List<String> rawlist = new java.util.ArrayList<>();
            List<String> list = new java.util.ArrayList<>();
            Set<String> extraFolders = new HashSet<>();

            for (Path p: Files.walk(getPath(), 2).toList()) {
                extraFolders.add(getExtraFolder(p.toString()));
                rawlist.add(removeExtraFolder(p.toString()));
            }

            for (String p: rawlist) {
                String subPath = p.substring(getPath().toString().length());
                int subPathIndex = subPath.indexOf('/');
                if (subPath.equals("")
                        || extraFolders.contains(subPath)
                        || (subPathIndex > 0 && subPathIndex < subPath.length() - 1 )) {
                    // If the path is empty or it is just the extra folder, or it is deeper than level 1.
                    continue;
                }
                list.add(p);
            }

            return list.stream();
        } catch (Exception e) {
            Credentials cred = getCredentials(sourceReference.getSecurityContext(), "s3", bucket);
            throw new GorResourceException(String.format("List failed for %s, region: %s, access key: %s, secret key: %s",
                    getName(), client.serviceClientConfiguration().region(),
                    cred != null ? cred.getOrDefault(Credentials.Attr.KEY, "No key in creds") : "No creds",
                    cred != null ? (!StringUtils.isEmpty(cred.getOrDefault(Credentials.Attr.KEY, "")) ? "Has secret" : "Empty secret")
                            : "No creds"),
                    getName(), e).retry();
        }
    }
}
