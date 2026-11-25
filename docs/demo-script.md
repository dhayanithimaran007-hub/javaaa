# Academic Demonstration & Presentation Script
## Smart Campus — Elite Campus Intelligence & Complaint Management Platform

This script guides an evaluator, student, or examiner through a complete end-to-end demonstration of the application, validating all functional, architectural, visual, and security requirements.

---

### Step 1: Launch & Database Health Telemetry (Enterprise Auth & Health)
1. Open terminal in `d:/Java project` and launch the application:
   ```cmd
   run.bat
   ```
2. On the **Login Screen**:
   - Point out the dual-panel enterprise layout: Midnight Navy branded showcase panel with feature highlights on the left, and crisp sign-in card on the right.
   - Notice the **Live Database Health Telemetry Chip** in the header displaying real-time ping latency (e.g., `DB: 2ms`) with a pulsing green indicator.
   - Click **"Database Settings"**:
     - Observe JDBC URL: `jdbc:mysql://localhost:3306/smart_campus_db?serverTimezone=Asia/Kolkata`
     - Click **"Test Connection"**: A non-blocking `SwingWorker` queries `SELECT 1` and audits record counts across all core tables (`users`, `complaints`, `assignments`, `complaint_history`, `feedback`, `audit_logs`).
     - Close the dialog.
   - Notice the **Show / Hide Password** eye toggle.

---

### Step 2: Student Workflow & Priority Engine (Workflow A)
1. **Self-Registration**:
   - On the login screen, click **"New Student? Register Here"**.
   - Note: Notice that the user role cannot be altered; the system strictly locks public registration to `STUDENT`.
   - Fill in:
     - Name: `John Doe`
     - Email: `johndoe@student.campus.edu`
     - Department: `Information Technology`
     - Phone: `9876500001`
     - Password: `StudentPass@123`
   - Click **"Create Account"**. A success toast notification appears.
2. **Student Login**:
   - Enter `johndoe@student.campus.edu` and `StudentPass@123` (or use the demo selector).
   - Click **"Sign In"**. The **Student Portal** opens with a collapsible `ModernSidebar`.
3. **Lodge Complaint & Smart Priority Scoring Engine**:
   - Click **"+ Lodge Complaint"** in the sidebar.
   - Enter:
     - Title: `Damaged Ceiling Fan Sparking in Classroom 302`
     - Category: `Electrical`
     - Building: `Academic Block B`
     - Room: `Room 302`
     - Description: `Ceiling fan wobbles violently and sparks when switched on. High risk of falling or electrical fire.`
     - Severity: `5`
     - Safety Risk: `5` (Immediate hazard)
     - Estimated People: `60` (Classroom capacity)
     - Operational Impact: `4`
   - Note the **Live Priority Scoring Preview**:
     - Mathematical Calculation:
       $$P = 20 \times (0.35 \times 5 + 0.30 \times 5 + 0.20 \times 4 + 0.15 \times 4) = 20 \times (1.75 + 1.50 + 0.80 + 0.60) = 20 \times 4.65 = 93.00$$
     - Live classification: **Score: 93.00 | Priority: CRITICAL | [CRITICAL SAFETY HAZARD]**
   - Click **"Submit Complaint"**. A floating toast confirms submission and initial `SUBMITTED` status is saved in MySQL.
4. **Inspect My Complaints & Priority Explainability**:
   - Navigate to **"My Complaints"**.
   - Notice the complaint displayed with bold red `CRITICAL` badge and `[SAFETY RISK]` indicator.
   - Double-click the row to open the upgraded **Complaint Details Inspector**:
     - Observe the **4-Step Lifecycle Progression Stepper** (Submitted $\to$ Acknowledged $\to$ In Progress $\to$ Resolved).
     - Switch to the **"Smart Prioritization"** tab to view the transparent factor breakdown, normalized population multiplier, SLA resolution target (4 Hours), and tactical recommendation.
     - Switch to **"Status History & Audit Trail"** to verify initial submission logging.
5. **Sign Out**:
   - Click **"Sign Out"** in the sidebar.

---

### Step 3: Administrator Operations Command Center (Workflow B)
1. **Admin Login**:
   - From the demo account dropdown on the login screen, select `Administrator (admin@campus.edu)` or type `admin@campus.edu` / `Admin@123`.
   - Click **"Sign In"**. The **Campus Operations Command Center** opens.
2. **Command Center Overview**:
   - Point out the **Live Clock Widget** (ticking in real-time) and live DB latency chip.
   - Point out the pulsing **Critical Safety Incident Alert Banner**: *"Action Required: Active Critical Safety Hazards detected! High-voltage, fire, or structural hazards require emergency response."*
   - Point out the **8 Real-Time KPI Cards**: Total Complaints, Submitted, Acknowledged, In Progress, Resolved, Critical Safety, Overdue, and Average Resolution Time.
   - Point out the **Java 2D Graphics Charts**:
     - **14-Day Complaint Volume Dynamics** gradient area trend chart.
     - **Category Distribution** interactive donut chart with hover tooltips and legends.
   - Point out the **Urgent & Safety-Critical Action Queue** table.
   - Point out the **Live Activity Feed**: Real-time chronological event stream displaying John Doe's complaint submission.
3. **All Complaints Filtering & Pagination**:
   - Navigate to **"All Complaints"**.
   - Demonstrate the multi-parameter filter toolbar: Category, Priority, Status, dynamic Building filter, Date Range, and search field.
   - Check **"Safety Hazards Only"**: The table filters immediately down to critical safety incidents.
   - Test client-side pagination controls (10/15/25/50 per page, Next, Previous).
4. **Intelligent Staff Assignment & Workload Recommendation**:
   - Select John Doe's complaint (#5).
   - Click **"Assign to Staff"**:
     - Notice the **Intelligent Recommendation Banner**: The system automatically identifies `Rajesh Kumar (Electrician)` as the ideal candidate (Trade: Electrical matches Category: Electrical, Workload: 1 active task, Confidence: 95%).
     - Click **"Apply Recommended Staff"** or choose another technician from the workload-badged dropdown.
     - Enter dispatch notes: *"Emergency: Replace fan regulator and secure ceiling bracket immediately."*
     - Click **"Confirm Assignment"**.
     - Notice status transitions to `ACKNOWLEDGED` and Assigned Staff updates instantly.
5. **Student Management Module (Phase 7)**:
   - Navigate to **"Student Directory"**.
   - Search for `John Doe` or filter by `Information Technology`.
   - Select John Doe and click **"View Student Profile"**:
     - Point out the modal profile card with student demographics, total lodged tickets, active tickets, and resolution history table.
     - Close the dialog.
6. **Advanced Analytics & Printable Executive Reporting (Phase 8)**:
   - Navigate to **"Analytics & Reports"**.
   - Observe the Category Donut Chart, Department Horizontal Bar Chart, and Technician Productivity Scorecard table.
   - Click **"Print Executive Report"**:
     - View the formal institutional executive briefing rendered in rich HTML with university header, summary KPI table, technician performance metrics, and official signature lines.
     - Point out the native **"Print Report"** button ready for paper or PDF export.
   - Close the report dialog.
7. **Sign Out**:
   - Click **"Sign Out"**.

---

### Step 4: Maintenance Staff Workflow (Workflow C)
1. **Staff Login**:
   - Select `Staff: Electrician (electrician@campus.edu)` / `Staff@123`.
   - Click **"Sign In"**. The **Maintenance Workspace** opens.
2. **View Assigned Work Order**:
   - Notice the new assignment listed in **"My Active Work Orders"** with high-priority accent.
   - Navigate to **"Assigned Complaints"**.
3. **Log Work Progress**:
   - Select the complaint and click **"Update Work Progress"**.
   - Select status `IN_PROGRESS`.
   - Enter note: *"Arrived at Room 302. Power isolated at circuit breaker. Disassembling damaged fan motor."*
   - Click **"Save Progress"**.
4. **Mark Work Resolved**:
   - Select the complaint and click **"Mark Work Resolved"**.
   - Enter resolution details: *"Replaced defective motor bearing and ceiling mount bracket. Reconnected wiring and tested on full speed. Safely operational."*
   - Click **"Confirm Resolution"**.
   - Notice complaint status is now green `RESOLVED` and resolved timestamp is recorded in MySQL.
5. **Sign Out**:
   - Click **"Sign Out"**.

---

### Step 5: Verification & Student Feedback (Workflow A Completed)
1. **Student Login**:
   - Log back in as `johndoe@student.campus.edu` / `StudentPass@123`.
2. **Verify Resolution**:
   - Notice KPI card **"Resolved"** incremented.
   - Under **"My Complaints"**, the status is green `RESOLVED`.
   - Double-click row:
     - Notice the **4-Step Stepper** is now fully green with checkmarks on Submitted, Acknowledged, In Progress, and Resolved!
     - Switch to **"Status History & Audit Trail"**: Verify complete 4-step chronological timeline with staff repair notes.
3. **Submit Feedback**:
   - Click **"Submit Feedback"**.
   - Select **"5 Stars - Excellent Resolution"**.
   - Comments: *"Prompt response, fixed before afternoon lecture started!"*
   - Click **"Submit Feedback"**.
   - A success toast confirms feedback is saved.

---

### Step 6: Security, Audit Trail & Health Verification (Workflow D)
1. **Student Privacy Protection**:
   - Direct service-layer checks enforce `UserSession.checkStudentOwnership(studentId)`. Querying another student's complaints is rejected with `SecurityException`.
2. **Password Security**:
   - Querying MySQL `SELECT email, password_hash FROM users;` reveals salted `$2a$12$` BCrypt hashes. Passwords never appear in logs or plaintext.
3. **CSV Export & Formula Sanitization**:
   - Log in as `admin@campus.edu`.
   - Navigate to **"Analytics & Reports"** $\to$ click **"Export Authorized Report (CSV)"**.
   - Open generated CSV in Excel or Notepad: verify all formula symbols (`=`, `+`, `-`, `@`) are escaped with leading single quotes (`'`) to neutralize CSV injection.
4. **Audit Trail Inspector**:
   - Navigate to **"Audit Trails"** to inspect the immutable chronological record of every login, complaint creation, assignment, progress change, and resolution.
   - Select any entry and click **"View Details"** to inspect the structured event payload.
5. **Automated Verification**:
   - Run `mvn test` in terminal: verify all unit, boundary, SLA escalation, and predictive maintenance tests pass with 0 failures and 0 errors.

---

### Step 7: Smart Campus 2.0 — Intelligence Engine A (Emergency Escalation Demo)
1. **Safety Detection & Override**:
   - Submit an `Electrical` complaint as a student with `Severity = 5`, `Safety Risk = 5`, `Affected = 60`.
   - Confirm `PriorityService` flags `[SAFETY OVERRIDE]` and logs `SAFETY_INCIDENT_DETECTED` to `audit_logs`.
2. **Emergency Command Center**:
   - Sign in as `admin@campus.edu`. Navigate to **"Emergency Operations"** in the sidebar.
   - Verify the active critical incident appears immediately at the top of the Emergency Incident table with live SLA countdown (`04h 00m` target).
3. **Incident Diagnostics & Escalation Trail**:
   - Double-click the incident to open **Emergency Incident Inspector**.
   - Review the safety override reason, priority score math, SLA deadline, and **Escalation Timeline** tab.
   - Use **Acknowledge Alert**, **Add Intervention Note**, and **Reassign Staff**.
4. **Controlled Overdue / SLA Breach Simulation**:
   - Execute `EmergencyEscalationServiceTest#testLiveEscalationReconciliationDeduplicationAndDemoA` via `mvn test` to verify controlled `+6h` `Clock.fixed` breach detection, restart reconciliation (Levels 1 -> 2 -> 3), concurrency locking, and administrator acknowledgment without modifying live production timestamps.

---

### Step 8: Smart Campus 2.0 — Intelligence Engine B (Predictive Maintenance Demo)
1. **Open Predictive Maintenance**:
   - In the Admin portal, click **"Predictive Maintenance"** in the sidebar.
2. **Execute Historical Pattern Analysis**:
   - Select an analysis window (e.g., `30 Days`) and click **"Run Predictive Analysis"**.
   - Inspect the 6 KPI cards (`Buildings Analyzed`, `Recurring Locations`, `High-Risk Buildings`, `Critical Maint Risks`, `Actions Pending`, `Completed Actions`), 4 Java 2D charts (`Complaints by Building`, `Recurring Failures by Category`, `Building Risk Comparison`, `Monthly Maintenance Trend`), and **Risk Ranking** table.
   - Double-click any risk row to inspect the explainable 5-factor weighted breakdown and triggered detection rules.
3. **Preventive Recommendation Lifecycle**:
   - Switch to the **Preventive Maintenance Recommendations** tab.
   - Select a recommendation and advance its lifecycle: **Under Review** -> **Approve** -> **Schedule Inspection** -> **Mark Completed**.
   - Verify every transition is recorded in **Security Audit**.
