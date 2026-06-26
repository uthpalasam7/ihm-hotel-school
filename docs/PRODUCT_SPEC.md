# Product Specification

## 1. Product Name

**IHM Hotel School Management System**

## 2. Product Goal

Provide a secure web application for IHM Hotel School to manage student enrollment, class attendance, lecturer access, fee schedules, fee collection, overdue payments, receipts, and operational reports.

The application must be accessible through the internet from multiple devices and locations.

## 3. Initial Scope

The first production version includes:

- One initial physical branch with support for additional branches
- Administrator and lecturer accounts
- Course definitions
- Course batches
- Lecturer assignments
- Student registration
- Multiple course enrollments per student
- Automatic registration-number generation
- Regular weekly schedules
- Automatic class-session generation
- Manual class-session creation
- Attendance marking
- Fee plans and automatic charge generation
- Discounts and waivers
- Payments without partial charge settlement
- Receipts
- Overdue warnings
- Attendance reports
- Financial reports
- Audit history

## 4. Out of Scope for the First Version

The following are not part of the first release unless separately approved:

- Examination marks
- Certificate generation
- Learning management content
- Online student self-registration
- Student portal
- Parent portal
- Online payment gateway
- WhatsApp or SMS notifications
- Biometric attendance
- QR attendance
- Payroll
- Inventory
- Accounting-ledger integration

The architecture should avoid blocking these future additions.

## 5. Users and Roles

### 5.1 Super Administrator

Can:

- Manage all branches
- Manage all users
- Access all operational and financial data
- View audit history
- Perform authorized corrections and voids

### 5.2 Administrator

Can operate within assigned branches and:

- Register students
- Manage courses and batches
- Assign lecturers
- Manage schedules and sessions
- Enroll students
- Configure batch fees
- Apply discounts and waivers
- Record and void payments
- Print receipts
- View and export reports
- Correct attendance with audit history

### 5.3 Lecturer

Can:

- View only assigned course batches
- View students enrolled in assigned batches
- View or create allowed class sessions for assigned batches
- Mark attendance
- View attendance reports for assigned batches
- See an overdue warning and total overdue amount

Cannot:

- View detailed payment history
- View discounts or waivers
- View receipts
- Create or modify financial records
- Access unassigned batches

## 6. Branches

The system starts with:

```text
Branch name: IHM Hotel School
Branch code: IHM-MAIN
Status: Active
```

Each course batch belongs to a branch. Users may belong to one or more branches. Reports must support branch filtering.

When a user has access to only one branch, the interface should select it automatically.

## 7. Courses

A course is a reusable programme definition.

Example:

```text
Course name: Pastry & Bakery
Short code: PB
```

Other examples:

- Food and Beverage
- Professional Cookery
- Housekeeping

Course fields:

- Name
- Short code
- Description
- Status
- Audit fields

## 8. Course Batches

A course batch represents a particular intake.

Example:

```text
Course: Pastry & Bakery
Batch number: 2026/PB02
Branch: IHM-MAIN
Start date: 2026-07-01
End date: 2026-12-31
Duration: 6 months
```

Batch information includes:

- Course
- Branch
- Batch number
- Start date
- End date
- Duration in months
- Schedule mode
- Fee plan
- Assigned lecturers
- Status
- Remarks

## 9. Students

Required fields:

- Full name
- NIC
- Contact number
- Address

Optional fields:

- Alternative contact number
- Email
- Date of birth
- Gender
- Photo
- Remarks

The NIC is mandatory and unique.

A student is created once and can be enrolled in multiple batches.

## 10. Enrollments

An enrollment connects a student to a course batch.

Example:

```text
Student: Nimal Perera
Batch: 2026/PB02
Registration number: 2026/PB02/0001
```

Enrollment fields include:

- Student
- Batch
- Registration number
- Enrollment date
- Status
- Remarks
- Audit fields

A student cannot enroll in the same batch more than once.

## 11. Lecturer Assignments

A batch can have one or more lecturers.

Assignments include:

- Lecturer
- Batch
- Assignment start date
- Assignment end date
- Status

Lecturer access is determined by active assignments and branch access.

## 12. Class Session Management

A class session represents one actual class occurrence.

Example:

```text
Batch: 2026/PB02
Date: 2026-07-06
Time: 09:00-13:00
Topic: Introduction to Baking
Lecturer: Assigned lecturer
```

### 12.1 Regular Schedule Mode

The administrator defines weekly patterns such as:

```text
Monday 09:00-13:00
Wednesday 09:00-13:00
```

The system previews and generates sessions between the batch dates.

Generated sessions can later be:

- Edited
- Cancelled
- Rescheduled
- Supplemented with extra sessions

### 12.2 Manual Schedule Mode

Sessions are created individually for irregular courses.

### 12.3 Session Statuses

- Scheduled
- Completed
- Cancelled
- Rescheduled

Cancelled sessions do not count toward attendance percentages.

## 13. Attendance

Attendance is marked for every enrollment in a class session.

Statuses:

- Present
- Absent
- Late
- Excused

Attendance workflow:

1. Lecturer opens an assigned class session.
2. The system displays all attendance-eligible enrollments.
3. The lecturer may select **Mark All Present**.
4. The lecturer changes individual statuses.
5. Optional remarks and late information are added.
6. Attendance is submitted.
7. Later edits are audited.

One attendance record is allowed per enrollment and session.

Overdue fees display a warning but do not block attendance.

## 14. Fee Plan

Every batch has:

- Registration fee
- Total course fee
- Examination fee
- Course duration in months
- Monthly installment due day
- Examination fee due date
- Currency

The course fee is divided across the duration months. Generated installments must total the exact course fee.

## 15. Student Charges

Enrollment creates:

- One registration-fee charge
- One course installment for each duration month
- One examination-fee charge

Statuses:

- Upcoming
- Due
- Paid
- Overdue
- Waived
- Cancelled

Partial settlement of a charge is not allowed.

## 16. Discounts and Waivers

Administrators may apply:

- Fixed discount
- Percentage discount
- Full waiver

A reason and approval information are required. All changes are audited.

## 17. Payments

Payment methods:

- Cash
- Bank transfer
- Card
- Cheque
- Other

A payment may settle one or more selected charges, but every selected charge must be settled in full.

Payments cannot be deleted. Incorrect payments are voided with a mandatory reason, authorization, and audit record.

Every completed payment receives a unique receipt number.

## 18. Overdue Warnings

A charge becomes overdue when:

- Its due date has passed
- It has not been fully paid, waived, or cancelled

Lecturers see:

- Payment overdue
- Total overdue amount

Lecturers do not see detailed financial records.

## 19. Dashboards

### Administrator Dashboard

- Active students
- Active batches
- Today's sessions
- Today's attendance percentage
- Students absent today
- Fees collected this month
- Outstanding amount
- Overdue amount
- Students with overdue fees
- Recent payments

### Lecturer Dashboard

- Assigned batches
- Today's classes
- Upcoming sessions
- Sessions awaiting attendance
- Attendance percentage by batch
- Frequently absent students

## 20. Reports

### Attendance Reports

- Daily attendance sheet
- Batch attendance
- Monthly attendance summary
- Individual student attendance
- Absence report
- Late report
- Attendance-percentage report
- Sessions without completed attendance

### Financial Reports

- Student payment history
- Student outstanding balance
- Batch fee collection
- Monthly collection
- Overdue payments
- Registration-fee report
- Examination-fee report
- Payment-method summary
- Daily cashier collection
- Voided payment report
- Discounts and waivers report

Reports support suitable filters, printing, PDF, and Excel export.

## 21. Non-Functional Requirements

- English-only interface
- Responsive on desktop, tablet, and mobile
- Internet-accessible
- Secure authentication
- Branch-aware permissions
- Audit logging
- Database backups
- Accessible controls
- Paginated lists
- Reliable financial calculations
- UTC timestamp storage
- Asia/Colombo default display timezone
- LKR default currency
