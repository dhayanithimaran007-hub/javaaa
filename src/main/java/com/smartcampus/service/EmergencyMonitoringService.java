package com.smartcampus.service;

import com.smartcampus.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class EmergencyMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(EmergencyMonitoringService.class);

    private final EmergencyEscalationService escalationService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private Consumer<EmergencyEscalationService.EscalationCycleResult> onCycleCompleteEdt;

    public EmergencyMonitoringService() {
        this(new EmergencyEscalationService());
    }

    public EmergencyMonitoringService(EmergencyEscalationService escalationService) {
        this.escalationService = escalationService;
    }

    public void setCycleListener(Consumer<EmergencyEscalationService.EscalationCycleResult> listenerOnEdt) {
        this.onCycleCompleteEdt = listenerOnEdt;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        int intervalSec = Math.max(5, AppConfig.getIntProperty("monitoring.interval.seconds", 30));

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Emergency-Monitor-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(this::runCycleSafely, 0, intervalSec, TimeUnit.SECONDS);
        logger.info("EmergencyMonitoringService started (interval={}s).", intervalSec);
    }

    public void triggerImmediateScanAsync() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.execute(this::runCycleSafely);
        } else {
            Thread t = new Thread(this::runCycleSafely, "Emergency-Manual-Scan");
            t.setDaemon(true);
            t.start();
        }
    }

    private void runCycleSafely() {
        try {
            EmergencyEscalationService.EscalationCycleResult result = escalationService.evaluateAndEscalateActiveComplaints();
            Consumer<EmergencyEscalationService.EscalationCycleResult> listener = this.onCycleCompleteEdt;
            if (listener != null) {
                SwingUtilities.invokeLater(() -> listener.accept(result));
            }
        } catch (Exception e) {
            logger.warn("Emergency monitoring cycle error: {}", e.getMessage());
        }
    }

    public synchronized void stop() {
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            logger.info("EmergencyMonitoringService stopped.");
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
