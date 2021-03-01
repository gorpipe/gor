/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.exceptions;

public class GorParsingException extends GorUserException {

    private String option;
    private String payload;

    private int line;
    private int pos;

    public GorParsingException(String message) {
        this(message,"","");
    }

    public GorParsingException(String message, String option) {
        this(message, option, "");
    }

    public GorParsingException(String message, String option, String payload) {
        this(message, option, payload, 0, 0, null);
    }

    public GorParsingException(String message, Throwable cause) {
        this(message, "", "", 0, 0, cause);
    }

    public GorParsingException(String message, int line, int pos) {
        this(message, "", "", line, pos, null);
    }

    public GorParsingException(String message, String option, String payload, int line, int pos, Throwable cause) {
        super(message, cause);
        this.option = option;
        this.payload = payload;
        this.line = line;
        this.pos = pos;
    }

    public String getOption() {
        return this.option;
    }
    public void setOption(String option) { this.option = option; }
    public String getPayload() {
        return this.payload;
    }
    public void setPayload(String payload) { this.payload = payload; }

    public int getLine() {
        return line;
    }
    public void setLine(int line) {
        this.line = line;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());

        if (!ExceptionUtilities.isNullOrEmpty(option)) {
            builder.append("Option: ");
            builder.append(option);
            builder.append("\n");
        }

        if (!ExceptionUtilities.isNullOrEmpty(payload)) {
            builder.append("Option Value: ");
            builder.append(payload);
            builder.append("\n");
        }

        return builder.toString();
    }

}
