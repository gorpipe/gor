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

package org.gorpipe.gor.driver;

import com.google.inject.Module;

/**
 * Gor driver plugin interface.
 * <p>
 * 1. Implement this interface (e.g. by extending Guice AbstractModule) and add reference to META-INF/services/org.gorpipe.gor.driver.Plugin
 * 2. E.g. in configure() method - add source providers by using static bindSourceProvider method in GorDriverModule
 * 3. News types of stream source providers can extend StreamSourceProvider class to automatically get handling for all data types.
 * 3. Add data types handling to all stream sources by using static bindStreamSourceIteratorFactory helper method in StreamSourcePlugin
 */
public interface Plugin extends Module {

}
