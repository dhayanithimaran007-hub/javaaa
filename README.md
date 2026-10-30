# Smart Campus Complaint Prioritization and Tracking System
## Native Java Swing Desktop Application with MySQL & Automated Priority Engine

[![Java](https://img.shields.io/badge/Java-25%2B-blue.svg)](https://www.oracle.com/java/)
[![Swing](https://img.shields.io/badge/GUI-Java%20Swing%20%2B%20FlatLaf-green.svg)](https://www.formdev.com/flatlaf/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.x-orange.svg)](https://www.mysql.com/)
[![Build](https://img.shields.io/badge/Build-Maven-red.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-60%2F60%20Passing-brightgreen.svg)](docs/test-cases.md)

---

## 1. Abstract
The **Smart Campus Complaint Prioritization and Tracking System** is a robust, modular desktop application engineered to streamline university facility management. Campus issues such as hazardous electrical faults, water leakages, internet outages, and broken laboratory instruments frequently suffer from delayed resolution due to manual reporting bottlenecks and lack of objective urgency scoring. 

This application introduces an automated, deterministic **Priority Scoring Engine** that mathematically classifies complaints based on technical severity, physical safety hazards, affected occupant volume, and academic operational disruption. Built with native Java Swing, FlatLaf modern UI, and MySQL persistence, it delivers dedicated role-based portals for **Students**, **Campus Administrators**, and **Maintenance Staff**, backed by full audit history and spreadsheet formula-safe reporting.

---

## 2. Problem Statement
In traditional campus environments:
1. **Subjective Prioritization**: Complaints are processed strictly chronologically or based on caller volume, allowing life-safety hazards (e.g., sparking wires in labs) to wait behind cosmetic repairs.
2. **Lack of Transparency**: Students have no visibility into whether their complaint has been reviewed, dispatched, or when it will be fixed.
3. **Accountability Gaps**: Technicians lack structured progress-logging tools, and administrators cannot track mean resolution times or recurring facility failures.
4. **Data Vulnerabilities**: Many student systems store plaintext credentials or allow unauthorized cross-student data viewing.

---

## 3. Objectives
- Develop an enterprise-grade desktop application using **Java 25**, **Java Swing**, and **FlatLaf**.
- Implement a transparent, auditable **Deterministic Priority Formula** combining 4 weighted factors.
- Provide strict **Role-Based Access Control (RBAC)** across Student, Administrator, and Staff tiers.
- Guarantee non-blocking responsiveness by running all database queries off the Swing Event Dispatch Thread using `SwingWorker`.
- Persist structured records in **MySQL 8.x** with ACID transactions and immutable chronological status audit history.
- Ensure 100% test coverage for priority scoring boundaries, lifecycle transitions, CSV formula sanitization, and password security.

---

## 4. Scope
- **Student Scope**: Account registration, complaint lodging, real-time priority preview, personal complaint tracking, status timeline inspection, satisfaction feedback, and password updates.
- **Administrator Scope**: Full campus complaints visibility, priority & safety-critical filtering, staff assignment, status overrides/rejections with mandatory notes, technician management, user directory administration, KPI statistics, and CSV export.
- **Maintenance Staff Scope**: Viewing assigned work orders, updating task progress, logging work notes, and recording final resolution summaries.
- **Excluded Scope**: Public internet exposure (application communicates directly with MySQL via JDBC, designed for intranet/academic local deployment).

---

## 5. Technology Stack

| Layer | Component / Tool | Version / Details |
|---|---|---|
| **Runtime** | Java JDK | OpenJDK / Oracle JDK 25+ (Tested on Java 25) |
| **User Interface** | Java Swing + FlatLaf | FlatLaf 3.5.4 (Modern clean styling, badge renderers) |
| **Database** | MySQL Server | MySQL Community Server 8.0.x / 8.3.x |
| **Driver** | MySQL Connector/J | 8.3.0 |
| **Password Security** | BCrypt | `at.favre.lib:bcrypt:0.10.2` (Cost factor 12) |
| **Logging** | SLF4J + Logback | SLF4J 2.0.13 + Logback Classic 1.5.6 (Console & Rolling File) |
| **Testing** | JUnit 5 Jupiter | JUnit 5.10.2 (60 automated unit & boundary tests) |
| **Build & Packaging** | Apache Maven | Maven 3.9+ with `maven-shade-plugin` 3.5.3 (Single Fat Executable JAR) |

---

## 6. System Architecture

The application adopts a multi-tier architectural pattern:
- **Presentation Layer**: Swing frames (`LoginFrame`, `StudentDashboard`, `AdminDashboard`, `StaffDashboard`) using FlatLaf components and custom badge renderers.
- **Security & Authorization Layer**: `UserSession` singleton managing current desktop session context, verifying role permissions, and enforcing student ownership rules.
- **Service Layer**: Business workflows (`PriorityService`, `ComplaintService`, `UserService`, `DatabaseHealthService`).
- **Data Access Layer (DAO)**: Pure JDBC with `PreparedStatement` parameters and transactional isolation (`Connection.setAutoCommit(false)`).
- **Persistence Layer**: Relational schema in MySQL (`smart_campus_db`).

Detailed architectural diagrams and sequence flows are documented in [docs/architecture.md](docs/architecture.md).

```
d:\Java project\
├── pom.xml                                      # Maven configuration & packaging
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
│   │   │   ├── model/                           # Domain Entities (Complaint, User, Assignment, etc.)
│   │   │   ├── model/enums/                     # Role, ComplaintStatus, PriorityLevel, Category
│   │   │   ├── security/                        # PasswordUtil (BCrypt), UserSession
│   │   │   ├── service/                         # PriorityService, ComplaintService, UserService
│   │   │   ├── ui/                              # LoginFrame, RegistrationDialog
│   │   │   ├── ui/components/                   # BadgeCellRenderer, StatCard, ModernButton, DBStatusDialog
│   │   │   ├── ui/student/                      # StudentDashboard, SubmitComplaintDialog, MyComplaintsPanel
│   │   │   ├── ui/admin/                        # AdminDashboard, AllComplaintsPanel, ReportsPanel, StaffPanel
│   │   │   ├── ui/staff/                        # StaffDashboard, AssignedComplaintsPanel, WorkProgressDialog
│   │   │   └── util/                            # ValidationUtil, CsvSanitizer
│   │   └── resources/
│   │       ├── db/schema.sql                    # MySQL Schema DDL
│   │       ├── db/seed.sql                      # Initial Demo Accounts & Sample Data
│   │       └── logback.xml                      # Logging configuration
│   └── test/java/com/smartcampus/               # 60 Unit & Boundary Tests
├── docs/                                        # Architecture, ERD, Test Cases, User Guide, Demo Script
├── packaging/                                   # Standalone 6.6MB Executable JAR
└── scripts/                                     # Windows & Unix run / setup-db scripts
```

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
- **Total**: **60 Tests Run, 0 Failures, 0 Errors, 0 Skipped.**

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
- [docs/test-cases.md](docs/test-cases.md): 60 automated test cases, boundary conditions, and test results.
- [docs/user-guide.md](docs/user-guide.md): Comprehensive operations manual for Students, Administrators, and Staff.
- [docs/demo-script.md](docs/demo-script.md): Step-by-step viva / examiner presentation walkthrough.

---

## 14. Conclusion & Acknowledgments

The **Smart Campus Complaint Prioritization and Tracking System** delivers a production-grade desktop management application that replaces chaotic manual logging with objective, auditable mathematical urgency calculation. By uniting Java 25, FlatLaf, robust JDBC architecture, and comprehensive security controls, the system provides campus facilities with transparent operations, rapid emergency response, and verifiable accountability.
