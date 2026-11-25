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

        properties.setProperty("db.url",
                "jdbc:mysql://localhost:3306/smart_campus_db?serverTimezone=Asia/Kolkata&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
        properties.setProperty("db.user", "root");
        properties.setProperty("db.password", "");
        properties.setProperty("app.name", "Smart Campus Complaint Prioritization and Tracking System");
        properties.setProperty("app.version", "2.0.0");
        properties.setProperty("app.theme", "light");

        properties.setProperty("sla.critical.hours", "4");
        properties.setProperty("sla.high.hours", "24");
        properties.setProperty("sla.medium.hours", "72");
        properties.setProperty("sla.low.hours", "168");
        properties.setProperty("sla.warning.early.pct", "50");
        properties.setProperty("sla.warning.approaching.pct", "75");
        properties.setProperty("sla.breach.pct", "100");
        properties.setProperty("sla.escalation.admin.threshold.pct", "125");
        properties.setProperty("monitoring.interval.seconds", "30");

        properties.setProperty("predictive.weight.frequency", "0.25");
        properties.setProperty("predictive.weight.recurrence", "0.25");
        properties.setProperty("predictive.weight.recency", "0.20");
        properties.setProperty("predictive.weight.severity", "0.15");
        properties.setProperty("predictive.weight.trend", "0.15");
        properties.setProperty("predictive.rule.high.building.freq", "4");
        properties.setProperty("predictive.rule.repeated.category.count", "3");
        properties.setProperty("predictive.rule.repeated.location.count", "2");
        properties.setProperty("predictive.rule.high.priority.count", "2");
        properties.setProperty("predictive.rule.trend.growth.ratio", "1.25");
        properties.setProperty("predictive.rule.repeated.asset.count", "2");

        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists() && configFile.isFile()) {
            try (InputStream in = new FileInputStream(configFile)) {
                Properties fileProps = new Properties();
                fileProps.load(in);
                for (String name : fileProps.stringPropertyNames()) {
                    String val = fileProps.getProperty(name);

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

    public static int getIntProperty(String key, int defaultValue) {
        String val = getProperty(key, null);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double getDoubleProperty(String key, double defaultValue) {
        String val = getProperty(key, null);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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
