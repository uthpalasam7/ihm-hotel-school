import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { map } from 'rxjs';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogResult,
} from './confirmation-dialog.component';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (!component.hasUnsavedChanges()) {
    return true;
  }

  const dialog = inject(MatDialog);
  return dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(
    ConfirmationDialogComponent,
    {
      data: {
        title: 'Discard unsaved changes?',
        message: 'Your changes have not been saved. Leave this page and discard them?',
        confirmLabel: 'Discard changes',
        cancelLabel: 'Keep editing',
      },
      maxWidth: '32rem',
      width: 'calc(100vw - 2rem)',
    },
  ).afterClosed().pipe(map((result) => Boolean(result?.confirmed)));
};
