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

package org.gorpipe.s3.shared;

import org.gorpipe.base.config.annotations.Documentation;
import org.gorpipe.s3.driver.S3Configuration;

public interface S3SharedConfiguration extends S3Configuration {

    @Documentation("Should S3 shared resources use fallback")
    @Key("GOR_S3_SHARED_USE_FALLBACK")
    @DefaultValue("true")
    boolean useFallback();

    @Documentation("Should S3 shared resources only be accessible using links on server")
    @Key("GOR_S3_SHARED_ONLY_ACCESS_WITH_LINKS_ON_SERVER")
    @DefaultValue("false")
    boolean onlyAccessWithLinksOnServer();

    @Documentation("Should S3 shared resources always use highest level (s3datat://shared) i links")
    @Key("GOR_S3_SHARED_USE_HIGHEST_TYPE_IN_LINKS")
    @DefaultValue("true")
    boolean useHighestTypeInLinks();
}
