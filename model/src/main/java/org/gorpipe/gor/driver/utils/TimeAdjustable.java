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

package org.gorpipe.gor.driver.utils;

/**
 * Classes that implement this interface promise to use the static {@link TimeUtils} instance for all queries and
 * calculations that involve current time. This results in the ability to "cheat" time by using the methods in
 * {@link TimeUtils} (or the default methods provided by this interface which delegate to the aforementioned
 * {@link TimeUtils} methods) to add/remove or reset a time delta.
 */
public interface TimeAdjustable {

    /**
     * Adds the specified delta value to the existing one and returns the new delta.
     *
     * @param delta the delta value to add
     * @return the current delta after adding the supplied one.
     */
    default long addAndGetTimeDelta(long delta) {
        return TimeUtils.instance().addAndGetTimeDelta(delta);
    }

    /**
     * Sets the delta to 0. After calling this method, {@link #getTimeMs()} will return the same value as
     * {@link System#currentTimeMillis()}.
     */
    default void resetTimeDelta() {
        TimeUtils.instance().resetTimeDelta();
    }

    /**
     * Returns <code>true</code> if a time delta has been set, <code>false</code> otherwise.
     *
     * @return Whether or not a time delta has been set.
     */
    default boolean hasTimeDelta() {
        return TimeUtils.instance().hasTimeDelta();
    }

    /**
     * Gets the time, including any delta that has been set.
     *
     * @return The current time in milliseconds since epoc plus/minus any delta that has been set.
     */
    default long getTimeMs() {
        return TimeUtils.instance().getTimeMs();
    }
}
