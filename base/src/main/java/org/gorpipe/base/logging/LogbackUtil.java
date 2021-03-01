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

package org.gorpipe.base.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LogbackUtil {
    public static final Marker MARKER_AUDIT = MarkerFactory.getDetachedMarker("audit");

    private LogbackUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Configure logging
     *
     * @param logFolder The default log folder if logback configuration file is not found and can not be created.
     * @param logName   The default log file name.
     */
    public static void configure(Path logFolder, String logName) {
        if (!LogbackUtil.foundLogConfigFile() && !LogbackUtil.createDefaultLogConfigFile()) {
            LogbackUtil.applyBasicLogConfig(logFolder, logName);
        }
    }

    /**
     * Creates a default logback configuration file and uses it to configure the logger in use.
     * The file name is specified by system property logback.configurationFile if it is set. If it is not set the
     * default file name is logback.xml.
     *
     * @return true if file creation was successful, else false
     */
    public static boolean createDefaultLogConfigFile() {
        String logConfigFileStr = System.getProperty("logback.configurationFile");
        File logConfigFile = new File(logConfigFileStr != null ? logConfigFileStr : "logback.xml");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            // Copy a default logback configuration file from resources
            FileUtils.copyInputStreamToFile(LogbackUtil.class.getClassLoader().getResourceAsStream(
                    "com/nextcode/logging/logback.default.xml"), logConfigFile);

            loggerContext.reset();
            ContextInitializer ci = new ContextInitializer(loggerContext);
            URL url = logConfigFile.toURI().toURL();

            ci.configureByResource(url);
            return true;
        } catch (IOException | JoranException | NullPointerException e) {
            //unable to create the logback configuration file
            return false;
        }
    }

    /**
     * Checks whether a logback.xml configuration file was found for the current logger. It does that
     * by running through a list of status messages that are generated when the logger is started.
     *
     * @return true if the config file was found. Otherwise false.
     */
    public static boolean foundLogConfigFile() {
        LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        List<Status> statusList = logContext.getStatusManager().getCopyOfStatusList();

        for (Status stat : statusList) {
            if (stat.getMessage().contains("Could NOT find resource [logback.xml]")) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method will create a basic configuration used by the logback util. It will not store the config file locally.
     *
     * @param logFolder where to store the logfile
     * @param logName   name of the log file
     */
    public static void applyBasicLogConfig(Path logFolder, String logName) {
        //unable to create the logback configuration file so lets create a basic log configuration
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        String today = new SimpleDateFormat("yyyyMMdd").format(
                new Date());
        String logFile = Paths.get(logFolder.toString(), logName + "_" + today + ".log").toFile().getAbsolutePath();

        PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
        layoutEncoder.setContext(loggerContext);
        layoutEncoder.setPattern("%date %level \\(%file:%line\\) - %msg%ex%n");
        layoutEncoder.start();

        FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setPrudent(true);
        fileAppender.setEncoder(layoutEncoder);
        fileAppender.setName("file");
        fileAppender.setContext(loggerContext);
        fileAppender.setFile(logFile);
        fileAppender.start();

        root.addAppender(fileAppender);
        root.setLevel(Level.DEBUG);
        root.info("Logging to file {}", logFile);
    }
}
