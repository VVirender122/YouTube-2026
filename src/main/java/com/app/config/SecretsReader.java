package com.app.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Central secret/config loader for mail credentials, database URIs and API keys.
 *
 * Priority:
 * 1. Environment variable with the requested key.
 * 2. External properties file from APP_SECRETS_FILE / -Dapp.secrets.file.
 * 3. Classpath secrets.properties (local development only; git-ignored).
 *
 * Never put real credentials, API keys or passwords in source code.
 */
public final class SecretsReader {

    private SecretsReader() {
    }

    public static String readData(String filename, Locale locale, String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        String configuredFile = System.getProperty("app.secrets.file");
        if (configuredFile == null || configuredFile.isBlank()) {
            configuredFile = System.getenv("APP_SECRETS_FILE");
        }

        if (configuredFile != null && !configuredFile.isBlank()) {
            Properties external = loadExternal(Path.of(configuredFile));
            String value = external.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        String bundleName = (filename == null || filename.isBlank()) ? "secrets" : filename;
        try {
            Properties classpath = new Properties();
            try (InputStream in = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(bundleName + ".properties")) {
                if (in != null) {
                    classpath.load(in);
                    String value = classpath.getProperty(key);
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read local secret configuration.", e);
        }

        throw new IllegalStateException(
                "Missing configuration '" + key + "'. Set the environment variable, "
                + "provide APP_SECRETS_FILE, or create src/main/resources/secrets.properties "
                + "from secrets.properties.example.");
    }

    private static Properties loadExternal(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Secret configuration file does not exist: " + path);
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read secret configuration: " + path, e);
        }
    }
}
