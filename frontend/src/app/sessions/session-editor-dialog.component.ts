import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { finalize, Observable } from 'rxjs';
import { Batch, BatchLecturer } from '../batches/batch.models';
import { errorMessage, fieldError } from '../shared/api-error';
import { DateValue, toIsoDate, toLocalDate } from '../shared/date-value';
import { DayMonthYearDateAdapter } from '../shared/day-month-year-date-adapter';
import { ClassSession, SessionRescheduleResponse } from './session.models';
import { SessionService } from './session.service';
import { forwardTimes, TIME_PATTERN } from './schedule-validators';

export interface SessionEditorData {
  batch: Batch;
  lecturers: BatchLecturer[];
  mode: 'create' | 'edit' | 'reschedule';
  session?: ClassSession;
}
export type SessionEditorResult = ClassSession | SessionRescheduleResponse | { conflict: true } | { accessDenied: true };

@Component({
  selector: 'app-session-editor-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule],
  providers: [{ provide: DateAdapter, useClass: DayMonthYearDateAdapter }, { provide: MAT_DATE_LOCALE, useValue: 'en-GB' }],
  templateUrl: './session-editor-dialog.component.html',
  styleUrl: './session-editor-dialog.component.scss',
})
export class SessionEditorDialogComponent {
  readonly data = inject<SessionEditorData>(MAT_DIALOG_DATA);
  private readonly api = inject(SessionService);
  private readonly dialog = inject(MatDialogRef<SessionEditorDialogComponent, SessionEditorResult>);
  private readonly destroyRef = inject(DestroyRef);
  readonly saving = signal(false);
  readonly error = signal<unknown>(null);
  readonly message = errorMessage;
  readonly fieldError = fieldError;
  readonly minDate = toLocalDate(this.data.batch.startDate);
  readonly maxDate = toLocalDate(this.data.batch.endDate);
  readonly form = new FormGroup({
    sessionDate: new FormControl<DateValue>(toLocalDate(this.data.session?.sessionDate ?? null), Validators.required),
    startTime: new FormControl(this.data.session?.startTime.slice(0, 5) ?? '09:00', { nonNullable: true, validators: [Validators.required, Validators.pattern(TIME_PATTERN)] }),
    endTime: new FormControl(this.data.session?.endTime.slice(0, 5) ?? '13:00', { nonNullable: true, validators: [Validators.required, Validators.pattern(TIME_PATTERN)] }),
    lecturerUserId: new FormControl<number | null>(this.data.session?.lecturerUserId ?? null),
    topic: new FormControl(this.data.session?.topic ?? '', { nonNullable: true, validators: Validators.maxLength(300) }),
    classroom: new FormControl(this.data.session?.classroom ?? '', { nonNullable: true, validators: Validators.maxLength(150) }),
    remarks: new FormControl(this.data.session?.remarks ?? '', { nonNullable: true, validators: Validators.maxLength(2000) }),
    reason: new FormControl('', { nonNullable: true, validators: this.data.mode === 'reschedule'
      ? [control => control.value.trim() ? null : { required: true }, Validators.maxLength(2000)] : [] }),
  }, { validators: forwardTimes });
  readonly initialLecturerUnavailable = this.data.session?.lecturerUserId != null
    && !this.eligibleLecturers().some(l => l.lecturerUserId === this.data.session?.lecturerUserId);

  constructor() {
    this.form.controls.sessionDate.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      const selected = this.form.controls.lecturerUserId.value;
      if (selected && !this.eligibleLecturers().some(l => l.lecturerUserId === selected)) {
        this.form.controls.lecturerUserId.setValue(null);
      }
    });
    if (this.initialLecturerUnavailable) this.form.controls.lecturerUserId.setValue(null);
  }

  eligibleLecturers(): BatchLecturer[] {
    const date = toIsoDate(this.form.controls.sessionDate.value);
    if (!date) return [];
    return this.data.lecturers.filter((lecturer, index, all) => lecturer.status === 'ACTIVE'
      && lecturer.assignmentStartDate <= date && (!lecturer.assignmentEndDate || lecturer.assignmentEndDate >= date)
      && all.findIndex(other => other.lecturerUserId === lecturer.lecturerUserId && other.status === 'ACTIVE'
        && other.assignmentStartDate <= date && (!other.assignmentEndDate || other.assignmentEndDate >= date)) === index);
  }

  save(): void {
    this.form.markAllAsTouched();
    const value = this.form.getRawValue(), date = toIsoDate(value.sessionDate);
    if (this.saving() || this.form.invalid || !date || date < this.data.batch.startDate || date > this.data.batch.endDate) return;
    if (this.data.mode === 'reschedule' && this.data.session && date === this.data.session.sessionDate
      && value.startTime === this.data.session.startTime.slice(0, 5)
      && value.endTime === this.data.session.endTime.slice(0, 5)) {
      this.error.set('Change the date or time to reschedule. Use Edit for other changes.'); return;
    }
    this.error.set(null); this.saving.set(true); this.dialog.disableClose = true; this.form.disable({ emitEvent: false });
    const session = this.data.session;
    const request: Observable<SessionEditorResult> = this.data.mode === 'reschedule' && session
      ? this.api.reschedule(session.id, { newDate: date, newStartTime: value.startTime, newEndTime: value.endTime,
        lecturerUserId: value.lecturerUserId, reason: value.reason.trim(), version: session.version })
      : this.data.mode === 'edit' && session
        ? this.api.update(session.id, { batchId: this.data.batch.id, sessionDate: date, startTime: value.startTime,
          endTime: value.endTime, lecturerUserId: value.lecturerUserId, topic: value.topic.trim() || null,
          classroom: value.classroom.trim() || null, remarks: value.remarks.trim() || null, version: session.version })
        : this.api.create({ batchId: this.data.batch.id, sessionDate: date, startTime: value.startTime,
          endTime: value.endTime, lecturerUserId: value.lecturerUserId, topic: value.topic.trim() || null,
          classroom: value.classroom.trim() || null, remarks: value.remarks.trim() || null });
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      this.saving.set(false); this.dialog.disableClose = false; this.form.enable({ emitEvent: false });
    })).subscribe({
      next: result => this.dialog.close(result),
      error: error => {
        if (error.status === 403) this.dialog.close({ accessDenied: true });
        else if (error.status === 409 && session) this.dialog.close({ conflict: true });
        else this.error.set(error);
      },
    });
  }
}
