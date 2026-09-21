# Student Cards, QR Attendance, and Document Delivery

Approved planning scope: 20 September 2026. Phase 6 card issuance, PDF, and
optional SMTP delivery are implemented in the current worktree; QR attendance
remains planned for Phase 8 and receipt delivery for Phase 10. This keeps the
15-phase roadmap.

## Phase allocation

| Phase | Deliverables |
|---|---|
| 6 — Enrollment, Charges, and Student Cards | Student card issuance, preview, PDF download, printing, cancellation/replacement, and optional staff-triggered card email; shared email-delivery foundation |
| 8 — Attendance and QR Scanning | Supervised phone-camera and USB QR scanning for a selected class session, with manual attendance backup |
| 10 — Payments, Receipts, and Delivery | Receipt print/download and optional staff-triggered receipt email using the delivery foundation from Phase 6 |
| 13–15 | Audit visibility, security/usability checks, and production setup for these features |

Implement enrollment and atomic charge generation first within Phase 6, then cards,
then document delivery. Card rendering or email failure must not roll back a valid
enrollment or its charges. QR attendance depends on Phase 7 class sessions.

## Student card lifecycle

- An authorized administrator can issue a card after enrollment and access it from
  the student profile or enrollment confirmation.
- One student-level card works across that student's courses and batches. It does
  not replace or contain a batch-specific enrollment registration number.
- Keep at most one active card credential per student. Reprinting or downloading
  the same active card does not create another credential.
- Generate a unique, unpredictable card token on the server. Encode only that
  opaque token in the QR, without NIC, contact details, fees, or other personal data.
- The printed student/card identifier is separate from the QR token and from
  enrollment registration numbers; never use an NIC as the printed identifier.
- Replacing a lost card revokes the old token and issues a new one atomically.
  Retain issuance, revocation, replacement, and delivery history for audit.
- A valid card identifies a student; it does not by itself establish attendance
  eligibility, authenticate an account, or grant access to student information.
- Enforce administrator role and enrollment-derived branch access on card actions.
  A student-level card remains shared across enrollments; replacing it must not
  alter any enrollment or historical attendance.

## Card design and handover

Use a wallet-sized, two-sided design with the IHM logo, white background, charcoal
text, and restrained gold accents. Maintain a high-contrast QR with clear space
around it; do not place branding over its modules.

| Front | Back |
|---|---|
| IHM logo and school name | Large QR code |
| Student ID label | Human-readable student/card identifier |
| Student name and photo when available | Configured school contact details |
| Human-readable student/card identifier | Instructions for returning a lost card |

Photos remain optional; provide a clean layout when none is available. Exclude NIC,
address, financial data, and course-specific registration numbers from the card.
Confirm print dimensions, legibility, and scan reliability with sample prints
before issuing real cards; final artwork will be reviewed during Phase 6.

The default handover is a physical card printed by the school or from the PDF by
its chosen printing shop. Staff can optionally email the card PDF. A student may
present the digital QR on a phone if the scanning equipment supports screens.
Neither student smartphones nor student portal accounts are required.

## Session-based QR attendance

1. Authorized staff sign in and open a specific class session.
2. Choose Scan student cards and select camera scanning or USB scanner input.
3. Scan the card under staff supervision. The server resolves the token and checks
   the selected session, branch, lecturer assignment, and eligible enrollment.
4. Save attendance for that enrollment and session, then show the student's name,
   available photo, and a clear success result. Record check-in time, responsible
   staff member, and capture method for traceability.
5. Review unmarked students and mark absences, lateness, or excuses before final
   submission. Do not automatically mark every unscanned student absent.

Both regular and manually scheduled sessions support QR scanning. A school-entry
scan alone must not mark attendance for all of a student's classes.

Successful initial scans mark Present and retain the scan time. Staff can apply
Late with the existing attendance controls; automatic lateness needs a separately
agreed threshold before implementation. Existing records are not silently
overwritten: repeat scans return Already marked, while corrections use the normal
confirmation and audit workflow. Enforce the existing enrollment/session unique
constraint for simultaneous scans and retried requests.

Rejected scans include unknown/revoked cards, wrong-batch or ineligible enrollment,
cancelled sessions, and unauthorized access. Show useful errors without exposing
out-of-scope student details. Overdue fees remain warnings and never block scanning;
lecturers see only the allowed warning and overdue amount.

First release: authenticated, online, supervised scanning. Show success only after
server confirmation. If saving fails or its outcome is uncertain, explain that and
allow a safe retry; do not claim attendance was saved. Keep manual attendance and
a paper-register fallback for outages, forgotten cards, camera denial, or unreadable
QRs. Offline synchronization and unattended kiosks are deferred.

Support a staff phone camera and a USB 2D QR scanner in keyboard mode. A static QR
can be copied or shared, so staff must check the person against the displayed name
and available photo. Fingerprint/biometric equipment remains outside this release.

## Optional card and receipt email

- Provide Print, Download PDF, and Email PDF actions. Email is optional; the
  student email field remains optional and missing email must not block enrollment,
  card printing, attendance, or payment.
- Sending is an explicit authorized staff action. Show the destination from the
  student's saved email for staff to check before sending. Use the authorized
  profile-edit workflow to correct an address; do not add arbitrary recipients.
- Phase 6 introduces configurable school sender details and a replaceable email
  service integration. Phase 10 reuses it for receipts. Provider and sender account
  selection remain deployment decisions, not a requirement to use Gmail or Outlook.
- Persist delivery requests and status separately from enrollments and payments.
  Queue delivery only for committed records; email failures never undo enrollment,
  payment, allocations, or receipt numbering.
- Show queued, sending, accepted by provider, and failed states; track confirmed
  delivery/bounces only when supported. Provider acceptance is not proof of inbox
  delivery. Support explicit resend and bounded automatic retries without creating
  new payments, receipts, or card credentials.
- Reuse a delivery-request identity for network retries so a double-click or
  retried request does not create duplicate jobs. An intentional resend is a new,
  audited delivery attempt for the same document.
- Recheck authorization and document state before sending/retrying. Never send a
  revoked card or an outdated receipt that hides a payment's voided status.
- Record the acting user, document reference, recipient snapshot, timestamps,
  attempts, and outcome under restricted access. Do not log PDF contents, QR
  credentials, mail-service secrets, or unrestricted personal data.
- If email is unavailable or unconfigured, clearly explain this and retain print
  and download. Printing/downloading does not imply that a document was handed over
  or emailed.

## Receipt workflow in Phase 10

After a completed payment and its allocations are committed, generate the unique
server-numbered receipt and offer print, download, or email. Receipt contents remain
those defined in [UI Requirements](UI_REQUIREMENTS.md#receipt-screen). Email sends
the receipt PDF to that enrollment student's checked saved address. Receipt access
and delivery remain restricted to authorized administrators, never lecturers.

## Current implementation notes

- Student card tokens are random 256-bit values. Only a SHA-256 lookup hash and
  AES-GCM encrypted token are stored. `IHM_CARD_SECRET` protects reprinting;
  keep it stable and back it up securely. The QR contains no student data.
- The card PDF is generated on demand, not retained as a public artifact. Card
  URLs require an authorized administrator and return `Cache-Control: no-store`.
- A card email is queued only by staff to the saved student address. When SMTP
  is unconfigured, print and download still work. The queue records provider
  acceptance separately from inbox delivery. Automatic retries are limited to
  three attempts. The job is committed as `SENDING` before SMTP starts; if the
  process is interrupted during a send, that attempt is marked failed for manual
  review instead of automatically sent again. SMTP-reported failures may be
  retried, so school-side delivery testing must check the provider's behavior.
- The sample PDF has been visually checked at wallet size. Test a physical print
  and the school's chosen scanners before relying on QR check-in.
- The PDF now uses bundled Noto fonts and shaped high-resolution name artwork
  for Sinhala and Tamil names, with standard PDF text for supported Latin names.
  Sample Sinhala and Tamil PDF pages were visually inspected. Other unsupported
  scripts fail clearly instead of silently changing a name. Confirm actual
  printed legibility with the school before rollout.

## Decisions to finalize during implementation

- School contact details, final card artwork, and physical printing arrangement.
- Email provider, sender identity, credentials, delivery limits, and retry policy.
- Supported phone/browser and scanner combinations, confirmed with real cards.
- Delivery-history retention period and operational cleanup policy.

These decisions do not change the agreed one-card-per-student model, session-based
attendance, optional email, or existing financial and authorization rules.
