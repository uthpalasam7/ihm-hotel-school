# IHM UI Modernization Plan

## 1. Objective

Modernize the IHM Hotel School frontend using Angular Material and a custom IHM design theme.

The modernization should improve:

- Visual consistency
- Form usability
- Navigation
- Tables and filtering
- Responsive behaviour
- Accessibility
- Loading and error feedback
- Maintainability

The migration must be gradual. Existing working functionality must remain available while screens are updated.

---

## 2. Technology Direction

Use:

- Angular Material `22.0.2`
- Angular CDK `22.0.2`
- Native CSS and Angular `animate.enter` / `animate.leave` when motion is required
- Angular Reactive Forms
- SCSS for IHM branding and layouts

The pilot uses the same `22.0.2` patch as the installed Angular framework packages.
Do not add the deprecated `@angular/animations`, `provideAnimations`, or
`provideAnimationsAsync` APIs for new UI code.

Do not upgrade Angular solely for this UI pilot unless the current version cannot safely support Angular Material.

Do not introduce another competing component library such as:

- PrimeNG
- NG-ZORRO
- Bootstrap component libraries

Custom SCSS may still be used for branding, page layouts, spacing, and responsive adjustments, but standard controls should use Angular Material where appropriate.

---

## 3. Design Principles

The updated UI should follow these principles:

- Simple and easy to understand
- Consistent across modules
- Responsive on desktop, tablet, and mobile
- Accessible through keyboard navigation
- Clear validation and error messages
- Minimal duplication of styles
- No fake data for design purposes
- No unnecessary animations
- No excessive use of colours
- No excessive number of buttons inside tables
- Clear distinction between primary and secondary actions

Existing business terminology should remain unchanged unless it is clearly confusing.

---

## 4. IHM Theme

Create a reusable Angular Material theme based on the existing IHM identity.

### Core Colours

- Gold: `#B0852C`
- Brown: `#AD6715`
- Charcoal: `#292929`
- Light neutral page background
- White card and form surfaces
- Consistent success, warning, information, and error colours

The implementation may adjust shades where required for:

- Readable text contrast
- Hover states
- Focus states
- Disabled states
- Accessibility

### Design Tokens

Define reusable values for:

- Typography
- Heading sizes
- Body text
- Button sizes
- Input heights
- Card padding
- Page spacing
- Section spacing
- Border radius
- Shadows
- Table density
- Status colours
- Responsive breakpoints

Avoid repeating raw colour values and spacing values throughout individual components.

### Typography

Use the native system UI font stack for both application text and Angular
Material component typography:

```text
ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
```

Configure this stack through the Angular Material theme's `plain-family` and
`brand-family` typography tokens, with regular, medium, and bold weights of
`400`, `500`, and `700`. Do not name an external font unless its font files are
bundled or loaded by the application.

### Buttons

Primary filled actions use a deep antique-gold surface with white text instead
of the brighter mustard theme colour. Use a moderate rounded rectangle rather
than a full pill shape. Text actions use the darker gold as an accent without a
filled background. Apply these choices through Angular Material button tokens
so create, save, and filter actions remain consistent.

---

## 5. First UI Pilot Scope

The first UI modernization pilot must include only:

1. Application shell and navigation
2. Dashboard
3. Branch list
4. Branch create/edit flow

Do not redesign Courses, Batches, Users, or future modules during this pilot.

---

## 6. Application Shell and Navigation

Modernize the existing application shell using Angular Material.

### Requirements

- Responsive side navigation
- Consistent top application header
- Preserve the existing IHM logo and branding
- Clear active-navigation indication
- Active branch selector
- User profile menu
- Change-password action
- Logout action
- Collapsible navigation where appropriate
- Mobile-friendly navigation
- Consistent page background
- Consistent page content width
- Consistent spacing between header, navigation, and content
- Appropriate loading behaviour while authentication is being restored

Show only destinations backed by implemented routes. Future module placeholders
that currently route to the Dashboard remain hidden until those modules exist.

Keep the desktop header visually quiet:

- Show multi-branch switching as a compact, clearly bounded branch context
  control with a status mark and menu, not a full outlined form field.
- Show single-branch context using the same visual surface without interactive
  menu affordances.
- Use an initials avatar as the account-menu trigger; place the user's name and
  roles inside the menu.
- Use text-only navigation. The active background and gold edge are sufficient;
  do not add letter-based navigation markers.

### Branch Selector

The active branch selector must preserve the existing multi-branch behaviour.

- Users with one branch should have it selected automatically.
- Users with multiple branches should be able to switch branches.
- Only branches authorized for the logged-in user should be displayed.
- The selected branch should remain visible in the header.
- Switching branches should refresh branch-scoped data.
- Frontend branch selection must not replace backend branch authorization.
- The current branch should remain selected after page reload when the user is still authorized for it.
- Super-administrator branch choices must page through the existing Branch API
  so the selector is not limited to the first 100 active branches.

Do not change existing authentication, refresh-token, authorization, or branch-security rules as part of the visual redesign.

---

## 7. Dashboard

Redesign the Dashboard using reusable Angular Material layouts and cards.

### Requirements

- Clear Dashboard title
- Clearly display the active branch
- Use summary cards for statistics already available from existing APIs
- Use readable labels and values
- Provide quick links to currently available important actions
- Responsive card layout
- Loading state
- Empty state
- Error state
- Appropriate behaviour when no branch is selected
- Accessible labels and keyboard behaviour

Do not:

- Invent fake statistics
- Add placeholder statistics that are not available
- Create new backend APIs solely to fill empty cards
- Display information the current user is not authorized to view

If the current Dashboard has very little real data, keep the design simple rather than filling it with decorative content.

The current application does not implement the documented Dashboard APIs.
For this pilot, show the active branch, authenticated role context, and
role-authorized quick links to implemented screens. Do not add metric cards
until real Dashboard data is available.

The persistent shell branch control satisfies the active-branch display
requirement. Do not repeat the same branch context inside the Dashboard content;
keep the Dashboard focused on the welcome message and authorized work areas.

---

## 8. Branch List

Redesign the Branch list using Angular Material components.

### Suggested Components

- `mat-table`
- `mat-paginator`
- `mat-form-field`
- `mat-input`
- `mat-button`
- `mat-menu`
- `mat-progress-spinner`
- A shared accessible status chip

### Requirements

- Search
- Server-side pagination using the existing `page` and `size` parameters
- Status display using consistent status chips
- Clear Add Branch action
- Edit action
- Responsive behaviour
- Loading state
- Empty state
- Error state
- Keyboard-accessible actions
- Clear feedback after create or update operations
- Preserve the current branch permissions and role restrictions

Avoid placing several large buttons in each row.

Use an action icon or action menu where that produces a cleaner result.

The implementation should still work properly when the number of branches is small.

The current backend enforces name-ascending ordering and does not honor arbitrary
sort parameters. Do not add `mat-sort` until a separate, documented server-side
sort contract is approved; sorting a single client page would be misleading.

---

## 9. Branch Create/Edit Flow

Redesign the Branch create/edit form using Angular Material.

The create/edit flow remains a route-based page. Dialogs are used only for
confirmation and reason capture.

### Suggested Components

- `mat-form-field`
- `mat-input`
- `mat-select`
- `mat-button`
- `mat-icon`
- `mat-dialog`
- `mat-snack-bar`
- `mat-progress-spinner`

### Requirements

- Responsive form layout
- Clear create and edit titles
- Clearly marked required fields
- Validation messages below related fields
- Correct edit-mode population
- Consistent Save and Cancel actions
- Disable Save while the form is invalid
- Disable repeated submissions while saving
- Show loading state while branch data is loading
- Show success notification after saving
- Show understandable error feedback when saving fails
- Preserve server-side validation messages where useful
- Warn before leaving when there are unsaved changes, where appropriate
- Return to an appropriate screen after successful saving

Do not change existing:

- Branch business rules
- Validation rules
- API contracts
- Security rules
- Audit behaviour
- Database structure

A confirmed defect may be fixed separately, but it must not be hidden inside an unrelated UI rewrite.

---

## 10. Shared UI Foundations

Create only the reusable components or patterns required by the first pilot.

Possible shared foundations include:

- Page header
- Summary card
- Status chip
- Search and filter toolbar
- Form section
- Form action bar
- Confirmation dialog
- Snackbar notification pattern
- Loading state
- Empty state
- Error state

Do not build a large abstract component framework before the application requires it.

A shared component should be created only when it:

- Is already needed in multiple places
- Provides consistent behaviour
- Reduces meaningful duplication
- Remains easy to understand and maintain

---

## 11. Form and Validation Standards

All modernized forms should follow the same behaviour:

- Use Angular Reactive Forms
- Show validation near the affected field
- Mark required fields consistently
- Avoid showing every validation error before the user interacts with the form
- Preserve valid entered values when another field fails
- Prevent duplicate submissions
- Show progress while saving
- Provide understandable API error feedback
- Do not expose raw backend exceptions to users
- Keep backend validation as the final source of truth

---

## 12. Loading, Empty, and Error States

Modernized pages should not appear broken while data is loading.

### Loading State

- Show a spinner or skeleton where appropriate
- Avoid displaying false empty states before loading finishes
- Prevent actions that require data until loading completes

### Empty State

- Explain that no records are available
- Offer a relevant action when the user has permission
- Do not show empty tables without explanation

### Error State

- Explain that data could not be loaded
- Provide a retry action where appropriate
- Do not leave the page permanently blank
- Preserve authentication handling for `401` and `403` responses

---

## 13. Responsive Behaviour

The first pilot must be checked on:

- Desktop
- Tablet
- Mobile-sized screens

Expected behaviour:

- Navigation collapses appropriately
- Forms become single-column on small screens
- Tables remain usable
- Important actions remain visible
- Text does not overlap
- Dialogs fit smaller screens
- Header content does not overflow
- Branch and user menus remain accessible

Horizontal scrolling may be used for data tables only when a better responsive layout is not practical.

---

## 14. Accessibility

Use Angular Material’s built-in accessibility features and preserve them during customization.

Check:

- Keyboard navigation
- Visible focus states
- Form labels
- Error-message association
- Button labels
- Icon-button accessible names
- Colour contrast
- Dialog focus handling
- Menu keyboard behaviour
- Status information not communicated only through colour

Do not remove focus indicators for visual reasons.

---

## 15. Styling Rules

- Prefer the global IHM Material theme.
- Prefer reusable layout classes and design tokens.
- Avoid repeated inline styles.
- Avoid large component-specific SCSS files where Material configuration can handle the styling.
- Do not override Material internals using fragile selectors unless necessary.
- Do not use `::ng-deep` unless there is no maintainable alternative and the reason is documented.
- Preserve existing styles for screens not included in the pilot.
- Do not allow the new theme to unintentionally break old screens.

---

## 16. Testing Requirements

Add or update frontend tests for the first pilot.

### Application Shell

Test:

- Navigation rendering
- Active route indication
- User menu actions
- Branch selector rendering
- Single-branch behaviour
- Multi-branch switching
- Responsive navigation behaviour where practical

### Dashboard

Test:

- Active-branch context
- Role-authorized quick actions
- No-active-branch state
- No fake metric rendering
- Active-branch changes

### Branch List

Test:

- Loading branches
- Search
- Server-side pagination and page-size changes
- Empty state
- Error state
- Add and edit navigation
- Status display

### Branch Form

Test:

- Create mode
- Edit mode
- Existing values populate correctly
- Required-field validation
- Save disabled for an invalid form
- Successful submission
- Failed submission
- Duplicate submission prevention
- Cancel behaviour
- Unsaved-change handling where implemented

Run:

```bash
cd frontend
npm test -- --watch=false
npm run build
```

Also verify:

```bash
docker compose config
git diff --check
```

The backend does not need to be changed for a purely visual migration. If backend files are changed to fix a confirmed defect, run the complete backend test suite and build.

### Stage 2 - Authentication Screens

When Stage 2 is implemented, add or update tests for:

- Login page loading, validation, and authentication error states
- Initial and forced password change screens
- Change password flow
- Session-expired and sign-in-required messages on the Login page
- Role-denied navigation and the dedicated Forbidden page
- Immediate logout behaviour

Stage 2 tests should verify that the existing JWT, refresh-token, authorization,
logout, and forced-password-change behaviour is preserved. The only redirect
changes are explanatory Login query parameters and routing authenticated users
without a required role to `/forbidden`.

---

## 17. Implementation Order

Follow this order:

1. Inspect the current Angular version and frontend architecture.
2. Confirm compatible Angular Material dependencies.
3. Add Angular Material and CDK.
4. Create the global IHM theme.
5. Add shared layout and feedback foundations.
6. Refactor the application shell.
7. Refactor the Dashboard.
8. Refactor the Branch list.
9. Refactor the Branch create/edit form.
10. Add and update tests.
11. Run tests and production build.
12. Perform desktop and mobile visual checks.
13. Stop and request review.
14. After approval, modernize Stage 2 - Authentication Screens.
15. After Stage 2 approval, modernize Stage 3 - Courses and Batches.
16. After Stage 3 approval, modernize Stage 4 - Users and Access Administration.
17. After Stage 4 approval, modernize Stage 5 - Students and Enrollments.
18. After Stage 5 approval, modernize Stage 6 - Sessions and Attendance.
19. After Stage 6 approval, modernize Stage 7 - Payments, Reports, Audit, and Settings.

Do not continue to other modules until the first pilot has been reviewed and approved.

---

## 18. Future UI Migration Stages

After the first pilot is approved, migrate other areas gradually.

Suggested order:

### Stage 2

- Login
- Initial or forced password change
- Change password
- Session-expired state shown as a Login alert
- Sign-in-required state shown as a Login alert
- Dedicated Forbidden page within the authenticated application shell
- Immediate logout from the account menu

Use one reusable split authentication layout: an IHM brand panel on desktop and
a single focused Material card on smaller screens. Login and change-password
forms use outlined Material fields, clear inline feedback, accessible password
visibility controls, and progress feedback. Normal password changes include a
Dashboard return action; required password changes do not.

Stage 2 is a visual modernization. Preserve the existing JWT and refresh-token
storage, API contracts, authorization checks, immediate logout, post-login
Dashboard destination, and forced-password-change workflow. Add only these
navigation refinements:

- Unauthenticated protected navigation redirects to
  `/login?reason=sign-in-required`.
- Refresh failure clears authentication state and redirects to
  `/login?reason=session-expired`.
- Authenticated role denial redirects to `/forbidden`.

Do not add forgotten-password functionality unless it is separately specified.

### Stage 3

- Courses
- Batch list
- Batch add/edit flow
- Lecturer selection

### Stage 4

- Users
- Roles
- User branch assignments

### Stage 5

- Students
- Enrollments

### Stage 6

- Sessions
- Attendance

### Stage 7

- Payments
- Reports
- Audit
- Settings

Each stage should reuse the approved theme and shared UI patterns.

---

## 19. Restrictions

During the first pilot:

- Do not redesign the entire application.
- Do not begin a new business-development phase.
- Do not change backend APIs without a confirmed technical need.
- Do not weaken authentication or authorization.
- Do not weaken branch-level data isolation.
- Do not remove existing audit behaviour.
- Do not change database migrations for visual reasons.
- Do not remove working features.
- Do not introduce multiple UI libraries.
- Do not upgrade unrelated dependencies.
- Do not add the deprecated `@angular/animations` package.
- Do not replace Reactive Forms with another form approach.
- Do not create fake Dashboard data.
- Do not migrate Courses, Batches, Users, or future modules.
- Do not commit generated build output or environment secrets.

During Stage 2:

- Do not change the existing JWT, refresh-token, authorization, immediate logout, post-login Dashboard, or forced-password-change behaviour.
- Limit redirect changes to the approved Login reason messages and dedicated `/forbidden` route.
- Do not add forgotten-password functionality unless it is separately specified.
- Do not change backend authentication rules or API contracts unless a confirmed defect requires it.
- Do not weaken authentication or authorization.

---

## 20. Pilot Completion Report

After completing the first pilot, provide:

- Confirmed Angular version
- Angular Material and CDK versions installed
- Files changed
- Theme and design tokens created
- Shared components created
- Application shell changes
- Dashboard changes
- Branch list changes
- Branch form changes
- Existing styles retained
- Existing styles removed
- Tests executed and results
- Build result
- Desktop and mobile checks completed
- Accessibility checks completed
- Known limitations
- Screens still using the old design
- Any remaining risks

Stop after completing the first pilot and wait for approval before modernizing Stage 2 or any later stage.
