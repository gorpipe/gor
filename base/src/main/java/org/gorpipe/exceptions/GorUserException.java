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

package org.gorpipe.exceptions;

public abstract class GorUserException extends GorException {

    private String commandName = "";
    private String commandStep = "";
    private String commandSource = "";
    private int commandIndex = -1;

    protected GorUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCommandName() {
        return this.commandName;
    }

    public void setCommandName(String command) {
        this.commandName = command;
    }

    public int getCommandIndex() {
        return this.commandIndex;
    }

    public void setCommandIndex(int commandIndex) {
        this.commandIndex = commandIndex;
    }

    public String getCommandStep() {
        return this.commandStep;
    }

    public void setCommandStep(String commandString) {
        this.commandStep = commandString;
    }

    public String getQuerySource() {
        return commandSource;
    }

    public void setQuerySource(String querySource) {
        this.commandSource = querySource;
    }

    public Boolean isCommandSet() {
        return this.commandName != null && this.commandName.length() > 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());

        if (!ExceptionUtilities.isNullOrEmpty(commandStep)) {
            builder.append("Command Step: ");
            builder.append(commandStep);
            builder.append("\n");
        }

        if (!ExceptionUtilities.isNullOrEmpty(commandName)) {
            builder.append("Command Name: ");
            builder.append(commandName);
            builder.append("\n");
        }

        if (!ExceptionUtilities.isNullOrEmpty(commandSource)) {
            builder.append("Command Source: ");
            builder.append(commandSource);
            builder.append("\n");
        }

        if (commandIndex >= 0) {
            builder.append("Command Index: ");
            builder.append(commandIndex);
            builder.append("\n");
        }

        return builder.toString();
    }
}
