package com.sakai.inventory.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvironmentLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentLoader.class);

    private EnvironmentLoader() {
        super();
    }

    public static String getEnv(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key must not be null or blank.");
        }

        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            LOGGER.warn("Environment variable {} is not set. Falling back to system property.", key);
            return System.getProperty(key);
        }
        return value;
    }

    public static void loadEnv() throws IOException {
        if (System.getenv("USER") == null) {
            Properties properties = System.getProperties();
            InputStream resourceAsStream = EnvironmentLoader.class.getClassLoader().getResourceAsStream(".env");
            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
                System.setProperties(properties);
            }
        } else {
            LOGGER.debug("Skipping .env loading because USER environment variable is already defined.");
        }
    }
}
