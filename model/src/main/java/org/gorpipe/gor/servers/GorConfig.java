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

package org.gorpipe.gor.servers;

import org.aeonbits.owner.Config;
import org.gorpipe.base.config.annotations.ConfigComponent;
import org.gorpipe.base.config.annotations.Documentation;

/**
 * Configuration options for gor
 */
@ConfigComponent("gor")
public interface GorConfig extends Config {
    String GOOGLE_APPLICATION_CREDENTIALS = "gor.google.application.credentials";
    @Documentation("Google application credentials in json format")
    @Key(GOOGLE_APPLICATION_CREDENTIALS)
    @DefaultValue("")
    String googleApplicationCredentials();
}
