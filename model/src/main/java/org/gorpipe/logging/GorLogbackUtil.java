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

package org.gorpipe.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ch.qos.logback.classic.LoggerContext;
import org.gorpipe.base.logging.ProcessIdConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


import static net.logstash.logback.argument.StructuredArguments.value;


public class GorLogbackUtil {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(GorLogbackUtil.class);

    @SuppressWarnings("unused")
    public static final Marker MARKER_AUDIT = MarkerFactory.getDetachedMarker("audit");
    @SuppressWarnings("unused")
    public static final Marker MARKER_SECURITY = MarkerFactory.getDetachedMarker("security");
    public static final Marker MARKER_SYSTEM = MarkerFactory.getDetachedMarker("system");

    private static final String UNKNOWN_PROPERTY = "";

    private GorLogbackUtil() {
        throw new IllegalStateException("Utility class");
    }

    @SuppressWarnings("unused") // Used from gor-services
    public static void logServiceStatus(org.slf4j.Logger logger) {
        try {
            logger.info(GorLogbackUtil.MARKER_SYSTEM, "System Information", GorLogbackUtil.getSystemProperties());
        } catch (Exception e){
            logger.warn("Error when logging out system information");
        }
    }

    private static Object[] getSystemProperties() {
        return new Object[]{
                value("os.arch", System.getProperty("os.arch", UNKNOWN_PROPERTY)),
                value("os.name", System.getProperty("os.name", UNKNOWN_PROPERTY)),
                value("os.version", System.getProperty("os.version", UNKNOWN_PROPERTY)),
                value("gor.version", getGORVersion()),
                value("java.vendor", System.getProperty("java.vendor", UNKNOWN_PROPERTY)),
                value("java.version", System.getProperty("java.version", UNKNOWN_PROPERTY))
        };
    }

    private static String getEnv(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && envValue.length() > 0) {
            return envValue;
        }
        return defaultValue;
    }


    public static void initServiceLog() {
        initLog(UNKNOWN_PROPERTY);
    }

    public static void initLog(String appName) {
        try {
            String serviceName = System.getProperty("service.name", UNKNOWN_PROPERTY);
            if (appName.equals(UNKNOWN_PROPERTY)) {
                appName = serviceName;
            }
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.putProperty("processid", ProcessIdConverter.getProcessId());
            context.putProperty("gor_service", serviceName);
            context.putProperty("gor_app", appName);
            context.putProperty("gor_version", getGORVersion());
            context.putProperty("environment", getEnv("ENVIRONMENT_NAME", "unkown_environment"));
            context.putProperty("csa_env", getEnv("CSA_ENV", "unkown"));

            MDC.put("processid", ProcessIdConverter.getProcessId());
            MDC.put( "gor_service", serviceName);
        } catch (Exception e) {
            logger.warn("Failed to initialize service status");
        }
    }

    @SuppressWarnings("unused") // Used from gor-services
    public static void terminateServiceLog() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }

    private static String getGORVersion() {
        String version = GorLogbackUtil.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = UNKNOWN_PROPERTY;
        }

        return version;
    }

    @SuppressWarnings("unused") // Used from gor-services
    public static Object[] getValuesFromMap(Map<String, String> map) {
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            values.add(value(entry.getKey(), entry.getValue()));
        }

        return values.toArray();
    }
}
