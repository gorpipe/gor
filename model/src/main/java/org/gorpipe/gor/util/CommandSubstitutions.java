package org.gorpipe.gor.util;

import gorsat.Commands.CommandParseUtilities;
import org.gorpipe.gor.driver.providers.rows.sources.db.DbScope;
import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.session.ProjectContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CommandSubstitutions {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommandSubstitutions.class);

    public static final String KEY_USER = "user";
    public static final String KEY_PROJECT = "project";
    public static final String KEY_PROJECT_ID = "project_id";
    public static final String KEY_DB_PROJECT_ID = "project-id";
    public static final String KEY_ORGANIZATION_ID = "organization_id";
    public static final String KEY_DB_ORGANIZATION_ID = "organization-id";

    public static final String KEY_REQUEST_ID = "request_id";
    public static final String KEY_CHROM = "chrom";
    public static final String KEY_BPSTART = "bpstart";
    public static final String KEY_BPSTOP = "bpstop";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_DATABASE = "database";

    /**
     * Process a list of commands and apply filter and seek substitutions.
     *
     * @param commands List of commands to process
     * @param seekChr  Chromosome to seek to, null if no seek
     * @param startPos Start position for seek
     * @param endPos   End position for seek, -1 if not specified
     * @param filter   Filter string, null if no filter
     * @return List of processed commands
     */
    public static List<String> cmdSetFilterAndSeek(List<String> commands, String seekChr, int startPos, int endPos, String filter) {
        List<String> filtercmd = filterCmd(commands, filter);
        List<String> seekcmd = new ArrayList<>();
        for (String cmd : filtercmd) {
            if (seekChr == null) {
                noSeekReplace(cmd, seekcmd);
            } else {
                seekReplace(cmd, seekcmd, seekChr, startPos, endPos);
            }
        }
        return seekcmd;
    }

    private static void noSeekReplace(String cmd, List<String> seekcmd) {
        int hPos;
        int sPos;
        hPos = cmd.indexOf("#(H:");
        if (hPos != -1) {
            int hEnd = cmd.indexOf(')', hPos + 1);
            cmd = cmd.substring(0, hPos) + cmd.substring(hPos + 4, hEnd) + cmd.substring(hEnd + 1);
        }

        sPos = cmd.indexOf("#(S:");
        if (sPos != -1) {
            int sEnd = cmd.indexOf(')', sPos + 1);
            cmd = cmd.substring(0, sPos) + cmd.substring(sEnd + 1);
        }

        if( sPos != -1 || hPos != -1 ) {
            seekcmd.addAll(Arrays.asList(cmd.split("[ ]+")));
        } else seekcmd.add(cmd);
    }

    private static String posReplace(String seek, String seekChr, int startPos, int endPos) {
        if( seekChr.startsWith("chr") ) seek = seek.replace("chn", seekChr.substring(3));
        else seek = seek.replace("chn", seekChr);
        int pos = seek.indexOf("pos-end");
        if (pos != -1) {
            if (endPos == -1) {
                int len = Integer.MAX_VALUE;
                seek = seek.replace("pos", (startPos + 1) + "").replace("end", len + "");
            } else {
                seek = seek.replace("pos", (startPos + 1) + "").replace("end", endPos + "");
            }
        } else if (seek.contains("pos")) {
            pos = seek.indexOf("pos-");
            if (endPos == -1) {
                seek = seek.replace("pos", startPos + "");
            } else if (startPos == endPos && pos != -1) {
                seek = seek.replace("pos-", startPos + "");
            } else {
                seek = seek.replace("pos", startPos+"").replace("end", endPos+"");
            }
        }

        return seek;
    }

    private static void seekReplace(String cmd, List<String> seekcmd, String seekChr, int startPos, int endPos) {
        int sPos;
        int hPos = cmd.indexOf("#(H:");
        if (hPos != -1) {
            int hEnd = cmd.indexOf(')', hPos + 1);
            cmd = cmd.substring(0, hPos) + cmd.substring(hEnd + 1);
        }

        sPos = cmd.indexOf("#(S:");
        if (sPos != -1) {
            int sEnd = cmd.indexOf(')', sPos + 1);
            String seek = cmd.substring(sPos + 4, sEnd).replace("chr", seekChr);
            seek = posReplace(seek, seekChr, startPos, endPos);
            cmd = cmd.substring(0, sPos) + seek + cmd.substring(sEnd + 1);
        }
        if( sPos != -1 || hPos != -1 ) {
            seekcmd.addAll(Arrays.asList(cmd.split("[ ]+")));
        } else seekcmd.add(cmd);
    }

    /**
     * Process a list of commands and apply filter substitutions.
     *
     * @param commands List of commands to process
     * @param filter   Filter string, null if no filter
     * @return List of processed commands
     */
    public static String filterCmd(String[] commands, String filter) {
        String[] ret = filterCmd(Arrays.asList(commands), filter).toArray(new String[0]);
        return String.join(" ", ret).trim();
    }

    private static List<String> filterCmd(List<String> commands, String filter) {
        List<String> seekcmd = new ArrayList<>();
        for (String cmd : commands) {
            if (filter == null) {
                int fPos = cmd.indexOf("#(F:");
                if (fPos != -1) {
                    cmd = "";
                }
            } else {
                int fPos = cmd.indexOf("#(F:");
                if (fPos != -1) {
                    int fEnd = CommandParseUtilities.quoteSafeIndexOf(cmd, ")", true, fPos+1);
                    if( fEnd == -1 ) fEnd = cmd.length()-1;
                    String filt = cmd.substring(fPos + 4, fEnd).replace("filter", filter);
                    cmd = cmd.substring(0, fPos) + filt + cmd.substring(fEnd + 1);
                }
            }
            if( cmd.length() > 0 ) seekcmd.add(cmd);
        }
        return seekcmd;
    }

    /**
     * Perform substitutions in a command string based on key-value pairs in a map.
     * @param command The command string containing placeholders in the format #{key}
     * @param map A map of key-value pairs for substitution
     * @return  updated command string with substitutions applied.
     */
    public static String commandSubstitutions(String command, Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (value != null) {
                command = command.replace("#{" + key + "}", value.toString());
            }
        }

        return command;
    }

    /**
     * Add parameters to a command string, replacing #{params} if present, otherwise appending to the end.
     * @param command The command string
     * @param paramString The parameters to add
     * @return The updated command string
     */
    public static String commandAddParams(String command, String paramString) {
        command = command.contains("#{params}") ? command.replace("#{params}", paramString) : command + " " + paramString;
        return command;
    }

    /**
     * Update a map with project and request information from the session.
     * @param session the current Gor session
     * @param map the map to update
     */
    public static Map<String, Object> updateMapWithProjectInfo(GorSession session, Map<String, Object> map) {
        if (map == null) return map;

        map.put(KEY_PROJECT, session.getProjectContext().getProjectName());
        map.put(KEY_REQUEST_ID, session.getRequestId());

        return updateMapFromProjectContext(session.getProjectContext(), map);
    }

    /**
     * Update a map with project and system properties from the ProjectContext.
     *
     * @param context The ProjectContext containing session and project information
     * @param map     The map to update with extracted values
     * @return The updated map
     */
    public static Map<String, Object> updateMapFromProjectContext(ProjectContext context, Map<String, Object> map) {
        if (map == null) return map;
        map = updateMapFromSecurityContext(context.getFileReader().getSecurityContext(), map);

        var projectRoot = context.getRoot().split("[ \t]+")[0];
        var cacheDir = context.getCacheDir();

        if (projectRoot != null && projectRoot.length() > 0) {
            Path rootPath = Paths.get(projectRoot);
            if (Files.exists(rootPath)) {
                try {
                    var rootRealPath = rootPath.toRealPath();
                    map.put("projectroot", rootRealPath.toString());

                    var cachePath = cacheDir != null ? Paths.get(cacheDir).toAbsolutePath() : rootRealPath.resolve("cache/result_cache");
                    if (Files.exists(cachePath)) {
                        map.put("projectcache", cachePath.toRealPath().getParent().toString());
                    }

                    Path dataPath = rootRealPath.resolve("source");
                    if (Files.exists(dataPath)) {
                        map.put("projectdata", dataPath.toRealPath().getParent().toString());
                    }
                } catch (IOException e) {
                    log.warn("Could not access project paths (rootpath=%s)".formatted(rootPath), e);
                }
            }
        }

        var csaroot = System.getProperty("csa.root");
        if (csaroot != null) {
            map.put("csaroot", Paths.get(csaroot));
        }

        var csacache = System.getProperty("csa.cache.root");
        if (csacache != null) {
            map.put("cacheroot", Paths.get(csacache));
        }

        return map;
    }

    /**
     * Update a map with project and organization IDs extracted from the security context string.
     *
     * @param securityContext The security context string containing project and organization information
     * @param map             The map to update with extracted values
     * @return The updated map
     */
    public static Map<String, Object> updateMapFromSecurityContext(String securityContext, Map<String, Object> map) {
        if (map == null) return map;

        var scopes = DbScope.parse(securityContext);
        for (var s : scopes) {
            if (s.getColumn().equalsIgnoreCase(KEY_PROJECT_ID)) {
                map.put(KEY_DB_PROJECT_ID, s.getValue());
                map.put(KEY_PROJECT_ID, s.getValue());
                map.put("projectid", s.getValue());
            } else if (s.getColumn().equalsIgnoreCase(KEY_ORGANIZATION_ID)) {
                map.put(KEY_DB_ORGANIZATION_ID, s.getValue());
                map.put(KEY_ORGANIZATION_ID, s.getValue());
            }
        }

        return map;
    }

    /**
     * Insert project context into a command string. Replaces #{projectroot}, #{projectdata}, #{projectcache},
     * #{csa.root}, #{cacheroot}, #{requestid}, #{projectid} and #{params}.
     *
     * @param cmd         Command string
     * @param paramString Parameters to insert
     * @param context     GorContext to get project information from
     * @return Command string with project context inserted
     * @throws IOException If an I/O error occurs
     */
    public static String insertProjectContext(String cmd, String paramString, GorContext context) throws IOException {
        var map = updateMapWithProjectInfo(context.getSession(), new HashMap<>());
        cmd = commandSubstitutions(cmd, map);
        cmd = commandAddParams(cmd, paramString);
        return cmd;
    }

    /**
     * Replace project related placeholders in the command string using information from the Gor session.
     * Replaces #{projectroot}, #{projectdata}, #{projectcache}, #{requestid}, and #{projectid}.
     *
     * @param myCommand The command string to process
     * @param session   The current Gor session
     * @return The command string with project related placeholders replaced
     * @throws IOException If an I/O error occurs
     */
    public static String projectReplacement(String myCommand, GorSession session) throws IOException {
        var map = updateMapWithProjectInfo(session, new HashMap<>());
        return commandSubstitutions(myCommand, map);
    }

}
