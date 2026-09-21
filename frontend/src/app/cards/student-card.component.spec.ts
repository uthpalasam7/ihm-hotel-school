import { TestBed } from '@angular/core/testing';
import { convertToParamMap, provideRouter, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { MatDialog } from '@angular/material/dialog';
import { EnrollmentService } from '../enrollments/enrollment.service';
import { StudentService } from '../students/student.service';
import { NotificationService } from '../shared/notification.service';
import { StudentCardComponent } from './student-card.component';
import { StudentCardService } from './student-card.service';

describe('StudentCardComponent', () => {
  const student = { id: 5, fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678',
    alternativeContactNumber: null, email: 'nimal@example.invalid', address: 'Colombo', dateOfBirth: null,
    gender: null, remarks: null, status: 'ACTIVE', photoAvailable: false, photoUrl: null,
    photoThumbnailUrl: null, createdAt: '', updatedAt: '', version: 0 };
  const card = { id: 2, studentId: 5, identifier: 'IHM-ST-000005', status: 'ACTIVE',
    issuedAt: '2026-09-21T00:00:00Z', revokedAt: null, version: 1, contactLine: 'Return lost cards to IHM Hotel School' };
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let studentApi: { get: ReturnType<typeof vi.fn>; loadPhoto: ReturnType<typeof vi.fn> };
  let enrollmentCount: number;
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    enrollmentCount=0;
    api={
      get: vi.fn(() => of(null)), issue: vi.fn(() => of(card)),
      replace: vi.fn(() => of({ ...card, version: 2 })), revoke: vi.fn(() => of({ ...card, status: 'REVOKED' })),
      qr: vi.fn(() => of(new Blob(['qr'], { type: 'image/png' }))),
      pdf: vi.fn(() => of(new Blob(['pdf'], { type: 'application/pdf' }))),
      history: vi.fn(() => of({ content: [], totalElements: 0, page: 0, size: 20, totalPages: 0 })),
      deliveries: vi.fn(() => of({ content: [], totalElements: 0, page: 0, size: 20, totalPages: 0 })),
      emailAvailability: vi.fn(() => of({ available: true })),
      email: vi.fn(() => of({ id: 9, status: 'QUEUED' })),
    };
    dialog={ open: vi.fn(() => ({ afterClosed: () => of({ confirmed: true, reason: 'Lost card' }) })) };
    studentApi={ get: vi.fn(() => of(student)), loadPhoto: vi.fn(() => of(new Blob(['photo'], { type: 'image/png' }))) };
    Object.defineProperty(URL,'createObjectURL',{ configurable:true, value:vi.fn(() => 'blob:qr') });
    Object.defineProperty(URL,'revokeObjectURL',{ configurable:true, value:vi.fn() });
    await TestBed.configureTestingModule({
      imports: [StudentCardComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '5' }) } } },
        { provide: StudentService, useValue: studentApi },
        { provide: EnrollmentService, useValue: { list: vi.fn(() => of({ content: [], totalElements: enrollmentCount, page: 0, size: 1, totalPages: 0 })) } },
        { provide: StudentCardService, useValue: api },
        { provide: MatDialog, useValue: dialog },
        { provide: NotificationService, useValue: { success: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('requires an enrollment before issuing a card', async () => {
    const fixture=TestBed.createComponent(StudentCardComponent); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Enroll the student first');
    expect(api['issue']).not.toHaveBeenCalled();
  });

  it('loads the full-size student photo for the card preview', async () => {
    enrollmentCount=1;
    studentApi.get.mockReturnValue(of({ ...student, photoAvailable: true }));
    api['get'].mockReturnValue(of(card));
    const fixture=TestBed.createComponent(StudentCardComponent); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    expect(studentApi.loadPhoto).toHaveBeenCalledWith(5, 'full');
  });

  it('shows the saved email and queues delivery only after staff confirmation', async () => {
    enrollmentCount=1;
    api['get'].mockReturnValue(of(card));
    const fixture=TestBed.createComponent(StudentCardComponent); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('nimal@example.invalid');
    (fixture.componentInstance as unknown as { email(): void }).email();
    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ data: expect.objectContaining({ message: expect.stringContaining('nimal@example.invalid') }) }));
    expect(api['email']).toHaveBeenCalledWith(5,expect.any(String));
  });

  it('passes a mandatory reason when replacing the card', async () => {
    enrollmentCount=1;
    api['get'].mockReturnValue(of(card));
    const fixture=TestBed.createComponent(StudentCardComponent); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    (fixture.componentInstance as unknown as { change(action: 'replace'): void }).change('replace');
    expect(api['replace']).toHaveBeenCalledWith(5,'Lost card');
  });

  it('keeps delivery history visible after a card is cancelled', async () => {
    enrollmentCount=1;
    api['get'].mockReturnValue(of({ ...card, status: 'REVOKED' }));
    api['deliveries'].mockReturnValue(of({ content: [{ id: 9, status: 'ACCEPTED',
      recipientEmail: 'nimal@example.invalid', createdAt: '2026-09-21T00:00:00Z', lastError: null }],
      totalElements: 1, page: 0, size: 20, totalPages: 1 }));
    const fixture=TestBed.createComponent(StudentCardComponent); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    const text=fixture.nativeElement.textContent as string;
    expect(text).toContain('Delivery attempts');
    expect(text).toContain('ACCEPTED');
    expect(text).not.toContain('Email card PDF');
  });
});
