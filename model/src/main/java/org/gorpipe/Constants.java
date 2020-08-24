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

package org.gorpipe;


/**
 * Constants allow querying of an application wide constant
 *
 * @version $Id$
 */
public abstract class Constants {
    private static Constants constants = null;

    /**
     * @return The system level constants
     */
    public static Constants get() {
        assert constants != null;
        return constants;
    }

    /**
     * Set the active constants for the application
     *
     * @param consts The constants
     */
    public static void set(Constants consts) {
        constants = consts;
    }

    /**
     * @return true if constants have been set
     */
    public static boolean isSet() {
        return constants != null;
    }

    /**
     * Replace all occurances of the constants found the the specified text using default Constants, if set
     *
     * @param text
     * @return The text after replacement
     */
    public static String replaceConstantsInText(String text) {
        return isSet() ? get().replaceConstantsInTextx(text) : text;
    }

    /**
     * Replace all occurances of the constants found the the specified text
     *
     * @param text
     * @return The text after replacement
     */
    public String replaceConstantsInTextx(String text) {
        String t = text.replace("#{project-id}", String.valueOf(projectId()));
        t = t.replace("#{project}", projectName());
        t = t.replace("#{user}", userName());
        return t;
    }

    /**
     * Check if project has changed.
     *
     * @param oldProjectName old project name
     * @return <code>true</code> if project has changed, otherwise <code>false</code>
     */
    public static boolean projectChanged(final String oldProjectName) {
        boolean configChanged = false;
        if (Constants.isSet()) {
            final String constantsProjectName = Constants.get().projectName();
            if (!constantsProjectName.equals(oldProjectName)) {
                configChanged = true;
            }
        }
        return configChanged;
    }

    /**
     * @return The current project name
     */
    abstract public String projectName();

    /**
     * @return The current loged in user
     */
    abstract public String userName();

    /**
     * @return The current project id
     */
    abstract public int projectId();
}
