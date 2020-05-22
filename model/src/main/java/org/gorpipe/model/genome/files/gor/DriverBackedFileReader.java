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

package org.gorpipe.model.genome.files.gor;

import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.adapters.StreamSourceRacFile;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSourceType;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.util.gorutil.standalone.GorStandalone;
import org.gorpipe.model.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Created by villi on 06/01/17.
 */
public class DriverBackedFileReader extends FileReader {
    private static final Logger log = LoggerFactory.getLogger(DriverBackedFileReader.class);

    private final String securityContext;
    private final Object[] constants;
    protected final String commonRoot;

    DriverBackedFileReader(String securityContext) {
        this(securityContext, null, new Object[]{});
    }

    public DriverBackedFileReader(String securityContext, String commonRoot, Object[] constants) {
        this.securityContext = securityContext;
        if ((commonRoot == null || commonRoot.length() < 1) && GorStandalone.isStandalone()) {
            this.commonRoot = GorStandalone.getStandaloneRoot();
        } else {
            this.commonRoot = commonRoot;
        }
        this.constants = constants;
    }

    /**
     * Resolve the given url, this includes traversing .link files and do fallback to link files if the file does not exits.
     *
     * @param url the url to resolve.
     * @return the resolved url.
     */
    public DataSource resolveUrl(String url) throws IOException {
        SourceReference sourceReference = new SourceReferenceBuilder(url).commonRoot(commonRoot).securityContext(securityContext).build();

        return GorDriverFactory.fromConfig().getDataSource(sourceReference);
    }

    private static String getResolvedUrl(DataSource ds) throws IOException {
        String cannonicalName = ds.getSourceMetadata().getCanonicalName();
        return cannonicalName.startsWith("file://") ? cannonicalName.substring(7) : cannonicalName;
    }

    @Override
    public String getSecurityContext() {
        return securityContext;
    }

    @Override
    protected void checkValidServerFileName(String fileName) {
        // This is not used for the standard reader
    }

    @Override
    public String[] readAll(String file) throws IOException {
        try (Stream<String> s = readFile(file)) {
            return s.toArray(String[]::new);
        }
    }

    @Override
    public String readHeaderLine(String file) throws IOException {
        final DataSource source = resolveUrl(file);
        String url = getResolvedUrl(source);
        if (url.startsWith("//db:")) {
            final int idxSelect = url.indexOf("select ");
            final int idxFrom = url.indexOf(" from ");
            if (idxSelect < 0 || idxFrom < 0) { // Must find columns
                return null;
            }
            final ArrayList<String> fields = StringUtil.split(url, idxSelect + 7, idxFrom, ',');
            final StringBuilder header = new StringBuilder(200);
            for (String f : fields) {
                if (header.length() > 0) {
                    header.append('\t');
                } else {
                    header.append('#');
                }
                final int idxAs = f.indexOf(" as ");
                if (idxAs > 0) {
                    header.append(f.substring(idxAs + 4).trim());
                } else {
                    final int idxPoint = f.indexOf('.');
                    header.append(f.substring(idxPoint > 0 ? idxPoint + 1 : 0).trim());
                }
            }
            return header.toString();
        }

        try (InputStream str = ((StreamSource) source).open()) {
            BufferedReader r = new BufferedReader(new InputStreamReader(str));
            return r.readLine();
        } finally {
            if (source != null) source.close();
        }
    }

    @Override
    public Stream<String> readFile(String file) throws IOException {
        return readAndClose(resolveUrl(file));
    }

    @Override
    public Path toPath(String resource) {
        return Paths.get(resource);
    }

    @Override
    public BufferedReader getReader(Path path) throws IOException {
        return getReader(path.toString());
    }

    @Override
    public BufferedReader getReader(String resource) throws IOException {
        DataSource source = resolveUrl(resource);
        String resolvedUrl = getResolvedUrl(source);
        return readSource(source, resolvedUrl);
    }

    @Override
    public Stream<String> iterateFile(String file, int maxDepth, boolean showModificationDate) throws IOException {
        DataSource source = resolveUrl(file);

        if (source.getSourceType() == FileSourceType.FILE) {
            File f = new File(getResolvedUrl(source));
            if (f.isDirectory()) {
                Path path = f.toPath();
                Path root = commonRoot != null ? Paths.get(commonRoot) : null;
                return DefaultFileReader.getDirectoryStream(maxDepth, showModificationDate, path, root);
            }
        }

        return readAndClose(source);
    }

    @Override
    public RacFile openFile(String file) throws IOException {
        DataSource source = resolveUrl(file);
        return new StreamSourceRacFile((StreamSource) source);
    }

    @Override
    public String getDictionarySignature(String dictionary, String[] tags) throws IOException {
        final DataSource file = resolveUrl(dictionary);
        return new DictionaryTable.Builder<>(Paths.get(getResolvedUrl(file))).securityContext(securityContext).build().getSignature(true, file.getSourceReference().commonRoot, tags);
    }

    @Override
    public String getFileSignature(String file) throws IOException {
        DataSource source = resolveUrl(file);
        return GorOptions.getFileSignature(source);
    }

    private BufferedReader readSource(DataSource source, String resolvedUrl) {
        try {
            return new BufferedReader(new InputStreamReader(((StreamSource) source).open()));
        } catch (IOException e) {
            String name = "";
            try {
                name = source.getName();
            } catch (IOException ex) {
                // Do nothing
            }
            throw ExceptionUtilities.mapGorResourceException(name, resolvedUrl, e);
        }
    }

    Stream<String> directDbUrl(String resolvedUrl) throws IOException {
        return DbSource.getDBLinkStream("//db:select * from " + resolvedUrl.substring(resolvedUrl.indexOf(':', 5) + 1), constants);
    }

    private Stream<String> readAndClose(DataSource source) throws IOException {
        String resolvedUrl = getResolvedUrl(source);

        if (resolvedUrl.startsWith("//db:")) {
            source.close();
            return DbSource.getDBLinkStream(resolvedUrl, constants);
        } else if (source instanceof org.gorpipe.gor.driver.providers.db.DbSource) {
            source.close();
            return directDbUrl(resolvedUrl);
        }

        BufferedReader br = readSource(source, resolvedUrl);
        Stream<String> lineStream = br.lines();
        lineStream.onClose(() -> {
            try {
                br.close();
                source.close();
            } catch (IOException e) {
                log.warn("Could not close file!", e);
            }
        });
        return lineStream;
    }

}
