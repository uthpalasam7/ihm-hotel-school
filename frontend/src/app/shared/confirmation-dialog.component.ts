import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ConfirmationDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  reasonLabel?: string;
  reasonRequired?: boolean;
}

export interface ConfirmationDialogResult {
  confirmed: true;
  reason: string;
}

@Component({
  selector: 'app-confirmation-dialog',
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule],
  templateUrl: './confirmation-dialog.component.html',
  styleUrl: './confirmation-dialog.component.scss',
})
export class ConfirmationDialogComponent {
  protected readonly data = inject<ConfirmationDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmationDialogComponent, ConfirmationDialogResult | undefined>);

  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: this.data.reasonRequired ? [Validators.required] : [],
  });

  protected confirm(): void {
    this.reason.markAsTouched();
    if (this.reason.invalid) {
      return;
    }
    this.dialogRef.close({ confirmed: true, reason: this.reason.value.trim() });
  }
}
