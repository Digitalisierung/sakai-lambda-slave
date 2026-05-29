package utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Hilfsklasse zum Laden von Umgebungsvariablen.
 * Unterstützt sowohl echte System-Umgebungsvariablen (AWS Lambda)
 * als auch lokale .env-Dateien für die Entwicklungsumgebung.
 */
public class EnvironmentLoader {

    /**
     * Gibt den Wert einer Umgebungsvariable zurück.
     * Zuerst wird die System-Umgebungsvariable geprüft. Falls diese nicht gesetzt ist,
     * wird auf eine System-Property als Fallback zurückgegriffen (z.B. für lokale Tests).
     *
     * @param key der Name der Umgebungsvariable
     * @return der Wert der Umgebungsvariable oder der System-Property
     * @throws IllegalArgumentException wenn key null oder leer ist
     */
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

    /**
     * Lädt Umgebungsvariablen aus einer lokalen .env-Datei in die System-Properties.
     * Diese Methode wird nur ausgeführt, wenn die Umgebungsvariable "USER" nicht gesetzt ist,
     * d.h. ausschließlich in der lokalen Entwicklungsumgebung außerhalb von Lambda.
     *
     * @throws IOException wenn die .env-Datei nicht gefunden oder nicht gelesen werden kann
     */
    public static void loadEnv() throws IOException {
        if (System.getenv("USER") == null) {
            Properties properties = System.getProperties();
            InputStream resourceAsStream = EnvironmentLoader.class.getClassLoader().getResourceAsStream(".env");
            properties.load(resourceAsStream);
            System.setProperties(properties);
        }
    }
}
