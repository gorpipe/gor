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
import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.base.config.annotations.Documentation;
import org.gorpipe.base.config.converters.DurationConverter;
import org.gorpipe.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MdrConfiguration extends Config {

    Logger log = LoggerFactory.getLogger(MdrConfiguration.class);

    /**
     * Parse MDR credentials from a string.
     *
     * The credentials are in the format:
     *   #name\tMdrUrl\tKeycloakUrl\tKeycloakClientId\tKeycloakClientSecret
     *   <name>\t<mdr url>\t<keycloakUrl>\t<clientId>\t<clientSecret>
     *
     *   # Lines starting with '#' are treated as comments and ignored.
     *
     * @param credentialsData The credential data
     * @return An MdrConfiguration list containing the parsed credentials.
     * @throws IllegalArgumentException if the credential string is not in the expected format.
     */
    static List<MdrConfiguration> parseConfigurationData(String credentialsData) {
        List<MdrConfiguration> mdrConfList = new java.util.ArrayList<>();
        for (String credLine : credentialsData.split("\n")) {
            credLine = credLine.trim();
            if (credLine.isEmpty() || credLine.startsWith("#")) {
                continue;
            }

            String[] parts = credLine.split("\t");
            if (parts.length != 5) {
                log.error("Invalid credential line format. Expected format: <mdr url>\\t<keycloakUrl></>\\t<clientId>\\t<clientSecret>");
                continue;
            }

            mdrConfList.add(ConfigManager.createConfig(MdrConfiguration.class, Map.of(
                    "GOR_MDR_SERVER_NAME", parts[0],
                "GOR_MDR_SERVER", parts[1],
                "GOR_KEYCLOAK_SERVER", parts[2],
                "GOR_KEYCLOAK_CLIENT_ID", parts[3],
                "GOR_KEYCLOAK_CLIENT_SECRET", parts[4]
            )));
        }

        return mdrConfList;
    }

    static HashMap<String, MdrConfiguration> loadMdrConfigurations(MdrConfiguration defaultConfig) {
        HashMap<String, MdrConfiguration> mdrConfigurationsMap = new HashMap<>();
        mdrConfigurationsMap.put(defaultConfig.mdrServerName(), defaultConfig);

        final String MDR_CREDENTIALS_PATH = System.getProperty("gor.mdr.credentials", "/mnt/csa/config/gor-mdr-config.tsv");

        if (!Strings.isNullOrEmpty(MDR_CREDENTIALS_PATH)) {
            try {
                String credentialsData = Files.readString(Path.of(MDR_CREDENTIALS_PATH));
                for (MdrConfiguration config : parseConfigurationData(credentialsData)) {
                    mdrConfigurationsMap.put(config.mdrServerName(), config);
                }
            } catch (Exception e) {
                log.error("Failed to read MDR credentials from path: " + MDR_CREDENTIALS_PATH, e);
            }
        }
        return mdrConfigurationsMap;
    }

    @Documentation("Name/alias of the MDR server")
    @Key("GOR_MDR_SERVER_NAME")
    @DefaultValue("default")
    String mdrServerName();

    @Documentation("URL to the MDR service")
    @Key("GOR_MDR_SERVER")
    @DefaultValue("https://mdr-service.dev.data.oci.genedx.net")
    String mdrServer();

    @Documentation("MDR service timeout in seconds")
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
    @DefaultValue("https://auth.dev.engops.genedx.net/realms/genedx-dev/protocol/openid-connect/token")
    String keycloakAuthServer();

    @Documentation("Keycloak auth server timout in seconds")
    @Key("GOR_KEYCLOAK_TIMEOUT")
    @DefaultValue("60")
    int keycloakAuthTimeout();

    @Documentation("Keycloak client id")
    @Key("GOR_KEYCLOAK_CLIENT_ID")
    @DefaultValue("dp-gor")
    String keycloakClientId();

    @Documentation("Keycloak service password")
    @Key("GOR_KEYCLOAK_CLIENT_SECRET")
    String keycloakClientSecret();

    @Documentation("The time to wait before the first mdr retry.")
    @Key("org.gorpipe.gor.driver.providers.retries.mdr.initial_wait")
    @DefaultValue("100 milliseconds")
    @ConverterClass(DurationConverter.class)
    Duration retryInitialWait();

    @Documentation("The maximum time to wait for mdr retrying.")
    @Key("org.gorpipe.gor.driver.retries.mdr.max_wait")
    @DefaultValue("60 seconds")
    @ConverterClass(DurationConverter.class)
    Duration retryMaxWait();

}
