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

import org.gorpipe.exceptions.GorSecurityException;
import org.gorpipe.gor.auth.AuthorizationAction;
import org.gorpipe.gor.auth.GorAuthRoleMatcher;
import org.gorpipe.gor.auth.SecurityPolicy;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.gorpipe.gor.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Gor server file reader.
 * Extends Default file reader adding path restrictions and special handling of result cache directories.
 */
public class DriverBackedSecureFileReader extends DriverBackedFileReader {

    private static final boolean USE_ROLE_ACCESS_VALIDATION_FOR_READ = Boolean.parseBoolean(System.getProperty("gor.access.read.validate.role", "false"));

    private static final String RESULT_CACHE_DIR = "cache/result_cache";
    private AccessControlContext accessControlContext;

    private DriverBackedFileReader unsecure;

    /**
     * Create reader
     *
     * @param commonRoot            resolved session root
     * @param securityContext       access keys used by the driver
     * @param accessControlContext  access control context
     */
    public DriverBackedSecureFileReader(String commonRoot,
                                        String securityContext, AccessControlContext accessControlContext) {
        this(commonRoot, securityContext, accessControlContext, System.currentTimeMillis());
    }

    public DriverBackedSecureFileReader(String commonRoot,
                                        String securityContext, AccessControlContext accessControlContext, long time) {
        super(securityContext, commonRoot, time);

        this.accessControlContext = accessControlContext != null ? accessControlContext : AccessControlContext.builder().build();
    }

    @Override
    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        if (dictionary.startsWith(RESULT_CACHE_DIR)) { // Files in Result Cache can be assumed to never change
            return Util.md5(dictionary);
        }
        return super.getDictionarySignature(dictionary, tags);
    }

    @Override
    public String getFileSignature(String file) throws IOException {
        // Notes:
        // 1. Handling of symbolic links:  We don't need any special handling of symbolic links as each file is resolved to its canonical form before getting
        //    the signature.  This is simpler and better for the caching (as two different queries using different links but the same actual files will
        //    hit the same cache).

        // Use md5 signature if it is available
        File md5file = new File(commonRoot + file + ".md5");
        if (md5file.exists()) {
            List<String> lines = java.nio.file.Files.readAllLines(md5file.toPath());
            if (!lines.isEmpty()) {
                return lines.get(0).trim();
            }
        }

        // Old cache fallback
        if (file.startsWith(RESULT_CACHE_DIR)) { // Files in Result Cache can be assumed to never change
            return Util.md5(file);
        }

        // Standard fallback
        return super.getFileSignature(file);
    }

    public AccessControlContext getAccessControlContext() {
        return accessControlContext;
    }

    @Override
    public void validateAccess(final DataSource dataSource) {
        dataSource.validateAccess();

        if (dataSource.getSourceReference().isWriteSource()) {
            validateWriteAccess(dataSource);
        } else {
            validateReadAccess(dataSource);
        }
    }

    @Override
    public boolean allowsAbsolutePaths() {
        return accessControlContext.isAllowAbsolutePath();
    }

    private void validateReadAccess(DataSource source) throws GorResourceException {
        validateServerFileNames(source.getAccessValidationPath());

        if (USE_ROLE_ACCESS_VALIDATION_FOR_READ && accessControlContext.getSecurityPolicy() != SecurityPolicy.NONE) {
            GorAuthRoleMatcher.needsRolebasedAccess(accessControlContext.getAuthInfo(),
                    PathUtils.relativize(URI.create(getCommonRoot()), source.getName()),
                    AuthorizationAction.READ);
        }
    }

    void validateWriteAccess(DataSource source) throws GorResourceException {
        // Could add global write access.  Leave this in here as an example.
        //if (GorAuthRoleMatcher.hasRolebasedSystemAdminAccess(accessControlContext.getAuthInfo())) {
        //    return;
        //}

        String validationPath = source.getAccessValidationPath();
        validateServerFileNames(validationPath);

        if (GorAuthRoleMatcher.hasRolebasedSystemAdminAccess(accessControlContext.getAuthInfo())) {
            return;
        }

        String relativeSource = PathUtils.relativize(URI.create(getCommonRoot()), source.getName());

        if (!GorAuthRoleMatcher.hasRolebasedAccess(accessControlContext.getAuthInfo(), relativeSource,
                AuthorizationAction.WRITE)) {
            isWithinAllowedFolders(source);
        }

        if (!GorAuthRoleMatcher.hasRolebasedAccess(accessControlContext.getAuthInfo(), relativeSource,
                AuthorizationAction.WRITE_LINK)) {
            if (DataUtil.isLink(validationPath)) {
                throw new GorSecurityException("Writing link files is not allowed", null);
            }
        }
    }

    public void isWithinAllowedFolders(DataSource dataSource) {
        isWithinAllowedFolders(commonRoot,  dataSource.getAccessValidationPath(), accessControlContext.getWriteLocations());
    }

    private static void isWithinAllowedFolders(String commonRoot, String fileName, List<String> writeLocations) {
        for (String location : writeLocations) {
            if (Path.of(PathUtils.resolve(commonRoot, fileName))
                    .startsWith(PathUtils.resolve(commonRoot, location+"/"))) {
                return;
            }
        }
        String message = String.format("Invalid File Path: File path not within folders allowed! Path given: %s. " +
                "Write locations are %s", fileName, Arrays.toString(writeLocations.toArray()));
        throw new GorSecurityException(message, null);
    }

    public void validateServerFileNames(String... filenames) {
        for (String filename : filenames) {
            validateServerFileName(filename, commonRoot, accessControlContext.isAllowAbsolutePath());
        }
    }

    public static void validateServerFileName(String filename, String projectRoot, boolean allowAbsolutePath) throws GorResourceException {
        if (PathUtils.isLocal(filename) && !allowAbsolutePath) {
            Path filePath = Paths.get(filename);
            var realProjectRoot = Paths.get(projectRoot);
            if (!filePath.isAbsolute()) {
                filePath = realProjectRoot.resolve(filePath);
            }
            filePath = PathUtils.relativize(realProjectRoot, filePath);
            if (filePath.isAbsolute() || !filePath.normalize().equals(filePath)) {
                String message = String.format("Invalid File Path: File paths must be within project scope! Path given: %s, Project root is: %s", filename, projectRoot);
                throw new GorSecurityException(message, null);
            }
        }
    }

    public static URI[] getProjectRelativePaths(String projectRoot, String... filenames) {
        var realProjectRoot = URI.create(projectRoot);

        URI[] ret = new URI[filenames.length];
        for (int i = 0; i < filenames.length; i++) {
            ret[i] =  PathUtils.relativize(realProjectRoot, URI.create(filenames[i]));
        }

        return ret;
    }

    public static void validateServerProjectRelativeURI(URI uri, String projectRoot, boolean allowsAbsolutePath) {
        if (PathUtils.isLocal(uri) && !allowsAbsolutePath
                && (PathUtils.isAbsolutePath(uri) || !uri.normalize().equals(uri))) {
            String message = String.format("Invalid File Path: File paths must be within project scope! Path given: %s, Project root is: %s", uri.toString(), projectRoot);
            throw new GorResourceException(message, PathUtils.resolve(projectRoot, uri.toString()));
        }
    }

    public void validateServerURIs(URI... uris) {
        for (URI uri : uris) {
            validateServerProjectRelativeURI(uri, commonRoot, allowsAbsolutePaths());
        }
    }

    @Override
    public DriverBackedFileReader unsecure() {
        if (unsecure == null) {
            unsecure = new DriverBackedFileReader(getSecurityContext(), getCommonRoot(), getQueryTime());
        }
        return unsecure;
    }
}
