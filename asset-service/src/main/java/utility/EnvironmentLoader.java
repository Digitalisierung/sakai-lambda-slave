package utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvironmentLoader {
    public static String getEnv(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be null or blank");
        }

        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            System.err.println("Environment variable " + key + " is not set");
            return System.getProperty(key);
        }
        return value;
    }

    public static void loadEnv() throws IOException {
        if (System.getenv("USER") == null) {
            Properties properties = System.getProperties();
            InputStream resourceAsStream = EnvironmentLoader.class.getClassLoader().getResourceAsStream(".env");
            properties.load(resourceAsStream);
            System.setProperties(properties); 
        }
    }
}
