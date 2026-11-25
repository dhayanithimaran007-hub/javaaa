package com.smartcampus.app;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartcampus.config.AppConfig;
import com.smartcampus.config.DatabaseInitializer;
import com.smartcampus.ui.LoginFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

public class SmartCampusApp {
    private static final Logger logger = LoggerFactory.getLogger(SmartCampusApp.class);

    public static void main(String[] args) {
        logger.info("Starting Smart Campus Complaint Prioritization and Tracking System v{}...", AppConfig.getAppVersion());

        try {
            FlatLightLaf.setup();

            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 12));
            logger.info("FlatLaf Look and Feel initialized.");
        } catch (Exception e) {
            logger.warn("Could not apply FlatLaf theme, defaulting to system: {}", e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        new Thread(() -> {
            try {
                DatabaseInitializer.initializeDatabase();
            } catch (Exception e) {
                logger.warn("Database initialization warning: {}", e.getMessage());
            }
        }, "DB-Init-Thread").start();

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            logger.info("Application UI ready on Event Dispatch Thread.");
        });
    }
}
