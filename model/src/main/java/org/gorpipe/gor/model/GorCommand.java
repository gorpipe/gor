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

import java.util.ArrayList;

/**
 * Gor Command
 */
public class GorCommand {
    private final String cmd;
    private String cmdNoComment = null;
    private ArrayList<CmdPos> positions = null;
    private boolean commentsFound = false;

    /**
     * @param cmd The command string
     */
    public GorCommand(String cmd) {
        this.cmd = cmd;
    }

    /**
     * @return The command string without comments
     */
    public String getWithoutComments() {
        if (cmdNoComment == null) {
            cmdNoComment = removeComments();
        }
        return cmdNoComment;
    }

    /**
     * @param posWithoutComment Position in a command string that has beens tripped comments
     * @return the position of the command in the string with comments
     */
    public int posWithComment(int posWithoutComment) {
        if (cmdNoComment == null) {
            cmdNoComment = removeComments();
        }
        int pos = posWithoutComment;
        if (commentsFound) {
            for (CmdPos cmp : positions) {
                if (cmp.pos <= posWithoutComment) {
                    pos = pos + cmp.commentLen;
                } else {
                    break;
                }
            }
        }
        return pos;
    }

    /**
     * Check if is NOR.
     *
     * @return <code>true</code> if is NOR, otherwise <code>false</code>
     */
    public boolean isNor() {
        return cmd.trim().startsWith("nor ");
    }

    private String removeComments() {
        positions = new ArrayList<CmdPos>();
        StringBuilder rc = new StringBuilder(cmd);
        int startOfComment = -1;
        int nestedCount = 0;
        int charsRemoved = 0;
        for (int i = 0; i < cmd.length() - 1; i++) {
            char ch1 = cmd.charAt(i);
            char ch2 = cmd.charAt(i + 1);
            if (ch1 == '/' && ch2 == '*') {
                if (startOfComment >= 0) {
                    nestedCount++;
                } else {
                    if(cmd.length() <= i+2 || cmd.charAt(i + 2) != '+') startOfComment = i;
                }
            } else if (ch1 == '*' && ch2 == '/') {
                if (nestedCount > 0) {
                    nestedCount--;
                } else if (startOfComment >= 0) {
                    commentsFound = true;
                    int end = i + 2;
                    int delFrom = startOfComment - charsRemoved;
                    int delTo = end - charsRemoved;
                    rc.delete(delFrom, delTo);
                    CmdPos cp = new CmdPos(delFrom, delTo - delFrom);
                    positions.add(cp);
                    charsRemoved = charsRemoved + end - startOfComment;
                    startOfComment = -1;
                }
            }
        }
        return rc.toString();
    }

    /**
     * @param cmd Gor Command
     * @return command with user contstants
     */
    public static String replaceClientConstants(String cmd) {
        String t = cmd;
        if (Constants.isSet()) {
            t = t.replace("#{project}", Constants.get().projectName());
            t = t.replace("#{user}", Constants.get().userName());
        }
        return t;
    }

    static class CmdPos implements Comparable<CmdPos> {
        public CmdPos(int pos, int len) {
            this.pos = pos;
            this.commentLen = len;
        }

        public int pos;
        public int commentLen;

        @Override
        public int compareTo(CmdPos o) {
            return (pos < o.pos) ? -1 : ((pos == o.pos) ? 0 : 1);

        }
    }

}
