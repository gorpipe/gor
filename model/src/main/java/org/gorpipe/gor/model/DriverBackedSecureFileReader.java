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

import org.gorpipe.gor.auth.AuthorizationAction;
import org.gorpipe.gor.auth.GorAuthRoleMatcher;
import org.gorpipe.gor.auth.SecurityPolicy;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gor server file reader.
 * Extends Default file reader adding path restrictions and special handling of result cache directories.
 */
public class DriverBackedSecureFileReader extends DriverBackedFileReader {

    private static final boolean USE_ROLE_ACCESS_VALIDATION_FOR_READ = Boolean.parseBoolean(System.getProperty("gor.access.read.validate.role", "false"));

    private static final String RESULT_CACHE_DIR = "cache/result_cache";
    private AccessControlContext accessControlContext;

    /**
     * Create reader
     *
     * @param commonRoot            resolved session root
     * @param constants             the session constants available for file reader
     * @param securityContext       access keys used by the driver
     * @param accessControlContext  access control context
     */
    public DriverBackedSecureFileReader(String commonRoot, Object[] constants,
                                        String securityContext, AccessControlContext accessControlContext) {
        super(securityContext, commonRoot, constants);

        this.accessControlContext = accessControlContext != null ? accessControlContext : AccessControlContext.builder().build();

        if (!this.accessControlContext.getWriteLocations().contains("result_cache")) {
            // Must create new acc object that allows us to add the write location.
            this.accessControlContext = new AccessControlContext(
                    this.accessControlContext.getAuthInfo(),
                    new ArrayList<>(this.accessControlContext.getWriteLocations()),
                    this.accessControlContext.isAllowAbsolutePath(),
                    this.accessControlContext.getSecurityPolicy());
            this.accessControlContext.getWriteLocations().add("result_cache");
        }
    }

    @Override
    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        if (dictionary.startsWith(RESULT_CACHE_DIR)) { // Files in Result Cache can be assumed to never change
            return Util.md5(dictionary);
        }
        return super.getDictionarySignature(dictionary, tags);
    }

    //@Override
    Stream<String> directDbUrl(String resolvedUrl) {
        throw new GorSystemException("Direct queries on db urls not allowed on server. Trying to open " + resolvedUrl, null);
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
        if (dataSource.getSourceReference().isWriteSource()) {
            validateWriteAccess(dataSource);
        } else {
            validateReadAccess(dataSource);
        }
    }

    private void validateReadAccess(DataSource source) throws GorResourceException {
        if (GorAuthRoleMatcher.hasRolebasedSystemAdminAccess(accessControlContext.getAuthInfo())) {
            return;
        }

        validateServerFileNames(source.getAccessValidationPaths());

        if (USE_ROLE_ACCESS_VALIDATION_FOR_READ && accessControlContext.getSecurityPolicy() != SecurityPolicy.NONE) {
            GorAuthRoleMatcher.needsRolebasedAccess(accessControlContext.getAuthInfo(),
                    source.getName(),
                    AuthorizationAction.READ);
        }
    }

    void validateWriteAccess(DataSource source) throws GorResourceException {
        if (GorAuthRoleMatcher.hasRolebasedSystemAdminAccess(accessControlContext.getAuthInfo())) {
            return;
        }

        String[] validationPaths = source.getAccessValidationPaths();
        validateServerFileNames(validationPaths);

        if (GorAuthRoleMatcher.hasRolebasedAccess(accessControlContext.getAuthInfo(), null,  AuthorizationAction.PROJECT_ADMIN)) {
            return;
        }

        if (!GorAuthRoleMatcher.hasRolebasedAccess(accessControlContext.getAuthInfo(), source.getName(),
                AuthorizationAction.WRITE)) {
            isWithinAllowedFolders(source);
        }

        for (String validationPath : validationPaths) {
            if (validationPath.toLowerCase().endsWith(".link")) {
                throw new GorResourceException("Writing link files is not allowed", null);
            }
        }
    }

    public void isWithinAllowedFolders(DataSource dataSource) {
        for (String validationPath : dataSource.getAccessValidationPaths()) {
            isWithinAllowedFolders(commonRoot, validationPath, accessControlContext.getWriteLocations());
        }
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
        throw new GorResourceException(message, fileName);
    }

    public void validateServerFileNames(String[] filenames) {
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
                throw new GorResourceException(message, filename);
            }
        }
    }
}
