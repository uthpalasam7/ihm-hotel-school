# Business Rules

## 1. Student Rules

1. NIC is mandatory.
2. NIC is unique after normalization.
3. A student record is created only once.
4. Existing students must be reused for new enrollments.
5. Students with history must not be permanently deleted.
6. A student may enroll in multiple different batches.

## 2. Course and Batch Rules

1. Course short code is mandatory and unique.
2. Batch number is mandatory and globally unique.
3. Every batch belongs to one course and one branch.
4. Start date must be on or before end date.
5. Duration months must be positive.
6. A batch may have multiple lecturers.
7. Existing enrollment charges must not change when batch fees are edited later.
8. Batches support regular or manual scheduling.

## 3. Registration Number Rules

1. Registration number belongs to the enrollment.
2. Format:

```text
{batchNumber}/{fourDigitSequence}
```

3. Example:

```text
2026/PB02/0001
```

4. The sequence is unique within the batch.
5. Generation occurs on the server inside a transaction.
6. Concurrency must not generate duplicate numbers.
7. Registration numbers must not be reused after cancellation.

## 4. Enrollment Rules

1. A student cannot be enrolled in the same batch twice.
2. Enrollment creation generates financial charges.
3. Enrollment and charge generation occur in one transaction.
4. If charge generation fails, enrollment creation rolls back.
5. Cancelling an enrollment must not erase historical attendance or financial data.
6. Attendance eligibility depends on enrollment status.

## 5. Lecturer Access Rules

1. Lecturers access only assigned branches and batches.
2. Assignment validity may have start and end dates.
3. Frontend visibility does not replace backend authorization.
4. Removing a lecturer assignment does not remove historical records.
5. Lecturers cannot manage financial data.
6. Lecturers see only an overdue warning and total overdue amount.

## 6. Session Rules

1. Attendance is always linked to a class session.
2. Sessions can be generated from regular weekly schedules.
3. Sessions can be created manually.
4. A preview is required before bulk generation.
5. Duplicate active sessions must be prevented.
6. Cancelled sessions do not count toward attendance.
7. Attendance cannot be recorded for cancelled sessions.
8. Sessions with attendance must not be hard-deleted.
9. Rescheduling preserves a link to the original session.
10. Changing a session that already has attendance requires a warning and audit trail.

## 7. Attendance Rules

1. Only one record is allowed for one enrollment and one session.
2. Supported statuses are Present, Absent, Late, and Excused.
3. Mark All Present is available.
4. Late may include check-in time and late minutes.
5. Historical changes are audited.
6. Overdue fees never block attendance.
7. Cancelled sessions are excluded from calculations.
8. Default attended count is Present plus Late.
9. Reports must clearly state how Excused records affect the denominator.

Recommended percentage:

```text
(Present + Late) / eligible completed sessions × 100
```

For the initial implementation, Excused is excluded from both the numerator and denominator.

## 8. Fee Plan Rules

1. The batch fee plan contains:
   - Registration fee
   - Total course fee
   - Examination fee
   - Duration months
   - Monthly due day
   - Examination due date
2. Currency defaults to LKR.
3. Money uses exact decimal values.
4. Negative fees are not allowed.
5. Monthly due day is selected by an administrator.
6. If a due day does not exist in a month, use the month's final day.
7. The registration fee is due on the enrollment date.
8. Course installments cover the number of configured duration months.
9. The first course installment is due on the selected due day in the batch start month.
10. If that date is before the batch start date, the first installment is due on the batch start date.
11. Remaining installments use the selected due day in each following month.
12. The examination fee uses the batch's configured examination due date.
13. Generated installments must exactly total the total course fee.
14. Any rounding remainder is added to the final installment.

## 9. Charge Rules

1. Enrollment generates one registration charge.
2. Enrollment generates one course installment per duration month.
3. Enrollment generates one examination-fee charge.
4. Charge statuses are Upcoming, Due, Paid, Overdue, Waived, and Cancelled.
5. A charge is Due on its due date.
6. A charge is Overdue after its due date while unsettled.
7. Paid, Waived, and Cancelled charges are not overdue.
8. Existing charges are historical snapshots and are not recalculated when a fee plan changes.
9. Partial payment of a charge is not allowed.

## 10. Payment Rules

1. A payment may settle one or more full charges.
2. Every selected charge must be completely settled.
3. Allocation total must equal payment total.
4. Payment and allocation creation occur in one transaction.
5. Payments receive unique receipt numbers.
6. Completed payments cannot be deleted.
7. Incorrect payments are voided.
8. Voiding requires:
   - Authorized user
   - Mandatory reason
   - Audit log
   - Allocation reversal
   - Charge-status restoration
9. Lecturers cannot record or void payments.
10. The backend recalculates all totals and never trusts browser totals alone.

## 11. Discount and Waiver Rules

1. Supported adjustments:
   - Fixed amount discount
   - Percentage discount
   - Full waiver
2. A reason is mandatory.
3. Approval information is mandatory.
4. Final payable amount cannot be negative.
5. Percentage values must be valid.
6. Paid charges cannot be adjusted without a controlled reversal workflow.
7. Adjustments are not hard-deleted.
8. All changes are audited.

## 12. Branch Rules

1. The system supports multiple physical branches.
2. The initial branch is IHM-MAIN.
3. Every batch belongs to one branch.
4. Users may belong to one or more branches.
5. Reports respect branch access.
6. A one-branch user is not repeatedly asked to select a branch.
7. Cross-branch access is denied unless explicitly authorized.

## 13. Audit Rules

Sensitive actions must capture:

- User
- Branch
- Action
- Entity
- Entity ID
- Timestamp
- Old value where relevant
- New value where relevant
- Mandatory reason where relevant

Audit logs are read-only through normal business workflows.
