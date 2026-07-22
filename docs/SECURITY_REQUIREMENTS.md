# Security Requirements

## 1. Authentication

- Use secure password hashing supported by Spring Security.
- Never store plain-text passwords.
- Access tokens must be short-lived.
- Refresh tokens must be stored securely and revocable.
- Store refresh-token hashes, not raw token values where practical.
- Logout must revoke the current refresh token.
- Disabling a user must revoke active refresh tokens.
- Require password change for temporary first-login credentials.
- Rate-limit failed login attempts.
- Return generic login errors.

## 2. Authorization

Authorization is enforced in backend controllers and services.

Verify:

- Role
- Branch access
- Lecturer assignment
- Entity ownership or scope
- Record status
- Requested operation

Never rely only on:

- Hidden buttons
- Angular route guards
- Client-provided branch IDs
- Client-provided roles

## 3. Branch Isolation

For every branch-owned resource:

1. Resolve the authenticated user's allowed branches.
2. Confirm access to the requested branch.
3. Apply branch predicates to queries.
4. Prevent cross-branch ID enumeration.
5. Apply the same rules to exports and dashboards.

Super administrators may access all branches.

The frontend may send `X-Active-Branch-Id` as the user's selected branch context. The backend must treat this value as untrusted input, validate branch membership or super-administrator access, validate that the branch is active where applicable, and reject unauthorized branch selections.

## 4. Lecturer Restrictions

Lecturers may access:

- Assigned batches
- Related students
- Related sessions
- Related attendance
- Limited overdue warning information

Lecturers must not access:

- Payment history
- Receipt details
- Discounts and waivers
- Financial audit logs
- Unassigned batches
- Other branches unless assigned

The attendance-sheet endpoint must return a reduced financial view for lecturers.

## 5. Financial Security

- Recalculate totals on the server.
- Use exact decimal arithmetic.
- Do not accept arbitrary payment amounts for partial settlement.
- Validate selected charges belong to the same enrollment.
- Validate charges are still unpaid at transaction time.
- Lock or version financial rows during payment.
- Generate receipts on the server.
- Do not delete completed payments.
- Require reason and authorization for voiding.
- Audit all sensitive financial actions.

## 6. Personal Data

Protect:

- NIC
- Contact information
- Address
- Date of birth
- Student photo
- Payment records

Rules:

- Avoid personal data in application logs.
- Mask sensitive values where practical.
- Limit exports to authorized users.
- Use secure transport.
- Configure retention and backups responsibly.

## 7. File Upload Security

For student photos:

- Allow only JPEG and PNG MIME types in the initial implementation.
- Verify file signatures, not only extensions.
- Enforce a 5 MiB maximum file size and a bounded request size.
- Generate server-side file names.
- Prevent path traversal.
- Store outside publicly writable application directories.
- Serve through controlled endpoints or signed URLs.
- Re-encode images to remove metadata, bound source dimensions, constrain the
  full image, and generate a small list thumbnail.
- Reject executable content.

The local filesystem root is configured with
`IHM_STUDENT_PHOTO_STORAGE_PATH`. Photo responses require authentication and
must not expose storage keys or original file names.

## 8. API Security

- HTTPS in production.
- Validate all request bodies and parameters.
- Use a consistent maximum page size.
- Rate-limit authentication and sensitive endpoints.
- Prevent mass assignment through DTOs.
- Use CSRF protection appropriate to the token-storage design.
- Configure CORS explicitly.
- Do not expose stack traces.
- Add secure response headers.
- Use request IDs for diagnostics.

## 9. Secrets

Do not commit:

- Database passwords
- JWT signing secrets
- Production URLs containing credentials
- Cloud-storage credentials
- Administrator passwords

Provide `.env.example` with placeholder values only.

Use secret-management facilities in deployed environments.

## 10. Audit Security

- Audit logs are append-only through application workflows.
- Normal administrators cannot modify audit records.
- Capture actor, action, target, timestamp, and reason.
- Record payment voids, waivers, and attendance corrections.
- Restrict audit-log access.
- Avoid storing authentication secrets in audit values.

## 11. Dependency and Build Security

- Use supported stable versions.
- Pin major dependency versions.
- Review vulnerability reports.
- Do not automatically apply risky major upgrades.
- Keep build tools and base images maintained.
- Use minimal production containers.
- Run application containers as non-root where practical.

## 12. Backup and Recovery

- Automate PostgreSQL backups.
- Encrypt backups.
- Restrict backup access.
- Test restoration.
- Document recovery procedures.
- Include uploaded student photos in the backup plan.
