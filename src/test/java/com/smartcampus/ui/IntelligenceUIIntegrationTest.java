package com.smartcampus.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartcampus.model.User;
import com.smartcampus.model.enums.RecommendationStatus;
import com.smartcampus.model.enums.Role;
import com.smartcampus.security.UserSession;
import com.smartcampus.service.EmergencyEscalationService;
import com.smartcampus.service.PredictiveMaintenanceService;
import com.smartcampus.ui.admin.EmergencyOperationsPanel;
import com.smartcampus.ui.admin.PredictiveMaintenancePanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Smart Campus 2.0 RBAC & UI Modules Integration Tests")
class IntelligenceUIIntegrationTest {

    @AfterEach
    void tearDown() {
        UserSession.logout();
    }

    @Test
    @DisplayName("RBAC: Student role must be blocked from Emergency Admin & Predictive Maintenance controls")
    void testRbacEnforcementForStudent() {
        User student = new User();
        student.setUserId(101);
        student.setEmail("alice@student.campus.edu");
        student.setFullName("Alice Sharma");
        student.setRole(Role.STUDENT);
        student.setActive(true);
        UserSession.login(student);

        EmergencyEscalationService escSvc = new EmergencyEscalationService();
        PredictiveMaintenanceService predSvc = new PredictiveMaintenanceService();

        assertThrows(SecurityException.class, escSvc::getEmergencyCommandData);
        assertThrows(SecurityException.class, () -> escSvc.acknowledgeAlert(1, "Unauthorized"));
        assertThrows(SecurityException.class, () -> escSvc.addInterventionNote(1, "Unauthorized"));

        assertThrows(SecurityException.class, () -> predSvc.runPredictiveAnalysis(30));
        assertThrows(SecurityException.class, () -> predSvc.getPredictiveDashboardData(null, null, null, null, 6));
        assertThrows(SecurityException.class, () -> predSvc.updateRecommendationLifecycle(1, RecommendationStatus.APPROVED, null));
    }

    @Test
    @DisplayName("UI Integration: EmergencyOperationsPanel and PredictiveMaintenancePanel instantiate cleanly on EDT")
    void testPanelsInstantiateCleanlyOnEdt() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires non-headless environment");

        SwingUtilities.invokeAndWait(() -> {
            FlatLightLaf.setup();
            EmergencyOperationsPanel emergencyPanel = new EmergencyOperationsPanel();
            PredictiveMaintenancePanel predictivePanel = new PredictiveMaintenancePanel();
            assertNotNull(emergencyPanel);
            assertNotNull(predictivePanel);
        });
    }
}
