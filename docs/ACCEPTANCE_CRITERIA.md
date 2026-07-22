# Acceptance Criteria

## 1. Authentication and Roles

- Users can log in with valid credentials.
- Invalid login returns a generic error.
- Disabled users cannot log in or refresh tokens.
- Administrators and lecturers see role-appropriate navigation.
- Backend endpoints reject unauthorized operations.
- Lecturers cannot access financial details.

## 2. Branch Support

- The system contains the default IHM branch.
- A second branch can be added without schema changes.
- Batches belong to branches.
- Users are limited to assigned branches.
- Reports filter by branch.
- A user with one branch is not repeatedly asked to select it.

## 3. Courses and Batches

- Admin can create Pastry & Bakery with code PB.
- Admin can create batch `2026/PB02`.
- Duplicate batch numbers are rejected.
- Start and end dates are validated.
- One or more lecturers can be assigned.
- Batch fees and scheduling mode can be configured.

## 4. Students

- NIC is required.
- Duplicate normalized NIC is rejected.
- Existing student appears when matching NIC is entered.
- Student photo is optional.
- Phase 5 search works by name, NIC, and contact number. Registration-number
  search is added with enrollments in Phase 6.
- A student can have multiple enrollments.

## 5. Enrollments

- Admin can enroll an existing student.
- Registration number follows `batch/sequence`.
- First enrollment can become `2026/PB02/0001`.
- Duplicate student-and-batch enrollment is rejected.
- Registration number generation is safe during concurrent requests.
- Charges are generated atomically with enrollment.

## 6. Sessions

- Admin can define multiple weekly schedule patterns.
- System previews sessions before generation.
- Duplicate sessions are identified.
- Generated sessions fall within batch dates.
- Manual sessions can be created.
- Sessions can be cancelled.
- Sessions can be rescheduled with traceability.
- Cancelled sessions cannot receive attendance.

## 7. Attendance

- Lecturer sees only assigned batches.
- Attendance sheet lists eligible enrolled students.
- Mark All Present works.
- Individual students can be marked Absent, Late, or Excused.
- Late details can be entered.
- One attendance record exists per student enrollment and session.
- Cancelled sessions do not affect percentages.
- Historical changes are audited.
- Overdue fees display a warning.
- Attendance remains enabled for overdue students.

## 8. Fee Plan and Charges

- Admin can configure registration, course, and examination fees.
- Admin selects monthly due day.
- Admin selects examination due date.
- Course fee is split over duration months.
- Final generated installment total equals the course fee.
- Registration fee is due at enrollment.
- Invalid due dates use month-end rules.
- Existing charges do not change when the batch fee plan changes.

## 9. Discounts and Waivers

- Admin can apply fixed discount.
- Admin can apply percentage discount.
- Admin can apply full waiver.
- Reason and approver are mandatory.
- Final amount cannot become negative.
- Lecturer cannot access these actions.
- Changes are audited.

## 10. Payments

- Admin can select one or more full unpaid charges.
- UI does not allow partial charge payment.
- Backend rejects partial settlement attempts.
- Payment allocation total equals payment total.
- Payment creates a unique receipt.
- Payment history is visible to authorized users.
- Completed payment cannot be deleted.
- Authorized user can void with a mandatory reason.
- Voiding restores charge states.
- Void action is audited.

## 11. Reports

- All required attendance reports are available.
- All required financial reports are available.
- Reports respect permissions and branch access.
- Filters work.
- PDF export works.
- Excel export works.
- Print layout is readable.
- Lecturer reports do not expose restricted financial data.

## 12. Usability

- Critical screens work on desktop, tablet, and mobile.
- Forms show validation errors.
- Lists show loading, empty, and error states.
- Destructive actions require confirmation.
- Statuses are understandable without relying only on color.
- Logo and theme are consistently applied.

## 13. Security and Reliability

- No secrets are committed.
- Passwords are hashed.
- Production traffic uses HTTPS.
- File uploads are validated.
- Sensitive actions are audited.
- Database backups and restoration steps are documented.
- Automated tests pass.
- Production builds complete successfully.
