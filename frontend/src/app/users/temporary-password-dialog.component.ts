import { Clipboard } from '@angular/cdk/clipboard';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface TemporaryPasswordDialogData {
  username: string;
  temporaryPassword: string;
  title?: string;
}

@Component({
  selector: 'app-temporary-password-dialog',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './temporary-password-dialog.component.html',
  styleUrl: './temporary-password-dialog.component.scss',
})
export class TemporaryPasswordDialogComponent {
  protected readonly data = inject<TemporaryPasswordDialogData>(MAT_DIALOG_DATA);
  private readonly clipboard = inject(Clipboard);
  protected readonly copied = signal(false);

  protected copyPassword(): void {
    this.copied.set(this.clipboard.copy(this.data.temporaryPassword));
  }
}
