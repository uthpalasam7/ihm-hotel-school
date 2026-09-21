# Phase 6 card and email rollout checks

Phase 6 can create and print a student QR card, but scanning it to mark class
attendance is planned for Phase 8 after class sessions exist. These checks
verify the card and delivery foundations only.

## Physical card check

1. Use a test student with no real personal data. Enroll the student, then open
   **Students → Student card** and issue a card.
2. Download the two-page PDF. Print both pages at **actual size / 100%**, not
   “fit to page.” The intended finished card is 85.6 × 53.98 mm. Confirm the
   printer or card shop can align the front and back and provide a protective
   finish without covering the QR.
3. Check the name, identifier, optional photo, school logo, and contact line.
   Include one Sinhala and one Tamil sample if those scripts are used at IHM.
4. Scan the QR on the printed card with the planned staff phone and a USB **2D
   QR scanner**. Both should read the code quickly under typical classroom
   lighting. A 1D-only barcode scanner will not read QR codes.
5. Replace the test card and confirm the new printed QR differs. Keep the old
   card as a revoked-token test for Phase 8; Phase 6 does not yet have an
   attendance scan screen.

The PDF's QR has an automated decode test after rendering at 150 dpi. This is
not a substitute for physical equipment testing.

## School SMTP check

1. Obtain a school-controlled SMTP sender account and its host, port,
   authentication method, and password or app-specific credential from the
   email administrator. Do not commit these values to Git.
2. Set the `IHM_MAIL_*` variables listed in [`.env.example`](../.env.example)
   in the backend's runtime environment and restart the backend. The card page
   should show **Email card PDF** when a saved student email exists.
3. Use a test mailbox owned by the school. Confirm the address shown on the
   card page, explicitly queue one email, and refresh the delivery status.
   `ACCEPTED` means the SMTP service accepted it; also verify inbox receipt,
   attachment opening, and whether the message reached spam.
4. Test a deliberately invalid address and an unavailable SMTP server in a
   controlled environment. Inspect the recorded failure and retry state. Do
   not use real student addresses for these failure tests.
5. Replace or cancel a test card while its email is queued and confirm the old
   PDF is not delivered. Confirm that printing and downloading still work when
   SMTP is disabled.

After these checks, record the chosen printer, card stock, staff phone/browser,
USB 2D scanner, SMTP provider, and school contact wording in the deployment
notes. Keep `IHM_CARD_SECRET` stable and backed up: changing it prevents
reprinting existing active QR cards.
