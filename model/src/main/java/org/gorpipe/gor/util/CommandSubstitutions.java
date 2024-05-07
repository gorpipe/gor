package org.gorpipe.gor.util;

import gorsat.Commands.CommandParseUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandSubstitutions {

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



}
