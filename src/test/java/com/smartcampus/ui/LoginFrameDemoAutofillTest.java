package com.smartcampus.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.smartcampus.config.DBConnection;
import com.smartcampus.model.User;
import com.smartcampus.service.UserService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginFrameDemoAutofillTest {

    @Test
    @DisplayName("All 8 demo accounts load cleanly, autofill fields, avoid duplicates, and stay visible")
    void testDemoAutofillVisibilityAndPopulation() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires non-headless environment");

        SwingUtilities.invokeAndWait(() -> {
            FlatLightLaf.setup();
            LoginFrame frame = new LoginFrame();
            try {

                Dimension[] sizes = {
                        new Dimension(980, 660),
                        new Dimension(860, 580),
                        new Dimension(1280, 800)
                };

                @SuppressWarnings("unchecked")
                JComboBox<String> cmbQuickFill = getField(frame, "cmbQuickFill", JComboBox.class);
                JTextField txtEmail = getField(frame, "txtEmail", JTextField.class);
                JPasswordField txtPassword = getField(frame, "txtPassword", JPasswordField.class);

                assertNotNull(cmbQuickFill);

                String[][] expected = {
                        {"Administrator", "admin@campus.edu", "Admin@123"},
                        {"Electrician", "electrician@campus.edu", "Staff@123"},
                        {"Plumber", "plumber@campus.edu", "Staff@123"},
                        {"Network Technician", "karthik@campus.edu", "Staff@123"},
                        {"Carpenter", "arun.carpenter@campus.edu", "Staff@123"},
                        {"General Maintenance", "vikram.maintenance@campus.edu", "Staff@123"},
                        {"Student (Alice)", "alice@student.campus.edu", "Student@123"},
                        {"Student (Bob)", "bob@student.campus.edu", "Student@123"}
                };

                assertEquals(expected.length + 1, cmbQuickFill.getItemCount(),
                        "Dropdown must contain prompt plus all 8 unique demo accounts");
                assertTrue(cmbQuickFill.getMaximumRowCount() < cmbQuickFill.getItemCount(),
                        "Dropdown popup should enable scrolling when items exceed maximumRowCount");

                Set<String> seenEmails = new HashSet<>();
                for (int i = 1; i < cmbQuickFill.getItemCount(); i++) {
                    String item = cmbQuickFill.getItemAt(i);
                    String[] expectedRow = expected[i - 1];
                    assertTrue(item.contains(expectedRow[0]), "Missing role label " + expectedRow[0]);
                    assertTrue(item.contains(expectedRow[1]), "Dropdown item must show email " + expectedRow[1]);
                    assertTrue(item.contains(expectedRow[2]), "Dropdown item must show password " + expectedRow[2]);
                    assertTrue(seenEmails.add(expectedRow[1].toLowerCase(Locale.ROOT)), "Duplicate email detected: " + expectedRow[1]);

                    cmbQuickFill.setSelectedIndex(i);
                    assertEquals(expectedRow[1], txtEmail.getText(), "Email autofill mismatch for index " + i);
                    assertEquals(expectedRow[2], new String(txtPassword.getPassword()), "Password autofill mismatch for index " + i);
                }

                List<JLabel> labels = collectComponents(frame.getContentPane(), JLabel.class);
                for (String[] row : expected) {
                    boolean foundCred = labels.stream()
                            .anyMatch(l -> l.getText() != null && l.getText().contains(row[1]) && l.getText().contains(row[2]));
                    assertTrue(foundCred, "Visible demo credential label missing for " + row[1]);
                }

                frame.addNotify();
                for (Dimension size : sizes) {
                    frame.setSize(size);
                    frame.validate();
                    layoutRecursively(frame.getContentPane());

                    assertTrue(cmbQuickFill.getWidth() >= 380,
                            "ComboBox width too narrow at " + size + ": " + cmbQuickFill.getWidth());
                    assertTrue(cmbQuickFill.getHeight() >= 34,
                            "ComboBox height too short at " + size + ": " + cmbQuickFill.getHeight());

                    Rectangle cmbBounds = SwingUtilities.convertRectangle(cmbQuickFill.getParent(), cmbQuickFill.getBounds(), frame.getContentPane());
                    Rectangle emailBounds = SwingUtilities.convertRectangle(txtEmail.getParent(), txtEmail.getBounds(), frame.getContentPane());
                    Rectangle passBounds = SwingUtilities.convertRectangle(txtPassword.getParent(), txtPassword.getBounds(), frame.getContentPane());
                    Rectangle contentBounds = frame.getContentPane().getBounds();

                    assertFalse(cmbBounds.intersects(emailBounds), "ComboBox must not overlap Email field at " + size);
                    assertFalse(cmbBounds.intersects(passBounds), "ComboBox must not overlap Password field at " + size);
                    assertTrue(cmbBounds.x >= 0 && cmbBounds.x + cmbBounds.width <= contentBounds.width,
                            "ComboBox exceeds window width at " + size + ": " + cmbBounds + " vs " + contentBounds);
                    assertTrue(emailBounds.x >= 0 && emailBounds.x + emailBounds.width <= contentBounds.width,
                            "Email field exceeds window width at " + size);
                    assertTrue(passBounds.x >= 0 && passBounds.x + passBounds.width <= contentBounds.width,
                            "Password field exceeds window width at " + size);
                }

                JButton btnTogglePassword = getField(frame, "btnTogglePassword", JButton.class);
                assertNotNull(btnTogglePassword);
                assertFalse(txtPassword.getEchoChar() == 0, "Password should be masked initially");
                btnTogglePassword.doClick();
                assertEquals((char) 0, txtPassword.getEchoChar(), "Show-password toggle must unmask password");
                btnTogglePassword.doClick();
                assertEquals('\u2022', txtPassword.getEchoChar(), "Second toggle must re-mask password");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                frame.dispose();
            }
        });
    }

    @Test
    @DisplayName("All 8 demo accounts exist active in MySQL and authenticate cleanly")
    void testAllDemoAccountsAuthenticateAgainstDatabase() throws Exception {
        Assumptions.assumeTrue(DBConnection.testConnection(), "Requires local MySQL connection");

        UserService userService = new UserService();
        String[][] accounts = {
                {"admin@campus.edu", "Admin@123", "ADMIN"},
                {"electrician@campus.edu", "Staff@123", "STAFF"},
                {"plumber@campus.edu", "Staff@123", "STAFF"},
                {"karthik@campus.edu", "Staff@123", "STAFF"},
                {"arun.carpenter@campus.edu", "Staff@123", "STAFF"},
                {"vikram.maintenance@campus.edu", "Staff@123", "STAFF"},
                {"alice@student.campus.edu", "Student@123", "STUDENT"},
                {"bob@student.campus.edu", "Student@123", "STUDENT"}
        };

        for (String[] row : accounts) {
            User user = userService.authenticate(row[0], row[1]);
            assertNotNull(user, "Expected user to authenticate: " + row[0]);
            assertTrue(user.isActive(), "Demo account must be active: " + row[0]);
            assertEquals(row[2], user.getRole().name(), "Role mismatch for " + row[0]);
        }
    }

    private static <T> T getField(Object target, String name, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    private static <T extends Component> List<T> collectComponents(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                result.add(type.cast(c));
            }
            if (c instanceof Container child) {
                result.addAll(collectComponents(child, type));
            }
        }
        return result;
    }

    private static void layoutRecursively(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) {
            if (c instanceof Container child) {
                layoutRecursively(child);
            }
        }
    }
}
