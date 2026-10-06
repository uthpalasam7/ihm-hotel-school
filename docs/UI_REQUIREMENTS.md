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

Apply the shared spacing and data-grid rules in [UI_STANDARDS.md](UI_STANDARDS.md)
to every new phase. The application shell controls the outer content width.

Desktop:

- Left navigation sidebar
- Header with active branch, user, and logout controls
- Main content area
- Breadcrumbs where useful

Mobile:

- Collapsible navigation drawer
- Compact header
- Touch-friendly controls
- Tables converted to cards or horizontal scroll where needed

Branch behaviour:

- If the logged-in user has one available branch, select it automatically and show it as read-only text.
- If the logged-in user has multiple available branches, show an active-branch selector in the header.
- Persist the selected branch and restore it only while the user is still authorized for that branch.
- Refresh branch-specific data after the active branch changes.
- Super administrators can switch between active branches.

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
- Duration months
- Status
- Start date
- End date
- Batch number
- Remarks

For new batches, enter only a positive whole batch number. Show the start-date
year and selected course short code as a non-editable prefix, with a preview of
the complete identifier. For example, entering `2` for PB starting in 2026 produces
`2026/PB02`. Pad to at least two digits, normalize redundant leading zeroes, and
retain numbers above 99. Recalculate the preview when the course or start year
changes; do not use today's year as a substitute for an unselected start date.
Missing/invalid inputs cannot produce a savable identifier. Continue submitting
the full batchNumber through the existing API; server authorization, validation,
and global uniqueness constraints remain in force. Editing keeps the existing
full-number field and never regenerates historical numbers from course/date edits.

Duration and status appear before the start/end date row. For new batches, a
valid start date and positive whole-month duration automatically suggest an
inclusive end date. Use the day before the corresponding day N months later;
if that end day exceeds the target month's length, use its final day.
Examples: 1 July + 6 months → 31 December; 15 July + 6 months → 14 January;
31 January + 1 month → 28 February (29 in a leap year).

The end date remains editable. Changing it manually stops automatic updates;
"Use calculated end date" explicitly restores them. Loading an existing batch
preserves its saved end date, including when its start date or duration changes,
until that action is selected. Invalid/missing inputs clear only automatically
calculated dates. Existing date-order validation still applies. Adjusting the end
date does not change duration, fee-installment count, or examination due date.
All dates in the batch wizard accept typed DD/MM/YYYY values as well as calendar
selection. Reject invalid dates instead of interpreting them as US month/day dates
or rolling them into the next month.

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

In Phase 5, show Photo, Full name, NIC, Contact number, Status, and Actions.
Active-enrollment counts and registration-number search begin in Phase 6 when
enrollment data exists.

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
- NIC with uppercase alphanumeric input
- Contact number as exactly 10 digits
- Alternative contact as exactly 10 digits when provided
- Email
- Address
- Date of birth
- Gender dropdown with Male, Female, Other, and Not specified options
- Photo
- Remarks

When NIC matches an existing student:

- Block duplicate creation
- Show the existing student
- Offer to open the record or create a new enrollment

### Student Profile

Phase 5 provides the Overview and photo-management content. Enrollment,
Attendance, Fees and payments, and Audit summary tabs are added only when their
underlying product phases are implemented.

Tabs:

- Overview
- Enrollments
- Attendance
- Fees and payments, administrator only
- Documents or photo
- Audit summary, authorized users only

### Student Card — Phase 6 implementation in progress

Provide administrator-only card actions from the student profile and successful
enrollment confirmation: Issue card, Preview, Print, Download PDF, Email PDF, and
Cancel/Replace card. Show the active card and issuance/replacement history. Require
confirmation and a reason for cancellation/replacement; explain that the old QR
will stop working. Reprinting retains the current credential.

Use the two-sided wallet-card design in the
[card and delivery plan](STUDENT_CARDS_QR_AND_DELIVERY_PLAN.md#card-design-and-handover):
logo, school name, Student ID label, student name, optional photo, human-readable
identifier, large QR, and school contact/return instructions. Keep photos optional
and exclude NIC, address, financial data, and batch registration numbers.

Use the stored full-size student photo for the card preview and printable PDF;
the list thumbnail is too small for an enlarged card portrait. Show the portrait
as a square crop in both the on-screen and printable card fronts.
Print/PDF handover is the default; offer optional email after staff check the saved
student address. Show absent-address, unconfigured-service, queued, provider-accepted,
and failed/retry states accurately. Card rendering/email failures must leave the
successful enrollment visible and allow retry without enrolling again.

### Lecturer Batch Roster — implemented in Phase 6

Lecturers can open Batches and view students only in currently assigned batches.
The roster shows student name, registration number, enrollment date, and status.
It provides no edit controls, NIC, contact details, remarks, or fee information.
Administrators retain their full student and enrollment management screens.

## 10. Enrollment Pages

The enrollment list follows the shared filter-card pattern: registration number,
student, or batch text search; enrollment status; `Apply filters`; and `Reset`.
The active branch comes from the shell. A student or batch link can scope the list;
Reset retains that scope and `Show all` removes it.

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
- Student card issuance or access to the existing student-level card (Phase 6)

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

Phase 7.5 implements this as an admin-only batch schedule page, linked from
regular batches. Weekly patterns use the shared Material grid on desktop and
cards on mobile. Pattern create/edit uses a reactive dialog with weekday,
start/end times, optional lecturer and classroom, and active/inactive status.
An active pattern can be deactivated after confirmation; saved sessions remain.
The preview accepts at most 366 inclusive days within the batch and optional
excluded dates. It shows new, kept and conflicting rows before the admin confirms
generation. Conflict, empty and expired previews cannot be submitted; editing a
pattern, changing the range or changing exclusions requires a fresh preview.
Dates entered in the form use DD/MM/YYYY. Manual and retired batches explain why
generation is unavailable. Phase 7.6 adds the admin Class sessions page: a
bounded month grid and server-paginated list for the active branch, with batch,
lecturer, date and status filters. Staff can create one-off sessions in either
schedule mode and edit, cancel or reschedule eligible sessions. Cancellation and
rescheduling require reasons; a replacement links to its original. A month too
large for the calendar shows a prompt to narrow the batch or use the list rather
than an incomplete calendar. Phase 7.7 opens the same Class sessions page to
lecturers, limited by backend authorization to currently assigned batches.
Lecturers can manage eligible scheduled sessions in those batches, including
co-taught and unassigned classes. The Batches menu and dashboard link to the
page; weekly-pattern editing and generation stay administrator-only. If an
assignment is revoked while the page is open, clear inaccessible details and
show the access error.

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

### QR Scanning — Planned Phase 8

Add Scan student cards to the selected session's attendance sheet. Keep its batch,
date, time, and lecturer visible. Offer staff-camera and USB QR scanner input, plus
manual marking for forgotten cards or failed scans. Explain camera permission and
provide a useful fallback if denied or unsupported.

Show name and available photo to the supervising staff member. Announce successful
server-confirmed saves, Already marked, unrecognized/revoked card, ineligible/wrong
batch, cancelled session, and save failures without disclosing unauthorized data.
Restore scanner input focus after each result and support keyboard operation.
Do not rely on sound or color alone.

Scans save Present with check-in time; staff review Late/Absent/Excused through
existing controls. Repeat scans do not overwrite recorded statuses. Unscanned
students stay unmarked until review, and final submission includes saved scans.
Overdue warnings remain staff-only and do not disable scanning. Both scheduling
modes use this same workflow. Never display success before server confirmation.

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
- Email PDF action for authorized administrators (planned Phase 10)
- Saved student recipient shown for checking before send; missing-address and
  unconfigured-service explanations with print/download still available
- Delivery status and audited retry/resend; provider acceptance is not confirmed delivery

Sending failures leave the successful payment and receipt visible. Retry delivery
without posting another payment or allocating another receipt number. Show voided
receipt status clearly and do not resend a stale PDF that hides a void.

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
