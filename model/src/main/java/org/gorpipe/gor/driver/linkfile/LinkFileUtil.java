package org.gorpipe.gor.driver.linkfile;

import gorsat.Commands.CommandParseUtilities;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.Strings;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkFileUtil {

    /**
     * Infer the data file name from the link file name.
     *
     * @param linkSource the link file path with the link extension
     * @param linkFileMeta additional link file meta data
     * @return the data file path
     */
    public static String inferDataFileNameFromLinkFile(StreamSource linkSource, String linkFileMeta) throws IOException {
        if (linkSource == null || Strings.isNullOrEmpty(linkSource.getFullPath())) {
            throw new IllegalArgumentException("Link file path is null or empty.  Can not infer data file name.");
        }

        var linkPath = linkSource.getSourceReference().getUrl();

        // Remove common the root if set.
        var pathReplacements = System.getenv("GOR_DRIVER_LINK_INFER_REPLACE");
        if (!Strings.isNullOrEmpty(pathReplacements)) {
            var parts = pathReplacements.split(";", 2);
            linkPath = linkPath.replaceAll(parts[0], parts.length > 1 ? parts[1] : "");
        }

        // Adjust the link path so it suitable as part of the data file path.
        if (PathUtils.isAbsolutePath(linkPath)) {
            throw new IllegalArgumentException("Link file path is absolute.  Can not infer data file name: " + linkSource.getFullPath());
        }

        var dataFileRootPath = "";

        // Get root from the link file
        var link = linkSource.exists()
                ? LinkFile.load(linkSource).appendMeta(linkFileMeta)
                : LinkFile.create(linkSource, linkFileMeta);

        var linkDataFileRootPath = link.getMeta().getProperty(LinkFileMeta.HEADER_DATA_LOCATION_KEY);
        if (!Strings.isNullOrEmpty(linkDataFileRootPath)) {
            dataFileRootPath = linkDataFileRootPath;
        } else if (link.getLatestEntry() != null) {
            dataFileRootPath = PathUtils.getParent(link.getLatestEntryUrl());
        }

        if (!Strings.isNullOrEmpty(linkDataFileRootPath)) {
            dataFileRootPath = linkDataFileRootPath;
        }

        // Get root from global const
        if (Strings.isNullOrEmpty(dataFileRootPath)) {
            dataFileRootPath = System.getenv(GorDriverConfig.GOR_DRIVER_LINK_MANAGED_DATA_ROOT_URL);

            // Insert project, only if we use global and global is set
            if (!Strings.isNullOrEmpty(dataFileRootPath)) {
                var project = linkSource.getSourceReference().getCommonRoot() != null
                        ? PathUtils.getFileName(linkSource.getSourceReference().getCommonRoot()) : "";
                if (!Strings.isNullOrEmpty(project)) {
                    dataFileRootPath = PathUtils.resolve(dataFileRootPath, project);
                }
            }
        }

        // Create a file name
        String uniqId = RandomStringUtils.insecure().next(8, true, true);
        var linkPathSplit = linkPath.indexOf('.');
        if (linkPathSplit > 0) {
            linkPath = "%s.%s.%s".formatted(
                    linkPath.substring(0, linkPathSplit),
                    uniqId,
                    linkPath.substring(linkPathSplit + 1));
        } else {
            linkPath = "%s.%s".formatted(linkPath, uniqId);
        }

        linkPath = linkPath.replaceAll("\\.link$", "");

        return PathUtils.resolve(dataFileRootPath, linkPath);
    }


    private static Pattern linkPattern = Pattern.compile(".* -link ([^\\s]*) ?.*", Pattern.CASE_INSENSITIVE);
    private static Pattern linkMetaPattern = Pattern.compile(".* -linkMeta [\"']([^\\s]*)[\"'] ?.*", Pattern.CASE_INSENSITIVE);

    public static String extractLinkOptionData(String options) {
        Matcher matcher = linkPattern.matcher(options);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String extractLinkMetaOptionData(String options) {
        Matcher matcher = linkMetaPattern.matcher(options);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    public record LinkData(String linkFile, String linkFileContent, String linkFileMeta, String linkFileInfo, String md5) {}

    public static LinkData extractLink(FileReader fileReader, String source, String optLinkFile, String optLinkFileMeta, String md5) {
        var linkFile = LinkFile.validateAndUpdateLinkFileName(optLinkFile);
        var linkFileContent = !Strings.isNullOrEmpty(linkFile) ? PathUtils.resolve(fileReader.getCommonRoot(), source) : "";

        if (Strings.isNullOrEmpty(linkFile) && !Strings.isNullOrEmpty(source)) {
            // Check if link file is forced from the source
            var dataSource = fileReader.resolveUrl(source, true);
            if (dataSource != null && dataSource.forceLink()) {
                linkFile = dataSource.getProjectLinkFile();
                linkFileContent = dataSource.getProjectLinkFileContent();
            }
        }
        var metaInfo = extractLinkMetaInfo(optLinkFileMeta);
        return new LinkData(linkFile, linkFileContent, metaInfo.linkFileMeta, metaInfo.linkFileInfo, md5);
    }

    public static LinkData extractLinkMetaInfo(String optLinkFileMeta) {
        var linkFileMeta = "";
        var linkFileInfo = "";

        if (!Strings.isNullOrEmpty(optLinkFileMeta)) {
            for (String s : CommandParseUtilities.quoteSafeSplit(StringUtils.strip(optLinkFileMeta, "\"\'"), ',')) {
                var l = s.trim();
                if (l.startsWith(LinkFileEntryV1.ENTRY_INFO_KEY)) {
                    linkFileInfo =  StringUtils.strip(l.substring(LinkFileEntryV1.ENTRY_INFO_KEY.length()  + 1), "\"\'");
                } else {
                    linkFileMeta += "## " + l + "\n";
                }
            }
        }

        return new LinkData("", "", linkFileMeta, linkFileInfo, "");
    }

    public static void writeLinkFile(FileReader fileReader, LinkData linkData) throws IOException {
        // Validate that we can write to the location (skip link extension as writing links is always forbidden).
        fileReader.resolveUrl(FilenameUtils.removeExtension(linkData.linkFile), true);

        // Use the nonsecure driver file reader as this is an exception from the write no links rule.
        var unsecureFileReader = new DriverBackedFileReader(fileReader.getSecurityContext(),
                fileReader.getCommonRoot(), fileReader.getQueryTime());

        LinkFile.load((StreamSource)unsecureFileReader.resolveUrl(linkData.linkFile, true))
                .appendMeta(linkData.linkFileMeta)
                .appendEntry(linkData.linkFileContent, linkData.md5, linkData.linkFileInfo, fileReader)
                .save(fileReader.getQueryTime());
    }
}
