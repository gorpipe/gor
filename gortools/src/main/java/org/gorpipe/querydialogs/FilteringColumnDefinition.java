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

package org.gorpipe.querydialogs;

import javax.swing.*;
import java.awt.*;


/**
 * Interface for filtering column definition.
 *
 * @version $Id$
 */
public interface FilteringColumnDefinition {

    /**
     * Check if the column definition has a short description.
     *
     * @return <code>true</code> if the column definition has a short description, otherwise <code>false</code>
     */
    boolean hasShortDescription();

    /**
     * Get the short description of column (few words).
     *
     * @return the short description of column
     */
    String getShortDescription();

    /**
     * Check if the column definition has a description.
     *
     * @return <code>true</code> if the column definition has a description, otherwise <code>false</code>
     */
    boolean hasDescription();

    /**
     * Get the description of the column (one sentence).
     *
     * @return the description of the column
     */
    String getDescription();

    /**
     * Check if help is defined for column.
     *
     * @return <code>true</code> if help is defined for column, otherwise <code>false</code>
     */
    boolean hasColumnHelp();

    /**
     * Display detailed help for the column.
     *
     * @param parent the parent window of the component displaying help
     */
    void displayHelp(final Window parent);

    /**
     * The description is displayed in a panel. If the description does not fit in the panel a '...more' hyperlink is added at the end of
     * the partial description. When the hyperlink is pressed a popup dialog should display the complete description.
     *
     * @param toolTipViewer a tooltip viewer to use for pinning description
     * @return panel for displaying the description
     */
    JPanel getDescriptionPanelWithHyperlink(final JComponent toolTipViewer);
}
