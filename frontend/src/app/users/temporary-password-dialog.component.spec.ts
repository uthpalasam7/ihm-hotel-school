import { Clipboard } from '@angular/cdk/clipboard';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { vi } from 'vitest';
import { TemporaryPasswordDialogComponent } from './temporary-password-dialog.component';

describe('TemporaryPasswordDialogComponent', () => {
  it('copies the one-time password only when the administrator requests it', () => {
    const copy = vi.fn(() => true);
    TestBed.configureTestingModule({
      imports: [TemporaryPasswordDialogComponent],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: { username: 'lecturer', temporaryPassword: 'TempPass123' },
        },
        { provide: MatDialogRef, useValue: {} },
        { provide: Clipboard, useValue: { copy } },
      ],
    });

    const fixture = TestBed.createComponent(TemporaryPasswordDialogComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      copyPassword: () => void;
      copied: () => boolean;
    };

    expect(copy).not.toHaveBeenCalled();
    component.copyPassword();

    expect(copy).toHaveBeenCalledWith('TempPass123');
    expect(component.copied()).toBe(true);
  });
});
