# Smart Campus — Elite Campus Intelligence & Complaint Management Platform
## Enterprise Java Swing Desktop Application with MySQL & Deterministic Priority Engine

[![Java](https://img.shields.io/badge/Java-25%2B-blue.svg)](https://www.oracle.com/java/)
[![Swing](https://img.shields.io/badge/GUI-Java%20Swing%20%2B%20FlatLaf-green.svg)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.x-orange.svg)](https://www.mysql.com/)
[![Build](https://img.shields.io/badge/Build-Maven-red.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-69%2F69%20Passing-brightgreen.svg)](docs/test-cases.md)

---

## 1. Abstract
The **Smart Campus — Elite Campus Intelligence & Complaint Management Platform** is a commercial-grade desktop operations suite engineered to revolutionize university facility and incident management. Campus grievances—such as hazardous electrical faults, water leaks, network outages, and structural hazards—frequently suffer from resolution paralysis when handled through subjective manual channels. 

This platform transforms campus maintenance operations into an **Operations Command Center** powered by an auditable, deterministic **Priority Scoring Engine**, live telemetry feeds, real-time workload balancing, and multi-tier role-based access for **Students**, **Campus Administrators**, and **Maintenance Staff**. Built purely with native Java Swing, FlatLaf, Java 2D Graphics, and MySQL persistence, it combines consumer-grade aesthetic elegance with mission-critical enterprise engineering.

---

## 2. Problem Statement
In traditional campus management setups:
1. **Subjective Prioritization**: Complaints are processed strictly chronologically or based on caller volume, allowing life-safety hazards (e.g., sparking electrical lines in laboratories) to languish behind routine paint touch-ups.
2. **Operational Opacity**: Students and facility managers lack real-time visibility into complaint triage, dispatch status, SLA milestones, and technician assignment.
3. **Technician Dispatch Imbalance**: Work orders are assigned without visibility into trade specializations or current task burdens, resulting in technician burnout and extended turnaround times.
4. **Data Security & Formula Risks**: Legacy campus tools often store plaintext passwords, expose student records to unauthorized eyes, or fail to protect CSV exports from formula injection attacks.

---

## 3. Core Objectives
- Deliver a native, commercial-grade desktop application using **Java 25**, **Java Swing**, and **FlatLaf**.
- Implement an explainable, auditable **Deterministic Priority Formula** combining technical severity, safety risk, affected occupant count, and operational impact.
- Provide a full-featured **Campus Operations Command Center** with live telemetry, safety alert banners, 8 real-time KPI metrics, interactive 2D charts, and live activity streams.
- Introduce an **Intelligent Staff Assignment Engine** pairing trade specialization with active technician workload metrics.
- Guarantee non-blocking UI responsiveness using `SwingWorker` for 100% of database queries.
- Ensure strict multi-tier **Role-Based Access Control (RBAC)**, BCrypt password hashing, parameter-bound `PreparedStatement` execution, and spreadsheet formula neutralization.
- Maintain 100% test coverage across priority scoring boundaries, lifecycle state machines, configuration resolution, and security policies (**69/69 automated tests**).

---

## 4. System Architecture & Project Structure

The platform adopts a clean multi-tier architectural pattern:
- **Presentation Layer**: Swing frames and panels styled with the `DesignSystem` token palette, collapsible `ModernSidebar`, rounded `CardPanel` containers, and Java 2D Graphics charts (`DonutChartPanel`, `BarChartPanel`, `HorizontalBarChartPanel`, `TrendLineChartPanel`).
- **Security & Authorization Layer**: `UserSession` singleton managing current desktop session context, verifying role permissions, and enforcing student ownership boundaries.
- **Service Layer**: Business engines (`PriorityService`, `ComplaintService`, `UserService`, `DatabaseHealthService`) providing SLA mapping, explainability breakdowns, and staff workload dispatch recommendations.
- **Data Access Layer (DAO)**: Pure JDBC with `PreparedStatement` parameters and transactional isolation (`Connection.setAutoCommit(false)`).
- **Persistence Layer**: Relational schema in MySQL (`smart_campus_db`).

```
d:\Java project\
├── pom.xml                                      # Maven configuration & shade fat-jar packaging
├── run.bat                                      # One-click Windows launch script
├── README.md                                    # Comprehensive system documentation
├── config/
│   ├── application.properties                   # Local DB configuration (gitignored)
│   └── application.properties.example           # Template DB configuration
├── src/
│   ├── main/
│   │   ├── java/com/smartcampus/
│   │   │   ├── app/SmartCampusApp.java          # Main Application Entry Point
│   │   │   ├── config/                          # AppConfig, DBConnection, DatabaseInitializer
│   │   │   ├── dao/                             # ComplaintDAO, UserDAO, AssignmentDAO, HistoryDAO, etc.
│   │   │   ├── model/                           # Domain Entities & Records (DTOs)
│   │   │   │   ├── enums/                       # Role, ComplaintStatus, PriorityLevel, Category
│   │   │   │   ├── ActivityFeedItem.java        # Real-time activity log event record
│   │   │   │   ├── StaffPerformanceDTO.java     # Technician productivity scorecard record
│   │   │   │   ├── StaffRecommendationDTO.java  # Intelligent assignment candidate record
│   │   │   │   └── StudentProfileDTO.java       # Student demographic & history scorecard
│   │   │   ├── security/                        # PasswordUtil (BCrypt Cost 12), UserSession
│   │   │   ├── service/                         # PriorityService, ComplaintService, UserService, DatabaseHealthService
│   │   │   ├── ui/
│   │   │   │   ├── LoginFrame.java              # Dual-panel enterprise authentication screen
│   │   │   │   ├── RegistrationDialog.java      # Student self-registration dialog
│   │   │   │   ├── theme/DesignSystem.java      # Midnight Navy palette tokens & Segoe UI typography
│   │   │   │   ├── components/                  # ModernSidebar, CardPanel, StatCard, ModernButton, ToastNotification
│   │   │   │   ├── charts/                      # DonutChartPanel, BarChartPanel, HorizontalBarChartPanel, TrendLineChartPanel
│   │   │   │   ├── student/                     # StudentDashboard, SubmitComplaintDialog, MyComplaintsPanel, FeedbackDialog
│   │   │   │   ├── admin/                       # AdminDashboard, AllComplaintsPanel, ReportsPanel, StaffManagementPanel,
│   │   │   │   │                                # StudentManagementPanel, StudentProfileDialog, PrintableReportDialog, AuditLogsPanel
│   │   │   │   └── staff/                       # StaffDashboard, AssignedComplaintsPanel, WorkProgressDialog, ResolutionDialog
│   │   │   └── util/                            # ValidationUtil, CsvSanitizer
│   │   └── resources/
│   │       ├── db/schema.sql                    # MySQL Schema DDL
│   │       ├── db/seed.sql                      # Initial Demo Accounts & Sample Data
│   │       └── logback.xml                      # Logging configuration
│   └── test/java/com/smartcampus/               # 69 Unit & Boundary Tests
├── docs/                                        # Architecture, ERD, Test Cases, User Guide, Demo Script
├── packaging/                                   # Standalone Executable JAR
└── scripts/                                     # Windows & Unix run / setup-db scripts
```

---

## 5. Elite Transformation — 10-Phase Competition Architecture

The platform was systematically transformed through 10 engineering phases to elevate it into an enterprise-grade commercial product:

| Phase | Module / Capability | Architectural Highlights |
|---|---|---|
| **Phase 1** | **Codebase & Architecture Preservation** | Non-breaking audit of domain logic, MySQL relational schema, and 100% preservation of passing test suites. Zero mockups or synthetic hardcoded data. |
| **Phase 2** | **Premium Enterprise Design System** | Palette tokens (`#10172A` Midnight Navy, `#17233B` Deep Navy, `#4776FF` Electric Blue, `#8665FF` Violet, `#25C6DA` Cyan, `#F4F6FB` Canvas). Rounded anti-aliased `CardPanel` containers, non-blocking `ToastNotification` system, and `ModernSidebar` (collapsible 245px $\leftrightarrow$ 72px icon mode). |
| **Phase 3** | **Campus Operations Command Center** | Real-time ticking clock widget, live MySQL ping latency telemetry chip, Critical Safety Incident alert banner with direct triage shortcut, 8 real-time KPI cards, pure Java 2D Graphics charts (`DonutChartPanel`, `TrendLineChartPanel`), Urgent Queue table, and a Live Activity Feed (`ActivityFeedPanel`). |
| **Phase 4** | **Intelligent Complaint Management** | Multi-parameter filtering in `AllComplaintsPanel` (Category, Priority, Status, Building, Date Range, Safety Hazard toggle, keyword search), client-side pagination (10/15/25/50 per page). Interactive `ComplaintDetailsDialog` with 4-step lifecycle progression stepper (Submitted $\to$ Acknowledged $\to$ In Progress $\to$ Resolved) and factor explainability breakdown. |
| **Phase 5** | **Smart Prioritization & Explainable Intelligence** | Deterministic 4-factor scoring formula $P = 20 \times (0.35S + 0.30R + 0.20A + 0.15I)$, SLA target resolution mapping (CRITICAL: 4h, HIGH: 24h, MEDIUM: 72h, LOW: 168h), detailed score breakdown calculation (`getDetailedFactorBreakdown()`), and tactical operational recommendations. |
| **Phase 6** | **Staff Management & Workload Balancing** | Technician directory with real-time active task counters and workload badges (Available, Moderate, Heavy, Overloaded). Automated staff recommendation engine (`recommendStaffForComplaint()`) scoring candidates based on trade specialization (Electrician, Plumber, IT Network, Carpenter, etc.) and live task burdens. |
| **Phase 7** | **Student Management Module** | Dedicated `StudentManagementPanel` directory with departmental filtering, search, and account status toggling. Modal `StudentProfileDialog` presenting student demographics, ticket summary cards, and complete historical complaint records. |
| **Phase 8** | **Advanced Analytics & Printable Executive Reporting** | `ReportsPanel` visual dashboard featuring category distribution donut chart, department horizontal bar chart (`HorizontalBarChartPanel`), technician performance scorecard (assigned, resolved, avg hours, satisfaction rating). Official Printable Report generator (`PrintableReportDialog`) formatting executive briefs with institutional headers and native printer service dispatch. |
| **Phase 9** | **Security, Audit & Live Database Health** | `AuditLogsPanel` with raw event inspector dialog, `DatabaseConnectionStatusDialog` featuring millisecond ping latency test and live schema record checklist across all core tables (`users`, `complaints`, `assignments`, `complaint_history`, `feedback`, `audit_logs`). BCrypt password hashing, PreparedStatement SQL injection defense, CSV formula neutralization. |
| **Phase 10** | **Final Polish & Competition Readiness** | Dual-panel `LoginFrame` with show/hide password toggle, demo persona selector, and live database health telemetry. Uniform modern sidebar navigation across all three role portals (Student, Admin, Staff). 100% passing automated test suite (**69/69 tests**). Zero broken interactions. |

---

## 7. Complaint Priority Formula & Engine

The **PriorityService** implements an auditable, deterministic formula calculating a composite priority score $P \in [0.0, 100.0]$:

$$P = 20 \times (0.35S + 0.30R + 0.20A + 0.15I)$$

Where:
- **$S$ = Technical Severity (1–5 scale)**: Functional failure degree.
- **$R$ = Safety Risk (0–5 scale)**: Physical danger or hazard (0 = no physical hazard).
- **$A$ = Affected Population (Normalized 1–5 scale)**:
  - 1–5 people $\rightarrow$ **1** (Individual / isolated workstation)
  - 6–20 people $\rightarrow$ **2** (Room / project team)
  - 21–50 people $\rightarrow$ **3** (Classroom / lab)
  - 51–200 people $\rightarrow$ **4** (Floor / major wing / lecture hall)
  - \>200 people $\rightarrow$ **5** (Entire building / campus-wide)
- **$I$ = Operational Impact (1–5 scale)**: Interruption to lectures, exams, or campus operations.

### Priority Level Thresholds
- **0.00 – 24.99**: `LOW` (Routine cosmetic/minor issues)
- **25.00 – 49.99**: `MEDIUM` (Standard maintenance)
- **50.00 – 74.99**: `HIGH` (Significant disruption requiring prompt action)
- **75.00 – 100.00**: `CRITICAL` (Emergency / severe disruption)

### Safety-Critical Human Review Override
Regardless of mathematical score, an issue is automatically flagged as **Safety-Critical** (`is_safety_critical = true`) if:
1. Safety Risk $R \ge 4$, OR
2. Severity $S = 5$ and Safety Risk $R \ge 2$, OR
3. Category is *"Safety and Security"* with $R \ge 3$ or $S \ge 4$, OR
4. Priority score falls in `CRITICAL` range ($\ge 75.0$).

This guarantees that physical hazards (e.g., exposed high-voltage wiring, gas leaks, falling debris) are surfaced to the top of administrator and staff queues even if isolated to a single student.

---

## 8. Database Setup & Configuration

### Prerequisites
- MySQL Community Server 8.0+ running on `localhost:3306`.

### Option A: Automatic Bootstrap (Recommended)
1. Ensure MySQL is running on port 3306.
2. Edit `config/application.properties` with your MySQL credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/smart_campus_db?serverTimezone=Asia/Kolkata&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
   db.user=root
   db.password=your_mysql_password
   ```
3. Run `run.bat`. The application automatically executes `schema.sql` and provisions demo accounts if the database is uninitialized!
4. If your password differs, click **"Database Settings"** on the Login screen, enter your credentials, and click **"Save & Initialize"**.

### Option B: Manual SQL Setup
Using the MySQL CLI or MySQL Workbench:
```cmd
mysql -u root -p < src/main/resources/db/schema.sql
mysql -u root -p < src/main/resources/db/seed.sql
```

---

## 9. Default Pre-Configured Demo Accounts

For immediate demonstration and examination evaluation, the seed script initializes the following accounts (all passwords hashed using BCrypt):

| Role | Name | Email Address | Password | Department |
|---|---|---|---|---|
| **ADMIN** | Campus Administrator | `admin@campus.edu` | `Admin@123` | Administration |
| **STAFF** | Rajesh Kumar (Electrician) | `electrician@campus.edu` | `Staff@123` | Electrical Maintenance |
| **STAFF** | Suresh Patel (Plumber) | `plumber@campus.edu` | `Staff@123` | Plumbing & Sanitation |
| **STUDENT** | Alice Sharma | `alice@student.campus.edu` | `Student@123` | Computer Science |
| **STUDENT** | Bob Verghese | `bob@student.campus.edu` | `Student@123` | Mechanical Engineering |

*Tip: The Login screen includes a **"Quick Demo Account"** dropdown that auto-fills credentials for any of the above personas with one click.*

---

## 10. Building and Running the Application

### 1. Launch via Windows Launcher
Double-click `run.bat` in the root folder.

### 2. Launch via Direct Java Command
```cmd
java --enable-native-access=ALL-UNNAMED -jar target/smart-campus-complaint-system-1.0.0-executable.jar
```
*(or using `packaging/smart-campus-app.jar`)*

### 3. Build & Package from Source using Maven
```cmd
mvn clean test
mvn clean package -DskipTests
```

---

## 11. Automated Test Execution

Run the complete JUnit 5 test suite:
```cmd
mvn test
```

### Test Results Breakdown:
- **`PriorityServiceTest`**: 32 tests (Scoring accuracy, thresholds, boundary values, population normalization, invalid input exceptions, and safety overrides).
- **`ComplaintStatusTransitionTest`**: 5 tests (Lifecycle state machine rules and invalid transition blocks).
- **`PasswordUtilTest`**: 7 tests (BCrypt hashing, matching, non-matching, empty input rejection).
- **`UserSessionTest`**: 4 tests (Authentication context, role enforcement, student ownership restrictions).
- **`ValidationUtilTest`**: 12 tests (RFC email format, password complexity, RFC 4180 CSV escaping, spreadsheet formula neutralization).
- **`ConfigurationResolutionTest`**: 9 tests (URL credential masking, configuration validation, property fallback preservation, live DB health check, live demo authentication).
- **Total**: **69 Tests Run, 0 Failures, 0 Errors, 0 Skipped (100% Pass Rate).**

Detailed test matrices are documented in [docs/test-cases.md](docs/test-cases.md).

---

## 12. Security Architecture & Threat Mitigations

1. **Password Security**: Passwords are never stored in plaintext. They are salted and hashed using BCrypt at cost factor 12.
2. **SQL Injection Defense**: 100% of SQL statements across all DAOs use parameterized `PreparedStatement`. No raw SQL string concatenation is used.
3. **Role-Based Access Control**: Sensitive actions are validated at the service layer (`UserSession.requireRole`). Button visibility in the UI is purely decorative; the service layer rejects unauthorized actions with `SecurityException`.
4. **Student Privacy Protection**: Students are strictly restricted to querying and modifying their own records via `UserSession.checkStudentOwnership(studentId)`.
5. **CSV Formula Injection Defense**: When exporting complaint reports to CSV, any cell beginning with `=`, `+`, `-`, or `@` is neutralized with a leading single quote (`'`), preventing remote code execution in Microsoft Excel or LibreOffice Calc.
6. **Self-Registration Lock**: Public registration strictly registers `STUDENT` accounts. Form tampering to select `ADMIN` or `STAFF` is impossible.

---

## 13. Documentation Index

- [docs/architecture.md](docs/architecture.md): System architecture, Mermaid tier diagrams, and threading model.
- [docs/database-design.md](docs/database-design.md): Relational schema, Mermaid ERD, table dictionaries, and index designs.
- [docs/test-cases.md](docs/test-cases.md): 69 automated test cases, boundary conditions, and test results.
- [docs/user-guide.md](docs/user-guide.md): Comprehensive operations manual for Students, Administrators, and Staff.
- [docs/demo-script.md](docs/demo-script.md): Step-by-step viva / examiner presentation walkthrough.

---

## 14. Conclusion & Acknowledgments

The **Smart Campus Complaint Prioritization and Tracking System** delivers a production-grade desktop management application that replaces chaotic manual logging with objective, auditable mathematical urgency calculation. By uniting Java 25, FlatLaf, robust JDBC architecture, and comprehensive security controls, the system provides campus facilities with transparent operations, rapid emergency response, and verifiable accountability.
