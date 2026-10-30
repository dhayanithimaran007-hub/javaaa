# Test Cases & Automated Verification Report
## Smart Campus Complaint Prioritization and Tracking System

### 1. Test Execution Summary

- **Test Framework**: JUnit 5 (Jupiter 5.10.2) + Maven Surefire 3.2.5
- **Compiler Target**: Java 25 (Tested on Java 25.0.2 runtime)
- **Total Tests Executed**: 60
- **Passed**: 60
- **Failed**: 0
- **Errors**: 0
- **Skipped**: 0
- **Pass Rate**: 100%

---

### 2. Priority Scoring Engine Test Matrix (`PriorityServiceTest`)

Formula: $P = 20 \times (0.35S + 0.30R + 0.20A + 0.15I)$

| Test ID | Test Description | Inputs (S, R, People, I, Category) | Expected Score | Expected Priority | Expected Safety Flag | Status |
|---|---|---|---|---|---|---|
| `TC-PRIO-01` | Minimum possible values | S=1, R=0, P=1 (A=1), I=1 | 14.00 | `LOW` | `false` | PASS |
| `TC-PRIO-02` | Maximum possible values | S=5, R=5, P=500 (A=5), I=5 | 100.00 | `CRITICAL` | `true` | PASS |
| `TC-PRIO-03` | Medium score classification | S=2, R=1, P=10 (A=2), I=2 | 34.00 | `MEDIUM` | `false` | PASS |
| `TC-PRIO-04` | High score classification | S=4, R=2, P=100 (A=4), I=3 | 65.00 | `HIGH` | `false` | PASS |
| `TC-PRIO-05` | Normalization: 1 person | P=1 | N/A | Scale A=1 | N/A | PASS |
| `TC-PRIO-06` | Normalization: 5 people | P=5 | N/A | Scale A=1 | N/A | PASS |
| `TC-PRIO-07` | Normalization: 6 people | P=6 | N/A | Scale A=2 | N/A | PASS |
| `TC-PRIO-08` | Normalization: 20 people | P=20 | N/A | Scale A=2 | N/A | PASS |
| `TC-PRIO-09` | Normalization: 21 people | P=21 | N/A | Scale A=3 | N/A | PASS |
| `TC-PRIO-10` | Normalization: 50 people | P=50 | N/A | Scale A=3 | N/A | PASS |
| `TC-PRIO-11` | Normalization: 51 people | P=51 | N/A | Scale A=4 | N/A | PASS |
| `TC-PRIO-12` | Normalization: 200 people | P=200 | N/A | Scale A=4 | N/A | PASS |
| `TC-PRIO-13` | Normalization: >200 people | P=201, 1000 | N/A | Scale A=5 | N/A | PASS |
| `TC-PRIO-14` | Safety Override: Risk >= 4 | S=2, R=4, P=1, I=1 | 38.00 | `MEDIUM` | `true` | PASS |
| `TC-PRIO-15` | Safety Override: S=5 & R>=2 | S=5, R=2, P=2, I=1 | 54.00 | `HIGH` | `true` | PASS |
| `TC-PRIO-16` | Safety Category Override | S=2, R=3, P=5, I=2, "Safety" | 42.00 | `MEDIUM` | `true` | PASS |
| `TC-PRIO-17` | Invalid Severity (-1, 0, 6, 10)| S out of range [1..5] | IllegalArgumentException | — | — | PASS |
| `TC-PRIO-18` | Invalid Safety Risk (-2, -1, 6)| R out of range [0..5] | IllegalArgumentException | — | — | PASS |
| `TC-PRIO-19` | Invalid People Count (-1, 0) | P <= 0 | IllegalArgumentException | — | — | PASS |
| `TC-PRIO-20` | Invalid Impact (-1, 0, 6) | I out of range [1..5] | IllegalArgumentException | — | — | PASS |

---

### 3. Lifecycle Status Transition Matrix (`ComplaintStatusTransitionTest`)

| Current Status | Target Status | Permitted? | Rule / Business Constraint | Status |
|---|---|---|---|---|
| `SUBMITTED` | `ACKNOWLEDGED` | Yes | Dispatched by administrator to staff | PASS |
| `SUBMITTED` | `REJECTED` | Yes | Requires mandatory administrative rejection reason | PASS |
| `SUBMITTED` | `RESOLVED` | No | Cannot jump straight to resolved without work | PASS |
| `SUBMITTED` | `IN_PROGRESS` | No | Must be acknowledged/assigned first | PASS |
| `ACKNOWLEDGED`| `IN_PROGRESS` | Yes | Staff begins physical inspection/work | PASS |
| `ACKNOWLEDGED`| `REJECTED` | Yes | Rejected during triage | PASS |
| `ACKNOWLEDGED`| `RESOLVED` | No | Cannot skip active work phase | PASS |
| `IN_PROGRESS` | `RESOLVED` | Yes | Requires resolution summary from staff | PASS |
| `IN_PROGRESS` | `REJECTED` | Yes | Deemed unfeasible or out of campus scope | PASS |
| `RESOLVED` | `IN_PROGRESS` | Yes | Permitted administrative reopen for rework | PASS |
| `RESOLVED` | `SUBMITTED` | No | Reopening transitions to active progress | PASS |
| `REJECTED` | `SUBMITTED` | Yes | Permitted student/admin resubmission | PASS |
| Any Status | Self / Null | No | Self-transitions or nulls are rejected | PASS |

---

### 4. Security & Role Authorization Matrix (`UserSessionTest`, `PasswordUtilTest`)

| Test ID | Component | Scenario | Expected Outcome | Status |
|---|---|---|---|---|
| `TC-SEC-01` | `PasswordUtil` | Hash generation | Outputs valid BCrypt string ($2a$12$...) | PASS |
| `TC-SEC-02` | `PasswordUtil` | Matching password verification | Returns `true` | PASS |
| `TC-SEC-03` | `PasswordUtil` | Non-matching password verification | Returns `false` | PASS |
| `TC-SEC-04` | `PasswordUtil` | Blank / empty password input | Throws `IllegalArgumentException` | PASS |
| `TC-SEC-05` | `UserSession` | Unauthenticated access | Throws `SecurityException` | PASS |
| `TC-SEC-06` | `UserSession` | Student role verification | `hasRole(STUDENT)==true`, `hasRole(ADMIN)==false` | PASS |
| `TC-SEC-07` | `UserSession` | Student accessing own complaint | Permitted (`studentId == currentUserId`) | PASS |
| `TC-SEC-08` | `UserSession` | Student accessing peer complaint | Blocked with `SecurityException` | PASS |
| `TC-SEC-09` | `UserSession` | Administrator accessing complaint | Permitted across all students | PASS |

---

### 5. Input Validation & CSV Sanitization Matrix (`ValidationUtilTest`)

| Test ID | Input / Scenario | Expected Result | Status |
|---|---|---|---|
| `TC-VAL-01` | Valid emails (`alice@student.campus.edu`, etc.) | Returns `true` | PASS |
| `TC-VAL-02` | Invalid emails (`not-an-email`, `user@.com`, etc.) | Returns `false` | PASS |
| `TC-VAL-03` | Strong password (`Student@123`, `AdminPass99`) | Returns `true` | PASS |
| `TC-VAL-04` | Weak password (<8 chars, numbers only, letters only)| Returns `false` | PASS |
| `TC-VAL-05` | Formula payload `=cmd|'/C calc'!A0` | Escaped with leading quote: `'=cmd...` | PASS |
| `TC-VAL-06` | Formula payload `@SUM(1,2)` | Neutralized inside quotes: `"\'@SUM(1,2)\""` | PASS |
| `TC-VAL-07` | Formula payload `+12345` | Escaped with leading quote: `'+12345` | PASS |
| `TC-VAL-08` | Text with comma `Science Block A, Room 101` | Enclosed in quotes: `"Science Block A, Room 101"` | PASS |
| `TC-VAL-09` | Text with double quotes `Lab "Super" PC` | RFC 4180 escaped: `"Lab ""Super"" PC"` | PASS |
