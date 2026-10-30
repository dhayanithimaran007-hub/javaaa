# Academic Demonstration & Presentation Script
## Smart Campus Complaint Prioritization and Tracking System

This script guides an evaluator, student, or examiner through a complete end-to-end demonstration of the application, validating all functional, architectural, and security requirements.

---

### Step 1: Launch & Database Health Verification (Workflow E)
1. Open terminal in `d:/Java project` and launch the application:
   ```cmd
   run.bat
   ```
2. On the **Login Screen**, click **"Database Settings"**:
   - Observe JDBC URL: `jdbc:mysql://localhost:3306/smart_campus_db?serverTimezone=Asia/Kolkata`
   - Click **"Test Connection"**: A non-blocking `SwingWorker` queries `SELECT 1` and reports connection latency and table count without freezing the UI.
   - Close the dialog.

---

### Step 2: Student Workflow (Workflow A)
1. **Self-Registration**:
   - On the login screen, click **"New Student? Register Here"**.
   - Note: Notice that the user role cannot be selected; the system strictly enforces the `STUDENT` role for public registration.
   - Fill in:
     - Name: `John Doe`
     - Email: `johndoe@student.campus.edu`
     - Department: `Information Technology`
     - Phone: `9876500001`
     - Password: `StudentPass@123`
   - Click **"Create Account"**. Receive success confirmation.
2. **Student Login**:
   - Enter `johndoe@student.campus.edu` and `StudentPass@123`.
   - Click **"Sign In"**. The **Student Portal** opens.
3. **Lodge Complaint & Priority Scoring Engine**:
   - Click **"+ Lodge Complaint"**.
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
   - Note the **Live Priority Preview**:
     - Formula: $P = 20 \times (0.35 \times 5 + 0.30 \times 5 + 0.20 \times 4 + 0.15 \times 4) = 20 \times (1.75 + 1.50 + 0.80 + 0.60) = 20 \times 4.65 = 93.00$
     - Result: **Score: 93.00 | Priority: CRITICAL | [CRITICAL SAFETY HAZARD]**
   - Click **"Submit Complaint"**.
   - Observe that Complaint ID is generated and saved in MySQL with initial `SUBMITTED` status.
4. **Inspect My Complaints & Tracking**:
   - Navigate to **"My Complaints"**.
   - Notice the complaint displayed with bold red `CRITICAL` badge and `[SAFETY RISK]` indicator.
   - Double-click the row to view **Complaint Details**:
     - Switch to **"Status History & Audit Trail"** tab.
     - Notice the initial entry logged with timestamp and priority breakdown.
5. **Sign Out**:
   - Click **"Sign Out"** in the sidebar.

---

### Step 3: Administrator Workflow (Workflow B)
1. **Admin Login**:
   - From the demo account dropdown on the login screen, select `Administrator (admin@campus.edu)` or type `admin@campus.edu` / `Admin@123`.
   - Click **"Sign In"**. The **Admin Control Center** opens.
2. **Dashboard Overview**:
   - Point out real-time KPI cards: Total Complaints, Submitted, Acknowledged, In Progress, Resolved, Critical Safety, Overdue, Average Resolution Time.
   - Under **"High-Priority & Safety-Critical Action Queue"**, notice John Doe's electrical complaint (#5) flagged at the top.
3. **All Complaints Filtering**:
   - Navigate to **"All Complaints"**.
   - Check **"Safety Hazards Only"**: The table filters down immediately to safety-critical issues.
   - Select John Doe's complaint.
4. **Staff Assignment**:
   - Click **"Assign to Staff"**.
   - Select `Rajesh Kumar (Electrician)` from the dropdown.
   - Enter dispatch notes: *"Emergency: Replace fan regulator and secure ceiling bracket immediately."*
   - Click **"Confirm Assignment"**.
   - Notice status changes to `ACKNOWLEDGED`, and the assigned staff column updates.
5. **Sign Out**:
   - Click **"Sign Out"**.

---

### Step 4: Maintenance Staff Workflow (Workflow C)
1. **Staff Login**:
   - Select `Staff: Electrician (electrician@campus.edu)` / `Staff@123`.
   - Click **"Sign In"**. The **Maintenance Workspace** opens.
2. **View Assigned Work Order**:
   - Notice the new assignment listed in **"My Active Work Orders"**.
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
   - Notice complaint status is now green `RESOLVED`.
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
     - Notice full chronological tracking history:
       1. Submitted by John Doe
       2. Acknowledged and assigned by Admin
       3. In Progress logged by Rajesh Kumar (Electrician)
       4. Resolved logged by Rajesh Kumar with full repair note
3. **Submit Feedback**:
   - Click **"Submit Feedback"**.
   - Select **"5 Stars - Excellent Resolution"**.
   - Comments: *"Prompt response, fixed before afternoon lecture started!"*
   - Click **"Submit Feedback"**.

---

### Step 6: Security & Authorization Verification (Workflow D)
1. **Student Privacy Check**:
   - John Doe can only see his own complaints in the table. Direct DAO and service-layer calls reject attempts to query other students' IDs (`SecurityException: Access denied`).
2. **Password Security**:
   - Passwords are never stored in plain text. Querying `SELECT email, password_hash FROM users;` in MySQL reveals standard `$2a$12$` BCrypt hashes.
3. **CSV Export & Formula Neutralization**:
   - Log in as `admin@campus.edu`.
   - Go to **"Analytics & CSV Export"** -> Click **"Export Authorized Report (CSV)"**.
   - Open generated CSV: verify that all formula characters (`=`, `+`, `-`, `@`) are neutralized with single quotes to prevent spreadsheet execution.
4. **Audit Trail**:
   - Navigate to **"Audit Trails"** to see an immutable log of every action executed during the demo.
