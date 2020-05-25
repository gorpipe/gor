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

package org.gorpipe.querydialogs.beans.model;

/**
 * Class for describing a dialog.
 *
 * @version $Id$
 */
public class DialogDescription {
    /**
     * Dialog name.
     */
    public final String name;
    /**
     * Dialog detailed description.
     */
    public final String detailedDescr;
    /**
     * Dialog short description.
     */
    public final String shortDescr;

    public final String helpLink;

    /**
     * Constructor.
     *
     * @param name          dialog name
     * @param detailedDescr dialog detailed description
     * @param shortDescr    dialog short description
     * @param helpLink    dialog help link
     */
    public DialogDescription(final String name, final String detailedDescr, final String shortDescr, final String helpLink) {
        this.name = name;
        this.detailedDescr = detailedDescr;
        this.shortDescr = shortDescr;
        this.helpLink = helpLink;
    }
}

