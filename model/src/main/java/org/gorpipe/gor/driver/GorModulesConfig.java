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

import java.util.List;
import com.google.inject.Module;
import org.gorpipe.gor.driver.config.ModuleConverter;
import org.gorpipe.base.config.annotations.ConfigComponent;
import org.gorpipe.base.config.annotations.Documentation;
import org.aeonbits.owner.Mutable;

/**
 * Created by stefan on 8.9.2016.
 */
@ConfigComponent("gor.modules")
public interface GorModulesConfig extends Mutable {
    String CORE_MODULE_KEY = "gor.modules.core";
    String EXTRA_MODULES_KEY = "gor.modules.extras";
    String OVERRIDE_MODULES_KEY = "gor.modules.overrides";

    String[] KEYS = {CORE_MODULE_KEY, EXTRA_MODULES_KEY, OVERRIDE_MODULES_KEY};

    @Documentation("The name of the core module class to use to provide the GorDriver mechanism")
    @Key(CORE_MODULE_KEY)
    @DefaultValue("org.gorpipe.gor.driver.GorDriverModule")
    @ConverterClass(ModuleConverter.class)
    Module driverModule();

    @Documentation("A comma separated list of module class names to provide other functionality than what the core provides")
    @Key(EXTRA_MODULES_KEY)
    @DefaultValue("")
    @ConverterClass(ModuleConverter.class)
    List<Module> extraModules();

    @Documentation("A comma separated list of module class names that should override what's provided by other modules, including the core and extras modules defined above")
    @Key(OVERRIDE_MODULES_KEY)
    @DefaultValue("")
    @ConverterClass(ModuleConverter.class)
    List<Module> overrideModules();
}
