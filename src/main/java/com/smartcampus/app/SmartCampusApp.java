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

        // 1. Initialize FlatLaf Look and Feel for modern Swing desktop appearance
        try {
            FlatLightLaf.setup();
            // Optional: apply clean Segoe UI / System font
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 12));
            logger.info("FlatLaf Look and Feel initialized.");
        } catch (Exception e) {
            logger.warn("Could not apply FlatLaf theme, defaulting to system: {}", e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // 2. Asynchronously verify / initialize database schema and demo accounts in background
        new Thread(() -> {
            try {
                DatabaseInitializer.initializeDatabase();
            } catch (Exception e) {
                logger.warn("Database initialization warning: {}", e.getMessage());
            }
        }, "DB-Init-Thread").start();

        // 3. Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            logger.info("Application UI ready on Event Dispatch Thread.");
        });
    }
}
