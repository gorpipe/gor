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

package org.gorpipe.s3.driver;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gorpipe.gor.driver.GorDriverModule;
import org.gorpipe.gor.driver.Plugin;
import org.gorpipe.base.config.ConfigManager;

public class S3DriverPlugin extends AbstractModule implements Plugin {

    @Override
    protected void configure() {
        GorDriverModule.bindSourceProvider(binder(), S3SourceProvider.class);
    }

    @Provides
    @Singleton
    public S3Configuration config() {
        return ConfigManager.createPrefixConfig("gor.s3", S3Configuration.class);
    }

}
