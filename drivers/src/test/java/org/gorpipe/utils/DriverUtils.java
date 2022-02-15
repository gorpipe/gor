package org.gorpipe.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class DriverUtils {

    private static final Logger log = LoggerFactory.getLogger(DriverUtils.class);

    public static String SECRETS_FILE_NAME = "../tests/config/secrets.env";

    public static Properties getDriverProperties() {
        Properties prop = new Properties();
        if (Files.exists(Paths.get(SECRETS_FILE_NAME))) {
            log.debug("Loading env: " + SECRETS_FILE_NAME);
            try (InputStream inputStream = new FileInputStream(SECRETS_FILE_NAME)) {
                prop.load(inputStream);
            } catch (IOException e) {
                // Do nothing
            }
        }

        prop.putAll(System.getenv());

        return prop;
    }
}
