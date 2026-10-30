package com.smartcampus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE_PATH = "config/application.properties";

    private static final Properties properties = new Properties();
    private static boolean initialized = false;

    static {
        loadConfig();
    }

    public static synchronized void ensureInitialized() {
        if (!initialized) {
            loadConfig();
        }
    }

    public static synchronized void loadConfig() {
        properties.clear();

        // 1. Load defaults (no sensitive passwords in Git-tracked source code)
        properties.setProperty("db.url",
                "jdbc:mysql://localhost:3306/smart_campus_db?serverTimezone=Asia/Kolkata&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
        properties.setProperty("db.user", "root");
        properties.setProperty("db.password", "");
        properties.setProperty("app.name", "Smart Campus Complaint Prioritization and Tracking System");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("app.theme", "light");

        // 2. Load from local file if exists
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists() && configFile.isFile()) {
            try (InputStream in = new FileInputStream(configFile)) {
                Properties fileProps = new Properties();
                fileProps.load(in);
                for (String name : fileProps.stringPropertyNames()) {
                    String val = fileProps.getProperty(name);
                    // Do not allow empty/blank properties to overwrite non-empty defaults
                    if (val != null) {
                        String trimmed = val.trim();
                        if (!trimmed.isEmpty()) {
                            properties.setProperty(name, trimmed);
                        }
                    }
                }
                logger.info("Loaded configuration from {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Could not read configuration file: {}", e.getMessage());
            }
        } else {
            // Try classpath
            try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) {
                    Properties cpProps = new Properties();
                    cpProps.load(in);
                    for (String name : cpProps.stringPropertyNames()) {
                        String val = cpProps.getProperty(name);
                        if (val != null) {
                            String trimmed = val.trim();
                            if (!trimmed.isEmpty()) {
                                properties.setProperty(name, trimmed);
                            }
                        }
                    }
                    logger.info("Loaded configuration from classpath");
                }
            } catch (IOException e) {
                logger.warn("Could not read classpath config: {}", e.getMessage());
            }
        }

        // 3. Environment variables override (highest precedence)
        String envDbUrl = System.getenv("DB_URL");
        if (envDbUrl != null && !envDbUrl.isBlank()) {
            properties.setProperty("db.url", envDbUrl.trim());
        }
        String envDbUser = System.getenv("DB_USER");
        if (envDbUser != null && !envDbUser.isBlank()) {
            properties.setProperty("db.user", envDbUser.trim());
        }
        String envDbPass = System.getenv("DB_PASSWORD");
        if (envDbPass != null && !envDbPass.isBlank()) {
            properties.setProperty("db.password", envDbPass);
        }

        initialized = true;
    }

    public static String getDbUrl() {
        ensureInitialized();
        return properties.getProperty("db.url");
    }

    public static String getDbUser() {
        ensureInitialized();
        return properties.getProperty("db.user");
    }

    public static String getDbPassword() {
        ensureInitialized();
        return properties.getProperty("db.password");
    }

    public static boolean isDbPasswordConfigured() {
        ensureInitialized();
        String password = properties.getProperty("db.password");
        return password != null && !password.isBlank() && !"your_mysql_password_here".equals(password.trim());
    }

    public static String getAppName() {
        ensureInitialized();
        return properties.getProperty("app.name", "Smart Campus Complaint System");
    }

    public static String getAppVersion() {
        ensureInitialized();
        return properties.getProperty("app.version", "1.0.0");
    }

    public static String getProperty(String key, String defaultValue) {
        ensureInitialized();
        return properties.getProperty(key, defaultValue);
    }

    public static synchronized void setProperty(String key, String value) {
        ensureInitialized();
        if (value != null) {
            properties.setProperty(key, value);
        } else {
            properties.remove(key);
        }
    }

    public static synchronized void updateDbCredentials(String url, String user, String password, boolean persist) {
        ensureInitialized();
        if (url != null && !url.isBlank())
            properties.setProperty("db.url", url.trim());
        if (user != null && !user.isBlank())
            properties.setProperty("db.user", user.trim());
        if (password != null)
            properties.setProperty("db.password", password);

        if (persist) {
            File configFile = new File(CONFIG_FILE_PATH);
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                properties.store(out, "Updated via Smart Campus Application Configuration");
                logger.info("Persisted updated configuration to {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to persist updated configuration: {}", e.getMessage());
            }
        }
    }
}
