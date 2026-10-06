import { TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Batch } from '../batches/batch.models';
import { SessionEditorDialogComponent } from './session-editor-dialog.component';
import { ClassSession } from './session.models';
import { SessionService } from './session.service';

const batch: Batch = { id: 7, course: { id: 1, name: 'Cookery', shortCode: 'CK' }, branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
  batchNumber: '2026/CK01', startDate: '2026-07-01', endDate: '2026-12-31', durationMonths: 6,
  scheduleMode: 'MANUAL', status: 'ACTIVE', registrationSequence: 0, lecturerCount: 1, studentCount: 0,
  feePlanConfigured: true, createdAt: '', updatedAt: '', version: 0 };
const session: ClassSession = { id: 12, batchId: 7, batchNumber: batch.batchNumber, courseName: 'Cookery', branchId: 1, branchName: 'IHM Hotel School',
  sessionDate: '2026-10-05', startTime: '09:00:00', endTime: '13:00:00', lecturerUserId: 20, lecturerName: 'Lecturer',
  topic: 'Safety', classroom: null, remarks: null, status: 'SCHEDULED', cancellationReason: null,
  originalSessionId: null, attendanceSubmittedAt: null, createdAt: '', updatedAt: '', version: 2,
  sourceScheduleId: null, generationDate: null, reschedulingReason: null };
const assignment = { id: 1, batchId: 7, lecturerUserId: 20, lecturerFullName: 'Lecturer', lecturerUsername: 'lecturer',
  assignmentStartDate: '2026-10-01', assignmentEndDate: '2026-10-31', status: 'ACTIVE' as const, createdAt: '', updatedAt: '' };

describe('SessionEditorDialogComponent', () => {
  function setup(mode: 'create' | 'edit' | 'reschedule', current?: ClassSession) {
    const api = { create: vi.fn(() => of(session)), update: vi.fn(() => of(session)),
      reschedule: vi.fn(() => of({ original: session, replacement: { ...session, id: 13 } })) };
    const dialog = { close: vi.fn(), disableClose: false };
    TestBed.configureTestingModule({ imports: [SessionEditorDialogComponent], providers: [provideNativeDateAdapter(),
      { provide: MAT_DIALOG_DATA, useValue: { mode, batch, session: current, lecturers: [assignment] } },
      { provide: MatDialogRef, useValue: dialog }, { provide: SessionService, useValue: api },
    ] });
    const fixture = TestBed.createComponent(SessionEditorDialogComponent);
    fixture.detectChanges(); return { fixture, component: fixture.componentInstance, api, dialog };
  }

  it('blocks invalid dates and time order before creating', () => {
    const { component, api } = setup('create');
    component.save(); expect(api.create).not.toHaveBeenCalled();
    component.form.controls.sessionDate.setValue(new Date(2026, 9, 5));
    component.form.controls.endTime.setValue('08:00'); component.save();
    expect(api.create).not.toHaveBeenCalled();
  });

  it('sends local date, chosen lecturer and trimmed optional fields', () => {
    const { component, api, dialog } = setup('create');
    component.form.patchValue({ sessionDate: new Date(2026, 9, 5), lecturerUserId: 20, topic: '  Safety  ' });
    component.save();
    expect(api.create).toHaveBeenCalledWith(expect.objectContaining({ batchId: 7, sessionDate: '2026-10-05',
      lecturerUserId: 20, topic: 'Safety' }));
    expect(dialog.close).toHaveBeenCalled();
  });

  it('limits lecturer options to assignments covering the selected day', () => {
    const { component } = setup('edit', session);
    expect(component.eligibleLecturers()).toHaveLength(1);
    component.form.controls.sessionDate.setValue(new Date(2026, 10, 5));
    expect(component.eligibleLecturers()).toHaveLength(0);
    expect(component.form.controls.lecturerUserId.value).toBeNull();
  });

  it('requires a reason and changed date or time for rescheduling', () => {
    const { component, api } = setup('reschedule', session);
    component.save(); expect(api.reschedule).not.toHaveBeenCalled();
    component.form.controls.reason.setValue('   '); component.form.controls.sessionDate.setValue(new Date(2026, 9, 6));
    component.save(); expect(api.reschedule).not.toHaveBeenCalled();
    component.form.controls.reason.setValue('  Room unavailable  '); component.save();
    expect(api.reschedule).toHaveBeenCalledWith(12, expect.objectContaining({ newDate: '2026-10-06',
      reason: 'Room unavailable', version: 2 }));
  });

  it('closes with a conflict result when an edited session is stale', () => {
    const { component, api, dialog } = setup('edit', session);
    api.update.mockReturnValue(throwError(() => ({ status: 409 })));
    component.save(); expect(dialog.close).toHaveBeenCalledWith({ conflict: true });
  });

  it('closes the form when session access is revoked', () => {
    const { component, api, dialog } = setup('edit', session);
    api.update.mockReturnValue(throwError(() => ({ status: 403 })));
    component.save(); expect(dialog.close).toHaveBeenCalledWith({ accessDenied: true });
  });
});
