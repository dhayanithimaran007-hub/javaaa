# User Guide & Operations Manual
## Smart Campus Complaint Prioritization and Tracking System

### 1. Introduction

The **Smart Campus Complaint Prioritization and Tracking System** is a native Java Swing desktop application designed for universities and academic institutions to manage campus facility maintenance, student grievances, automated priority queuing, and staff dispatching.

---

### 2. Getting Started & Authentication

#### Launching the Application
Execute `run.bat` (Windows) or `scripts/run.sh` (macOS/Linux). Alternatively, launch via command line:
```cmd
java --enable-native-access=ALL-UNNAMED -jar target/smart-campus-complaint-system-1.0.0-executable.jar
```

#### Login Screen
Upon startup, the **LoginFrame** appears:
- Enter your registered university email and password.
- **Quick Demo Account Dropdown**: Select any preconfigured demo user (`admin@campus.edu`, `alice@student.campus.edu`, `electrician@campus.edu`) to auto-populate credentials for evaluation.
- Click **"Sign In"** or press **Enter**.
- **Database Settings Button**: Click to test MySQL connectivity or change credentials on the fly.

#### Student Self-Registration
1. Click **"New Student? Register Here"** on the login screen.
2. Complete the form: Full Name, Email, Department, Phone, and Password.
3. *Note*: Public registration strictly assigns the `STUDENT` role. Administrator and Staff accounts cannot be self-registered.

---

### 3. Student Portal Guide

#### A. Overview Dashboard
- **KPI StatCards**: Displays Total Complaints, Pending Actions, Active In-Progress, Resolved, and Critical Issues submitted by you.
- **Recent Complaints Table**: Double-click any row to open the complete **Complaint Details & Audit Trail Dialog**.
- **"+ Lodge Complaint"**: Click to open the complaint submission form.

#### B. Submitting a Complaint
1. Click **"+ Lodge Complaint"** in the sidebar.
2. Enter:
   - **Title**: Short summary (e.g., *"Broken Projector in Lecture Hall 3"*).
   - **Category**: Select from the 9 campus categories.
   - **Building & Room**: Physical campus location.
   - **Description**: Detailed description of the problem.
3. Select scoring factors:
   - **Severity (1-5)**: Technical magnitude of failure.
   - **Safety Risk (0-5)**: Presence of physical hazard (0 = no physical risk, 5 = severe shock/collapse risk).
   - **Estimated People Affected**: Raw count of impacted occupants.
   - **Operational Impact (1-5)**: Disruption to classes/labs.
4. Observe the **Live Priority Preview** calculated deterministically using the formula:
   $$P = 20 \times (0.35S + 0.30R + 0.20A + 0.15I)$$
5. Click **"Submit Complaint"**.

#### C. Tracking Status & Submitting Feedback
- Click **"My Complaints"** in the sidebar to view all complaints you own.
- Filter by Category, Priority, or Status.
- Double-click a row to inspect the chronological timeline (Who changed the status, when, and work notes).
- For **RESOLVED** complaints: Click **"Submit Feedback"** to leave a 1–5 star rating and comment.

---

### 4. Administrator Control Center Guide

#### A. Dashboard Overview
- Real-time KPI statistics queried live from MySQL.
- **High-Priority & Safety-Critical Action Queue**: Lists urgent issues requiring staff dispatch.
- Double-click any entry to view full details.

#### B. All Complaints & Dispatch Management
- Navigate to **"All Complaints"**.
- Use the filter bar to isolate categories, buildings, or check **"Safety Hazards Only"**.
- **Assign to Staff**: Select a complaint and click **"Assign to Staff"**. Choose an active technician from the dropdown, add dispatch instructions, and confirm.
- **Reject Complaint**: Select a complaint and click **"Reject Complaint"**. Provide a mandatory rejection explanation.
- **Reopen Complaint**: Select a resolved/rejected complaint and click **"Reopen Complaint"** to return it to active progress.

#### C. Staff Management
- Navigate to **"Staff Directory"**.
- Click **"+ Add Maintenance Staff"** to provision a technician account with trade department (e.g., Electrical, Plumbing).
- Use **"Activate / Deactivate Staff"** to toggle active employment status.

#### D. User Directory
- View all campus accounts across `STUDENT`, `STAFF`, and `ADMIN` roles.
- Filter by role and active status, or search by name/email.
- Deactivate accounts without deleting historical data.

#### E. Analytics & CSV Export
- Navigate to **"Analytics & CSV Export"**.
- Inspect category breakdown, lifecycle status distributions, resolution percentage, and average turnaround time.
- Click **"Export Authorized Report (CSV)"** to save a formula-sanitized spreadsheet report.

#### F. Audit Logs
- Navigate to **"Audit Trails"** to inspect administrative activities (logins, assignments, status changes, user activations).

---

### 5. Maintenance Staff Workspace Guide

#### A. Overview & Tasks
- Displays assigned tasks categorized by Acknowledged, In Progress, Completed, and Critical Safety.
- Lists active work orders assigned specifically to you.

#### B. Updating Work Progress
1. Navigate to **"Assigned Complaints"**.
2. Select a task and click **"Update Work Progress"**.
3. Choose status (`ACKNOWLEDGED` or `IN_PROGRESS`) and enter work notes (e.g., *"Inspected electrical panel, ordered replacement breaker"*).
4. Click **"Save Progress"**. The update is instantly recorded in the complaint audit trail.

#### C. Completing Work
1. Select the task and click **"Mark Work Resolved"**.
2. Enter comprehensive resolution details (e.g., *"Replaced 32A MCB switch, tested wiring load, restored power"*).
3. Confirm resolution. The complaint status changes to `RESOLVED` and records the resolution timestamp.
