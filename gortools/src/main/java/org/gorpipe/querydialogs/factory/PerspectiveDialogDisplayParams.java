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

package org.gorpipe.querydialogs.factory;

import org.gorpipe.util.Pair;

import java.awt.*;
import java.util.HashMap;
import java.util.Set;

/**
 * Class for defining a perspective dialog display parameters.
 *
 * @version $Id$
 */
public class PerspectiveDialogDisplayParams {

    private static final HashMap<String, Color> colorMap = new HashMap<>();
    private static final HashMap<String, Color> fadeInColorMap = new HashMap<>();

    static {
        colorMap.put("RED", new Color(128, 0, 0));
        colorMap.put("GREEN", new Color(0, 128, 0));
        colorMap.put("BLUE", Color.BLUE);
        colorMap.put("MAGENTA", Color.MAGENTA);
        colorMap.put("DARK_GRAY", Color.DARK_GRAY);
        colorMap.put("BLACK", Color.BLACK);
        colorMap.put("CYAN", new Color(0, 172, 170));
        colorMap.put("ORANGE", new Color(255, 153, 0));
        colorMap.put("OLIVE", new Color(128, 128, 0));
    }

    static {
        fadeInColorMap.put("RED", new Color(255, 153, 153));
        fadeInColorMap.put("GREEN", new Color(153, 255, 153));
        fadeInColorMap.put("BLUE", new Color(153, 153, 255));
        fadeInColorMap.put("MAGENTA", new Color(255, 153, 255));
        fadeInColorMap.put("DARK_GRAY", new Color(204, 204, 204));
        fadeInColorMap.put("BLACK", new Color(140, 140, 140));
        fadeInColorMap.put("CYAN", new Color(153, 255, 255));
        fadeInColorMap.put("ORANGE", new Color(255, 203, 121));
        fadeInColorMap.put("OLIVE", new Color(243, 246, 0));
    }

    /**
     * Category name.
     */
    public final String category;
    /**
     * The image color for dialog display.
     */
    public final String imageColorName;

    /**
     * Constructor.
     *
     * @param category       category name
     * @param imageColorName image color name for dialog display
     */
    public PerspectiveDialogDisplayParams(final String category, final String imageColorName) {
        this.category = category;
        this.imageColorName = imageColorName.toUpperCase();
    }

    /**
     * Get random color from available colors in map.
     *
     * @return color
     */
    public static Pair<Color, Color> getRandomColor() {
        final int colorIdx = (int) Math.round(Math.random() * (colorMap.size() - 1));
        final String colorName = getAvailableColors()[colorIdx];
        return new Pair<>(colorMap.get(colorName.toUpperCase()), fadeInColorMap.get(colorName.toUpperCase()));
    }

    private static String[] getAvailableColors() {
        final Set<String> availableColors = colorMap.keySet();
        return availableColors.toArray(new String[availableColors.size()]);
    }

    /**
     * Get the image color.
     *
     * @return the image color
     */
    public Color getImageColor() {
        return colorMap.get(imageColorName);
    }

    /**
     * Get the fade in image color.
     *
     * @return the fade in image color
     */
    public Color getFadeInImageColor() {
        return fadeInColorMap.get(imageColorName);
    }
}
