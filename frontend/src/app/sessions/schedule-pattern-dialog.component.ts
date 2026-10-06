import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { BatchLecturer } from '../batches/batch.models';
import { errorMessage, fieldError } from '../shared/api-error';
import { Schedule, ScheduleStatus, WEEKDAYS } from './schedule.models';
import { ScheduleService } from './schedule.service';
import { forwardTimes, TIME_PATTERN } from './schedule-validators';

export interface SchedulePatternDialogData { batchId: number; batchNumber: string; lecturers: BatchLecturer[]; schedule?: Schedule; }
@Component({
  selector: 'app-schedule-pattern-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './schedule-pattern-dialog.component.html',
  styleUrl: './schedule-pattern-dialog.component.scss',
})
export class SchedulePatternDialogComponent {
  readonly data = inject<SchedulePatternDialogData>(MAT_DIALOG_DATA);
  private readonly dialog = inject(MatDialogRef<SchedulePatternDialogComponent>);
  private readonly api = inject(ScheduleService);
  private readonly destroyRef = inject(DestroyRef);
  readonly days = WEEKDAYS;
  readonly saving = signal(false);
  readonly error = signal<unknown>(null);
  readonly message = errorMessage;
  readonly fieldError = fieldError;
  readonly form = new FormGroup({
    dayOfWeek: new FormControl(this.data.schedule?.dayOfWeek ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(7)] }),
    startTime: new FormControl(this.data.schedule?.startTime ?? '09:00', { nonNullable: true, validators: [Validators.required, Validators.pattern(TIME_PATTERN)] }),
    endTime: new FormControl(this.data.schedule?.endTime ?? '13:00', { nonNullable: true, validators: [Validators.required, Validators.pattern(TIME_PATTERN)] }),
    defaultLecturerUserId: new FormControl<number | null>(this.data.schedule?.defaultLecturerUserId ?? null),
    classroom: new FormControl(this.data.schedule?.classroom ?? '', { nonNullable: true, validators: [Validators.maxLength(150)] }),
    status: new FormControl<ScheduleStatus>(this.data.schedule?.status ?? 'ACTIVE', { nonNullable: true, validators: [Validators.required] }),
  }, { validators: forwardTimes });
  readonly unavailableLecturer = this.data.schedule?.defaultLecturerUserId != null
    && !this.data.lecturers.some(l => l.lecturerUserId === this.data.schedule?.defaultLecturerUserId);

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving()) return;
    const value = this.form.getRawValue();
    this.error.set(null); this.saving.set(true); this.dialog.disableClose = true; this.form.disable({ emitEvent: false });
    this.api.save(this.data.batchId, { ...value, classroom: value.classroom.trim() || null,
      ...(this.data.schedule ? { version: this.data.schedule.version } : {}) }, this.data.schedule?.id)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
        this.saving.set(false); this.dialog.disableClose = false; this.form.enable({ emitEvent: false });
      })).subscribe({ next: () => this.dialog.close(true), error: error => this.error.set(error) });
  }
}
