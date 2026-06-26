# UI Requirements

## 1. Design Direction

Use the supplied IHM Hotel School logo and a professional education-management style.

Theme:

- Primary gold: `#B0852C`
- Secondary brown: `#AD6715`
- Charcoal: `#292929`
- Light background: `#FDFDFD`
- Border grey: `#E5E5E5`

Requirements:

- English only
- Responsive desktop, tablet, and mobile layout
- Accessible labels and keyboard navigation
- Visible focus states
- Clear loading, success, empty, warning, and error states
- Avoid large gold background areas
- Use status badges consistently

## 2. Main Layout

Desktop:

- Left navigation sidebar
- Header with branch, user, and logout controls
- Main content area
- Breadcrumbs where useful

Mobile:

- Collapsible navigation drawer
- Compact header
- Touch-friendly controls
- Tables converted to cards or horizontal scroll where needed

## 3. Login Page

Include:

- IHM logo
- Username
- Password
- Show/hide password
- Login button
- Validation messages
- Generic authentication error
- Loading state

Do not reveal whether a username exists.

## 4. Navigation by Role

### Administrator

- Dashboard
- Branches
- Users
- Courses
- Batches
- Students
- Enrollments
- Sessions
- Attendance
- Fees
- Payments
- Reports
- Audit, when authorized
- Settings

### Lecturer

- Dashboard
- My Batches
- My Sessions
- Attendance
- Attendance Reports
- Profile

Do not show financial navigation to lecturers.

## 5. Administrator Dashboard

Cards:

- Active students
- Active batches
- Today's sessions
- Today's attendance percentage
- Fees collected this month
- Outstanding amount
- Overdue amount

Sections:

- Students absent today
- Overdue students
- Recent payments
- Upcoming sessions
- Sessions awaiting attendance

Every item should link to a filtered detail page.

## 6. Lecturer Dashboard

Cards:

- Assigned batches
- Today's sessions
- Upcoming sessions
- Sessions awaiting attendance

Sections:

- Frequently absent students
- Attendance percentage by batch

Financial information is limited to overdue warning details on attendance views.

## 7. Course Pages

### Course List

Columns:

- Course name
- Short code
- Status
- Number of batches
- Actions

Features:

- Search
- Status filter
- Add course
- Edit
- Activate/deactivate

### Course Form

Fields:

- Name
- Short code
- Description
- Status

## 8. Batch Pages

### Batch List

Columns:

- Batch number
- Course
- Branch
- Start date
- End date
- Duration
- Status
- Lecturer count
- Student count
- Actions

Filters:

- Branch
- Course
- Status
- Date range
- Search

### Batch Create/Edit Wizard

Step 1: General details

- Branch
- Course
- Batch number
- Start date
- End date
- Duration months
- Status
- Remarks

Step 2: Fee plan

- Registration fee
- Total course fee
- Examination fee
- Monthly due day
- Examination due date
- Installment preview

Step 3: Schedule

- Regular or manual mode
- Weekly patterns for regular mode
- Session preview
- Excluded dates

Step 4: Lecturer assignments

- Select one or more lecturers
- Assignment dates

Step 5: Review and save

Do not generate student charges until enrollment.

## 9. Student Pages

### Student List

Columns:

- Photo thumbnail
- Full name
- NIC
- Contact number
- Active enrollments
- Status
- Actions

Search by:

- Name
- NIC
- Contact number
- Registration number

### Student Form

Fields:

- Full name
- NIC
- Contact number
- Alternative contact
- Email
- Address
- Date of birth
- Gender
- Photo
- Remarks

When NIC matches an existing student:

- Block duplicate creation
- Show the existing student
- Offer to open the record or create a new enrollment

### Student Profile

Tabs:

- Overview
- Enrollments
- Attendance
- Fees and payments, administrator only
- Documents or photo
- Audit summary, authorized users only

## 10. Enrollment Pages

### Enrollment Form

- Search or create student
- Select branch
- Select course batch
- Enrollment date
- Remarks
- Fee summary
- Generated-charge preview
- Confirmation

After save, show:

- Registration number
- Enrollment details
- Generated charges

### Batch Student List

Columns:

- Registration number
- Photo
- Student name
- NIC
- Contact number
- Enrollment status
- Attendance percentage
- Payment status badge for administrators
- Overdue warning for lecturers
- Actions

## 11. Session Pages

### Session Calendar/List

Views:

- Calendar
- List

Filters:

- Branch
- Batch
- Lecturer
- Date range
- Status
- Attendance state

### Session Generation

For regular schedules:

1. Show weekly patterns.
2. Select generation date range.
3. Optionally exclude dates.
4. Preview proposed sessions.
5. Show duplicates and conflicts.
6. Confirm generation.

### Session Form

Fields:

- Batch
- Date
- Start time
- End time
- Lecturer
- Topic
- Classroom
- Remarks

Actions:

- Save
- Cancel session
- Reschedule
- Open attendance

Cancellation requires a reason.

## 12. Attendance Screen

Header:

- Course
- Batch
- Session date and time
- Lecturer
- Topic
- Attendance completion status

Toolbar:

- Mark all present
- Reset unsaved changes
- Save draft
- Submit attendance
- Search student

Student row/card:

- Photo
- Registration number
- Name
- Status selector
- Check-in time when Late
- Late minutes when Late
- Remarks
- Overdue warning when applicable

Overdue warning:

- Clear red or amber badge
- Text: `Payment overdue`
- Total overdue amount
- Does not disable attendance controls

Submission:

- Confirm before overwriting existing attendance
- Warn about unmarked students
- Display success confirmation

## 13. Fee and Charge Pages

### Enrollment Fee Account

Summary:

- Total original fees
- Discounts
- Waivers
- Paid
- Outstanding
- Overdue

Charge table:

- Type
- Description
- Due date
- Original amount
- Adjustment
- Payable amount
- Status
- Actions

Actions for administrators:

- Apply discount
- Apply waiver
- Record payment
- View receipt

### Discount/Waiver Dialog

- Type
- Fixed amount or percentage
- Calculated result
- Reason
- Approver
- Confirmation

## 14. Payment Screen

Workflow:

1. Select student enrollment.
2. Display unpaid charges.
3. Select one or more full charges.
4. System calculates total.
5. Select payment method.
6. Enter reference when applicable.
7. Add remarks.
8. Confirm.
9. Generate receipt.

Do not provide an amount field that permits partial payment.

### Receipt Screen

Include:

- IHM logo
- Branch
- Receipt number
- Student
- Registration number
- Batch and course
- Payment date
- Payment method
- Settled charges
- Total
- Received by
- Print button
- PDF download

## 15. Reports UI

Common report controls:

- Branch
- Course
- Batch
- Student
- Lecturer where relevant
- Date range
- Status
- Search
- Apply filters
- Reset
- Export PDF
- Export Excel
- Print

Show active filters in exported reports.

## 16. Status Presentation

Recommended badge meanings:

- Active / Paid / Present: success
- Due / Late / Warning: warning
- Overdue / Absent / Cancelled: danger
- Upcoming / Excused / Draft: neutral or info
- Completed: success or charcoal

Do not rely on color alone. Include readable text and icons where appropriate.

## 17. Confirmation and Error Behaviour

Require confirmation for:

- Session cancellation
- Session rescheduling when attendance exists
- Attendance overwrite
- Enrollment cancellation
- Applying waivers
- Payment submission
- Payment voiding
- User deactivation

Errors should explain:

- What failed
- Which field needs correction
- Whether the action can be retried

Never show raw stack traces.
