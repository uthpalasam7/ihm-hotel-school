# Decisions and Assumptions

## Confirmed Decisions

1. Each lecturer has an account.
2. Lecturers see only assigned course batches.
3. Sessions may be created manually.
4. Registration number belongs to the student's enrollment in a batch.
5. NIC is mandatory.
6. The first version includes attendance and student fees.
7. Fee types are registration fee, course fee, and examination fee.
8. Course fee is split across the duration months.
9. The system is accessible through the internet from multiple devices.
10. The interface is English only.
11. All identified attendance and fee reports are required.
12. The architecture supports multiple physical branches from the beginning.
13. The school initially has one branch.
14. Administrator selects the monthly due day for each batch.
15. Partial payments are not allowed.
16. Discounts and waivers are allowed.
17. Examination-fee due date is configurable per batch.
18. Overdue fees show a prominent warning but do not block attendance.
19. Sessions can be automatically generated from weekly schedules.
20. Sessions can also be manually added, cancelled, edited, or rescheduled.
21. Approved 20 September 2026: Phase 6 includes one student-level QR card across
    courses, printable/downloadable as a PDF, with optional card email.
22. Phase 8 includes supervised camera/USB QR attendance for the selected class
    session; manual attendance remains available. Biometric attendance is deferred.
23. Phase 10 includes optional receipt PDF email alongside printing and downloading.
24. Email is staff-triggered to the checked saved student address; missing email
    or delivery failure never blocks enrollment, attendance, or payment.
25. Card replacement revokes the old QR token without changing enrollments or
    attendance history. Card photos remain optional.
26. The roadmap remains 15 phases. These additions are planned, not implemented;
    see [the detailed plan](STUDENT_CARDS_QR_AND_DELIVERY_PLAN.md).
27. User guidance direction confirmed 3 October 2026: reduce repeated developer-led
    training through role-specific in-app Help, clear workflow steps and actionable
    empty/error states, a first-use administrator setup checklist, and short
    illustrated task guides. Use videos only where demonstration adds value.
    Train one school administrator to support staff and onboard new users.
    Prioritize Help, the enrollment guide, and the lecturer attendance guide for
    the proposed pilot after Phase 8; observe staff using test records and improve
    confusing screens or instructions. These are planned additions, not implemented.

## Technical Decisions

1. Angular frontend.
2. Java 17 Spring Boot backend.
3. PostgreSQL database.
4. Flyway migrations.
5. JWT access and refresh tokens.
6. Docker Compose for local infrastructure.
7. LKR as initial currency.
8. Asia/Colombo as initial display timezone.
9. UTC timestamp storage.
10. Student-photo storage behind an abstraction.
11. In Phase 3, `ADMIN` users may manage lecturer accounts only within their assigned branches. `SUPER_ADMIN` remains responsible for branch management and for creating or changing `ADMIN` and `SUPER_ADMIN` accounts.
12. Student is a global identity record. Branch and lecturer scope is derived
    from enrollments beginning in Phase 6; Phase 5 Student Management is
    available to `SUPER_ADMIN` and `ADMIN` only.
13. Initial student-photo support accepts JPEG and PNG up to 5 MiB, re-encodes
    uploads, stores them behind an abstraction, and generates a thumbnail.

## Initial Business Assumptions

These assumptions may be changed by the project owner:

1. Registration fee is due on the enrollment date.
2. The first course installment uses the chosen due day in the batch start month.
3. When that due day falls before the batch start date, the first installment is due on the batch start date.
4. Later installments use the selected day in following months.
5. When a selected day does not exist, the due date is the month's final day.
6. Any installment rounding remainder is applied to the final installment.
7. Excused attendance is excluded from both the numerator and denominator in the first attendance-percentage calculation.
8. A payment may settle several full charges in one transaction.
9. Receipt numbering includes branch, year, and a server-generated sequence.
10. Lecturers may see only the overdue indicator and total overdue amount.

## Questions to Revisit Before Production

1. Should administrators require a second approver for large discounts or waivers?
2. Should a lecturer be allowed to create sessions or only administrators?
3. Should attendance be editable by lecturers after submission, or require administrator approval?
4. Is there a maximum number of days after which attendance is locked?
5. Should registration fee be required before final enrollment activation?
6. Are cheque payments considered completed immediately or after clearance?
7. What official legal receipt format is required?
8. What student-photo retention rules are required?
9. What backup retention period is required?
10. Which cloud or hosting provider will be used?
11. What school contact details, card artwork, and printing arrangement will be used?
12. Which email provider and sender account will deliver documents, with what retry
    limits and retention policy?
13. Which staff phones/browsers and USB QR scanners will be supported after testing?
