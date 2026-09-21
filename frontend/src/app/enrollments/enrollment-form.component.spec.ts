import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { Batch } from '../batches/batch.models';
import { BatchService } from '../batches/batch.service';
import { Student } from '../students/student.models';
import { StudentService } from '../students/student.service';
import { NotificationService } from '../shared/notification.service';
import { EnrollmentPreview } from './enrollment.models';
import { EnrollmentService } from './enrollment.service';
import { EnrollmentFormComponent } from './enrollment-form.component';

@Component({ template: '' })
class BlankComponent {}

describe('EnrollmentFormComponent', () => {
  const student: Student = {
    id: 5, fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678',
    alternativeContactNumber: null, email: null, address: 'Colombo', dateOfBirth: null,
    gender: null, remarks: null, status: 'ACTIVE', photoAvailable: false, photoUrl: null,
    photoThumbnailUrl: null, createdAt: '', updatedAt: '', version: 0,
  };
  const batch: Batch = {
    id: 8, batchNumber: '2026/PB01', course: { id: 2, name: 'Pastry', shortCode: 'PB' },
    branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
    startDate: '2026-01-20', endDate: '2026-03-31', durationMonths: 3, scheduleMode: 'MANUAL',
    status: 'UPCOMING', remarks: null, registrationSequence: 0, lecturerCount: 0,
    studentCount: 0, feePlanConfigured: true, createdAt: '', updatedAt: '', version: 1,
  };
  const preview: EnrollmentPreview = {
    currencyCode: 'LKR', totalAmount: 600, batchVersion: 1, feePlanVersion: 2,
    charges: [{ type: 'REGISTRATION_FEE', installmentNumber: null, description: 'Registration fee', dueDate: '2026-01-15', amount: 100 }],
  };
  let api: { preview: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<EnrollmentFormComponent>;

  beforeEach(async () => {
    api = { preview: vi.fn(() => of(preview)), create: vi.fn(() => of({ id: 10, registrationNumber: '2026/PB01/0001' })) };
    await TestBed.configureTestingModule({
      imports: [EnrollmentFormComponent],
      providers: [
        provideRouter([{ path: 'enrollments/:id', component: BlankComponent }]),
        provideNativeDateAdapter(),
        { provide: EnrollmentService, useValue: api },
        { provide: StudentService, useValue: { list: vi.fn(() => of({ content: [student], totalElements: 1, page: 0, size: 5, totalPages: 1 })) } },
        { provide: BatchService, useValue: { list: vi.fn(() => of({ content: [batch], totalElements: 1, page: 0, size: 5, totalPages: 1 })) } },
        { provide: NotificationService, useValue: { success: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(EnrollmentFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  function component() {
    return fixture.componentInstance as unknown as {
      form: { controls: { enrollmentDate: { setValue(v: Date): void }; remarks: { setValue(v: string): void } } };
      selectStudent(student: Student): void; selectBatch(batch: Batch): void;
      previewCharges(): void; enroll(): void; preview(): EnrollmentPreview | null;
    };
  }

  it('previews before saving and sends the reviewed fee versions', () => {
    const c = component();
    c.selectStudent(student); c.selectBatch(batch);
    c.form.controls.enrollmentDate.setValue(new Date(2026, 0, 15));
    c.previewCharges();
    expect(api.preview).toHaveBeenCalledWith(expect.objectContaining({ studentId: 5, batchId: 8, enrollmentDate: '2026-01-15' }));
    expect(c.preview()?.totalAmount).toBe(600);
    expect(api.create).not.toHaveBeenCalled();
    c.enroll();
    expect(api.create).toHaveBeenCalledWith(expect.objectContaining({ expectedBatchVersion: 1, expectedFeePlanVersion: 2 }));
  });

  it('invalidates a preview when enrollment details change', () => {
    const c = component(); c.selectStudent(student); c.selectBatch(batch);
    c.previewCharges();
    expect(c.preview()).not.toBeNull();
    c.form.controls.remarks.setValue('Changed');
    expect(c.preview()).toBeNull();
    c.enroll();
    expect(api.create).not.toHaveBeenCalled();
  });

  it('cancels an in-flight preview when the student changes', () => {
    const pending = new Subject<EnrollmentPreview>(); api.preview.mockReturnValue(pending.asObservable());
    const c = component(); c.selectStudent(student); c.selectBatch(batch); c.previewCharges();
    c.selectStudent({ ...student, id: 6 });
    pending.next(preview);
    expect(c.preview()).toBeNull();
    expect(api.create).not.toHaveBeenCalled();
  });
});
