# Shared page and data-grid standards

Use these patterns for every new IHM administration, attendance, and finance page.
The Students list and profile are the visual reference. Do not add a separate
per-page outer `max-width` or centered margin to list and detail pages: the
application shell already provides the 90rem maximum content width and responsive
padding. Feature page roots use `display: grid` with `var(--ihm-space-4)` gaps.
Constrain only a specific form or reading panel when its content needs it.

## Page structure

- Keep application templates in separate `.component.html` files referenced by
  `templateUrl`. Component TypeScript holds metadata and behavior; do not inline
  page markup in its `template` property.
- Use `app-page-header` for the eyebrow, title, subtitle, and page actions.
- Use `mat-card appearance="outlined"` for filters, summaries, and results.
- Use the theme tokens in `frontend/src/styles.scss` for spacing, colors, and
  borders. A card normally has `var(--ihm-space-4)` or `var(--ihm-space-5)`
  content padding; a result table runs edge to edge inside its card.
- Keep summary fields in a compact responsive grid. Put status and actions next
  to their related record. Avoid large empty regions created by fixed heights or
  overly narrow content wrappers.

## Data grids

Use the shared `.ihm-data-grid` class from `frontend/src/styles.scss` on the
result card. Use Angular Material's native `<table mat-table [dataSource]="...">`
with `matColumnDef`, `mat-header-cell`, `mat-cell`, `mat-header-row`, and `mat-row`,
following the Students list. Wrap the table in `.table-wrap` and give it a
`.visually-hidden` caption. Headers use the same small uppercase style as
Students; rows use the same border, padding, dark record links, secondary text,
status chips, and right-aligned money/actions. Do not repeat these table rules in
each feature stylesheet.

Let Material own header height, line height, padding, and borders. Shared grid
styles provide IHM typography and record presentation; do not simulate Material
headers with plain table markup and fixed-height CSS. Bind the current server
page directly to `dataSource`; keep server-side pagination and filtering in the
existing services instead of adding client-side pagination over a partial dataset.

For new or updated record lists, pair the desktop table (`.ihm-desktop-table`)
with `.ihm-mobile-cards` and `.ihm-mobile-card` at 760px and below. The existing
Students and Users lists use equivalent mobile classes and are the visual
reference: clear identity and status, labeled details, then visible actions that
wrap within the card. Keep pagination outside the cards. Use the shared heading,
details, and actions classes instead of one-off mobile table widths. Only keep
horizontal table scrolling for genuinely tabular material such as a schedule
matrix, and document why a card view would lose meaning.

Filter cards use labeled search and relevant server-backed controls, followed by
`Apply filters` and `Reset` actions. Enter applies the search. Applying or resetting
filters returns to page one; pagination keeps the last applied filters. A Reset
clears only user-selected filters, not a student or batch scope opened from a
related record. Show that scope separately with a `Show all` link.

For paged lists, request data from the server. Use `mat-paginator` with first/last
controls and 10, 20, 50, and 100 rows per page. A change of page size must be
sent to the API; a new search or filter resets to page zero. Keep loading, empty,
error, and retry states visible through `app-page-state`.

## Printable student cards

Use the stored full-size photo for both browser preview and PDF. The card portrait
is square and cropped consistently in both; list thumbnails are for small list
avatars only. Embed the full photo pixels in the PDF, then verify a PDF exported
from the running backend contains those pixels. PDF layout and on-screen preview
must be checked together after changes. Test a physical print at actual ID-1 size
before issuing real cards.

Before finishing a new phase, compare its list and detail pages with Students at
desktop and mobile widths, inspect any printable output, and record exceptions
here when a workflow genuinely needs a different pattern.
