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

package org.gorpipe.gor.driver.providers.stream.sources.mdr;

import org.aeonbits.owner.Config;
import org.gorpipe.base.config.annotations.Documentation;

public interface MdrConfiguration extends Config {

    @Documentation("URL to the MDR service")
    @Key("GOR_MDR_SERVER")
    @DefaultValue("https://platform.wuxinextcodedev.com/mdr")
    String mdrServer();

    @Documentation("MDR service timout in seconds")
    @Key("GOR_MDR_TIMEOUT")
    @DefaultValue("60")
    int mdrTimeout();

    @Documentation("MDR default link type, direct or presigned")
    @Key("GOR_MDR_LINK_TYPE")
    @DefaultValue("direct")
    String mdrDefaultLinkType();

    @Documentation("MDR include grouped documents by default")
    @Key("GOR_MDR_GROUPED")
    @DefaultValue("false")
    boolean mdrIncludeGrouped();

    @Documentation("MDR cache duration in minutes")
    @Key("GOR_MDR_CACHE_DURATION")
    @DefaultValue("60")
    int mdrCacheDuration();

    @Documentation("Keycloak auth server url")
    @Key("GOR_KEYCLOAK_SERVER")
    @DefaultValue("https://platform.wuxinextcodedev.com/auth/realms/wuxinextcode.com/protocol/openid-connect/token")
    String keycloakAuthServer();

    @Documentation("Keycloak auth server timout in seconds")
    @Key("GOR_KEYCLOAK_TIMEOUT")
    @DefaultValue("60")
    int keycloakAuthTimeout();

    @Documentation("Keycloak client id")
    @Key("GOR_KEYCLOAK_CLIENT_ID")
    @DefaultValue("gor")
    String keycloakClientId();

    @Documentation("Keycloak service password")
    @Key("GOR_KEYCLOAK_CLIENT_SECRET")
    String keycloakClientSecret();
}
