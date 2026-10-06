import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { SchedulePatternDialogComponent } from './schedule-pattern-dialog.component';
import { ScheduleService } from './schedule.service';

describe('SchedulePatternDialogComponent', () => {
  let fixture: ComponentFixture<SchedulePatternDialogComponent>; let c: SchedulePatternDialogComponent;
  let save: ReturnType<typeof vi.fn>; let close: ReturnType<typeof vi.fn>;
  beforeEach(async () => {
    save = vi.fn(() => of({})); close = vi.fn();
    await TestBed.configureTestingModule({ imports: [SchedulePatternDialogComponent], providers: [
      { provide: MAT_DIALOG_DATA, useValue: { batchId: 7, batchNumber: '2026/CK01', lecturers: [], schedule: { id: 2, version: 4, dayOfWeek: 3, startTime: '09:00:00', endTime: '13:00:00', defaultLecturerUserId: null, classroom: 'Kitchen', status: 'ACTIVE' } } },
      { provide: MatDialogRef, useValue: { close } }, { provide: ScheduleService, useValue: { save } },
    ] }).compileComponents();
    fixture = TestBed.createComponent(SchedulePatternDialogComponent); c = fixture.componentInstance; fixture.detectChanges();
  });
  afterEach(() => fixture.destroy());
  it('rejects equal/reversed times and long classrooms', () => {
    for (const end of ['09:00', '08:00']) { c.form.controls.endTime.setValue(end); c.save(); }
    c.form.controls.endTime.setValue('13:00'); c.form.controls.classroom.setValue('x'.repeat(151)); c.save(); expect(save).not.toHaveBeenCalled();
  });
  it('retains the edit version and normalizes optional classroom text', () => {
    c.form.controls.classroom.setValue('  Kitchen 2  '); c.save();
    expect(save).toHaveBeenCalledWith(7, expect.objectContaining({ version: 4, classroom: 'Kitchen 2', defaultLecturerUserId: null }), 2); expect(close).toHaveBeenCalledWith(true);
  });
  it('keeps the form open with server validation errors', () => {
    save.mockReturnValue(throwError(() => ({ error: { message: 'Weekly pattern overlaps an active pattern', fieldErrors: [{ field: 'classroom', message: 'Room invalid' }] } })));
    c.save(); fixture.detectChanges(); expect(close).not.toHaveBeenCalled(); expect(c.form.enabled).toBe(true); expect(fixture.nativeElement.textContent).toContain('overlaps'); expect(fixture.nativeElement.textContent).toContain('Room invalid');
  });
  it('prevents duplicate saves while a mutation is running', () => {
    const pending = new Subject(); save.mockReturnValue(pending); c.save(); c.save(); expect(save).toHaveBeenCalledTimes(1); expect(c.form.disabled).toBe(true); pending.complete();
  });
});
