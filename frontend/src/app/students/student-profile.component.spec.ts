import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { NotificationService } from '../shared/notification.service';
import { Student } from './student.models';
import { StudentProfileComponent } from './student-profile.component';
import { StudentService } from './student.service';

describe('StudentProfileComponent', () => {
  const student: Student = {
    id: 5, fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678',
    alternativeContactNumber: null, email: 'nimal@example.invalid', address: 'Kurunegala',
    dateOfBirth: null, gender: null, remarks: null, status: 'ACTIVE', photoAvailable: false,
    photoUrl: null, photoThumbnailUrl: null, createdAt: '', updatedAt: '2026-07-20T00:00:00Z', version: 0,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StudentProfileComponent],
      providers: [
        provideRouter([]),
        { provide: StudentService, useValue: { get: vi.fn(() => of(student)), loadPhoto: vi.fn(), changeStatus: vi.fn(), uploadPhoto: vi.fn(), deletePhoto: vi.fn() } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(undefined) }) } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('renders the student profile without future enrollment or finance tabs', async () => {
    const fixture = TestBed.createComponent(StudentProfileComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Nimal Perera');
    expect(text).toContain('Contact information');
    expect(text).not.toContain('Fees and payments');
    expect(text).toContain('View enrollments');
    expect(text).toContain('Enroll in a batch');
  });
});
